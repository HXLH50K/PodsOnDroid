package moe.chenxy.hyperpods.utils

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Application-owned, bounded work: leaving the page does not cancel verification. */
object InterfaceScopeRestart {
    enum class Mode { INTERFACE_ONLY, ALL, SELECTED }
    enum class Failure {
        ALREADY_RUNNING, NO_VERIFIED_HOSTS, HOST_UNAVAILABLE, ACTIVE_CALL,
        CALL_STATE_UNKNOWN, AUDIO_STATE_UNAVAILABLE, IDENTITY_CHANGED, ROOT_COMMAND_FAILED,
        MODULE_OUTDATED, RELOAD_TIMEOUT, PROCESS_DID_NOT_EXIT, PERMISSION_DENIED,
        OPERATION_CANCELLED, VERIFICATION_INTERRUPTED, OPERATION_TIMED_OUT, UNEXPECTED_ERROR
    }

    enum class HostState {
        PENDING, PREFLIGHTED, SIGNAL_PENDING, SIGNAL_ACKNOWLEDGED, VERIFIED, INDIRECTLY_VERIFIED,
        AWAITING_NORMAL_RELAUNCH, OLD_PROCESS_PRESENT, OLD_MODULE, MISSING_MODULE_REPORT,
        ORIGINAL_PROCESS_ABSENT, FAILED, OUTCOME_UNKNOWN
    }

    data class HostResult(
        val processName: String,
        val optional: Boolean,
        val state: HostState = HostState.PENDING,
        val originalPid: Int? = null,
        val replacementPid: Int? = null,
    )

    data class Result(
        val completed: List<String>,
        val failure: Failure? = null,
        val rootFailure: ScopeRestart.FailureCode? = null,
        val pending: List<String> = emptyList(),
        val hosts: List<HostResult> = emptyList(),
        val signalOutcomeUnknown: Boolean = false,
    )

    data class State(
        val operationId: Long = 0,
        val running: Boolean = false,
        val cancelling: Boolean = false,
        val processName: String? = null,
        val hosts: List<HostResult> = emptyList(),
        val result: Result? = null,
    )

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    val lastResult: Result? get() = state.value.result
    private var job: Job? = null

    /** Old module versions are deliberately eligible; only replacement reports must be current. */
    fun eligible(
        reports: Collection<ModuleStatus.Report>,
        mode: Mode = Mode.INTERFACE_ONLY,
    ): List<ModuleStatus.Report> = reports.filter {
        val host = ScopeRestart.Host.exact(it.packageName, it.processName)
        host != null && (mode != Mode.INTERFACE_ONLY || !host.affectsControlIpc) &&
            it.frameworkApi in 102..1000 && it.pid > 1 && it.uid > 0
    }.sortedBy { ScopeRestart.reloadOrder.indexOf(ScopeRestart.Host.exact(it.packageName, it.processName)) }

    @Synchronized
    fun start(context: Context, mode: Mode, confirmedProcesses: Set<String>): Boolean {
        if (state.value.running) return false
        val available = ScopeRestartSelection.available().map { it.processName }.toSet()
        if (confirmedProcesses.isEmpty() || !available.containsAll(confirmedProcesses)) {
            Log.w("PODS-ScopeRestart", "Restart rejected: target is outside the active system profile")
            return false
        }
        val application = context.applicationContext
        val confirmed = confirmedProcesses.toSet()
        mutableState.value = State(operationId = state.value.operationId + 1, running = true)
        job = scope.launch(start = CoroutineStart.LAZY) {
            val operation = ScopeReloadOperation(ScopeRestart(), object : ScopeReloadEnvironment {
                override suspend fun reports(timeoutMs: Long) = ModuleStatusQuery.collect(application, timeoutMs)
                override fun packageUid(packageName: String) = application.packageManager.getPackageUid(packageName, 0)
                override fun audioIsNormal(): Boolean? =
                    application.getSystemService(AudioManager::class.java)?.let { it.mode == AudioManager.MODE_NORMAL }
            }) { process, hosts ->
                mutableState.value = state.value.copy(processName = process, hosts = hosts)
            }
            val result = try {
                withTimeout(180_000) { operation.run(mode, confirmed) }
            } catch (_: CancellationException) {
                operation.result ?: Result(emptyList(), Failure.OPERATION_CANCELLED, pending = confirmed.toList())
            }
            mutableState.value = state.value.copy(
                running = false, cancelling = false, processName = null, hosts = result.hosts, result = result
            )
        }
        job?.start()
        return true
    }

    @Synchronized
    fun cancel() {
        if (!state.value.running) return
        mutableState.value = state.value.copy(cancelling = true)
        job?.cancel()
    }
}

/** Report collection must authenticate a new request, never return a persisted status cache. */
internal interface ScopeReloadEnvironment {
    suspend fun reports(timeoutMs: Long): Map<String, ModuleStatus.Report>
    fun packageUid(packageName: String): Int
    fun audioIsNormal(): Boolean?
}

/** A single sequential attempt; no retries of signals, force-stop, component launches or reboot. */
internal class ScopeReloadOperation(
    private val executor: ScopeRestart,
    private val environment: ScopeReloadEnvironment,
    private val progress: (String?, List<InterfaceScopeRestart.HostResult>) -> Unit = { _, _ -> },
) {
    private val hosts = linkedMapOf<String, InterfaceScopeRestart.HostResult>()
    private var active: ScopeRestart.Target? = null
    private var mayHaveSignalled = false
    private var mode = InterfaceScopeRestart.Mode.INTERFACE_ONLY
    var result: InterfaceScopeRestart.Result? = null
        private set

    suspend fun run(
        mode: InterfaceScopeRestart.Mode,
        confirmedProcesses: Set<String>,
    ): InterfaceScopeRestart.Result {
        this.mode = mode
        val confirmed = confirmedProcesses.toSet()
        val full = mode != InterfaceScopeRestart.Mode.INTERFACE_ONLY
        val required = ScopeRestart.requiredReloadHosts.map { it.processName }.toSet()
        val allowed = ScopeRestart.Host.entries.filter { full || !it.affectsControlIpc }
            .map { it.processName }.toSet()
        confirmed.sortedBy { name -> ScopeRestart.reloadOrder.indexOfFirst { it.processName == name } }
            .forEach { hosts[it] = InterfaceScopeRestart.HostResult(it, full && it == ScopeRestart.Host.XIAOMI_UI.processName) }
        try {
            if (confirmed.isEmpty() || !allowed.containsAll(confirmed) ||
                mode == InterfaceScopeRestart.Mode.ALL && !confirmed.containsAll(required)) {
                return finish(InterfaceScopeRestart.Failure.NO_VERIFIED_HOSTS)
            }
            currentCoroutineContext().ensureActive()
            val initial = environment.reports(2500)
            checkIdle(initial)?.let { return finish(it) }
            val evidence = mutableListOf<ScopeRestart.VerifiedHost>()
            // Validate every required report and real package UID before the first root command.
            for ((name, status) in hosts.toMap()) {
                val report = initial[name]
                if (report == null) {
                    if (status.optional) {
                        update(name, InterfaceScopeRestart.HostState.AWAITING_NORMAL_RELAUNCH)
                        continue
                    }
                    update(name, InterfaceScopeRestart.HostState.MISSING_MODULE_REPORT)
                    return finish(InterfaceScopeRestart.Failure.HOST_UNAVAILABLE)
                }
                if (!valid(report, name)) {
                    update(name, InterfaceScopeRestart.HostState.FAILED)
                    return finish(InterfaceScopeRestart.Failure.IDENTITY_CHANGED)
                }
                evidence.add(report.evidence())
            }
            var exitPlan: ScopeRestart.ProcessExitPlan? = null
            val targets = if (full) {
                val selectedHosts = if (mode == InterfaceScopeRestart.Mode.SELECTED)
                    ScopeRestart.Host.entries.filter { it.processName in confirmed }.toSet() else null
                when (val discovered = executor.discoverProcessExit(evidence, explicitUserApproval = true,
                    confirmedHosts = selectedHosts)) {
                    is ScopeRestart.ProcessExitDiscoveryResult.Rejected -> return rootFailed(discovered.failure)
                    is ScopeRestart.ProcessExitDiscoveryResult.Ready -> {
                        exitPlan = discovered.plan
                        discovered.plan.targets
                    }
                }
            } else {
                when (val discovered = executor.discover(ScopeRestart.Preset.UI_HOSTS, evidence)) {
                    is ScopeRestart.DiscoveryResult.Rejected -> return rootFailed(discovered.failure)
                    is ScopeRestart.DiscoveryResult.Ready -> discovered.plan.targets
                }
            }
            targets.forEach {
                hosts[it.host.processName] = hosts.getValue(it.host.processName).copy(
                    state = InterfaceScopeRestart.HostState.PREFLIGHTED, originalPid = it.pid
                )
            }
            hosts.values.filter { it.optional && targets.none { target -> target.host.processName == it.processName } }
                .toList().forEach { update(it.processName, InterfaceScopeRestart.HostState.AWAITING_NORMAL_RELAUNCH) }
            publish()
            for (target in targets) {
                currentCoroutineContext().ensureActive()
                active = target
                mayHaveSignalled = false
                publish()
                val fresh = environment.reports(2500)
                checkIdle(fresh)?.let { return finish(it) }
                if (full) {
                    // A collection also reveals failures in hosts whose turn has not arrived.
                    for (requiredTarget in targets.filter { it.host in ScopeRestart.requiredReloadHosts }) {
                        val name = requiredTarget.host.processName
                        val report = fresh[name]
                        if (report == null) {
                            update(name, InterfaceScopeRestart.HostState.MISSING_MODULE_REPORT)
                            return finish(InterfaceScopeRestart.Failure.HOST_UNAVAILABLE)
                        }
                        if (!valid(report, name, requiredTarget.uid)) {
                            update(name, InterfaceScopeRestart.HostState.FAILED)
                            return finish(InterfaceScopeRestart.Failure.IDENTITY_CHANGED)
                        }
                        if (hosts.getValue(name).state in verifiedStates && !report.matchesInstalled) {
                            update(name, InterfaceScopeRestart.HostState.OLD_MODULE, report.pid)
                            return finish(InterfaceScopeRestart.Failure.MODULE_OUTDATED)
                        }
                    }
                }
                val current = fresh[target.host.processName]
                if (current == null) {
                    if (hosts.getValue(target.host.processName).optional && originalIsAbsent(target)) {
                        update(target.host.processName, InterfaceScopeRestart.HostState.AWAITING_NORMAL_RELAUNCH)
                        continue
                    }
                    if (result != null) return requireNotNull(result)
                    update(target.host.processName, InterfaceScopeRestart.HostState.MISSING_MODULE_REPORT)
                    return finish(InterfaceScopeRestart.Failure.HOST_UNAVAILABLE)
                }
                if (!valid(current, target.host.processName, target.uid)) {
                    update(target.host.processName, InterfaceScopeRestart.HostState.FAILED)
                    return finish(InterfaceScopeRestart.Failure.IDENTITY_CHANGED)
                }
                if (current.pid != target.pid) {
                    // An earlier provider exit may already have replaced this consumer.
                    if (!acceptReplacement(target, current, indirect = true)) return requireNotNull(result)
                    continue
                }
                val uiPlan = if (!full) {
                    when (val checked = executor.discover(ScopeRestart.Preset.UI_HOSTS, listOf(target.evidence))) {
                        is ScopeRestart.DiscoveryResult.Rejected -> return rootFailed(checked.failure)
                        is ScopeRestart.DiscoveryResult.Ready -> {
                            if (checked.plan.targets.single().startTimeTicks != target.startTimeTicks) {
                                return rootFailed(ScopeRestart.Failure(ScopeRestart.FailureCode.PID_REUSED, target.host))
                            }
                            checked.plan
                        }
                    }
                } else null
                currentCoroutineContext().ensureActive()
                update(target.host.processName, InterfaceScopeRestart.HostState.SIGNAL_PENDING)
                mayHaveSignalled = true
                val signal = if (full) executor.signalProcessExit(requireNotNull(exitPlan), target)
                    else executor.signal(requireNotNull(uiPlan))
                when (signal) {
                    is ScopeRestart.SignalResult.Stopped -> return rootFailed(signal.failure)
                    is ScopeRestart.SignalResult.Completed -> {
                        if (signal.signalled != listOf(target)) {
                            return finish(InterfaceScopeRestart.Failure.IDENTITY_CHANGED)
                        }
                    }
                }
                update(target.host.processName, InterfaceScopeRestart.HostState.SIGNAL_ACKNOWLEDGED)
                if (!verifyReplacement(target)) return requireNotNull(result)
                mayHaveSignalled = false
            }
            active = null
            // A later dependent restart must not turn an earlier success into a stale success.
            val finalReports = environment.reports(2500)
            for (target in targets.filter { hosts.getValue(it.host.processName).state in verifiedStates }) {
                active = target
                val final = finalReports[target.host.processName]
                if (final == null) {
                    if (hosts.getValue(target.host.processName).optional && originalIsAbsent(target)) {
                        update(target.host.processName, InterfaceScopeRestart.HostState.AWAITING_NORMAL_RELAUNCH)
                        continue
                    }
                    if (result != null) return requireNotNull(result)
                    update(target.host.processName, InterfaceScopeRestart.HostState.MISSING_MODULE_REPORT)
                    return finish(InterfaceScopeRestart.Failure.HOST_UNAVAILABLE)
                }
                if (!acceptReplacement(target, final,
                        hosts.getValue(target.host.processName).state == InterfaceScopeRestart.HostState.INDIRECTLY_VERIFIED)) {
                    return requireNotNull(result)
                }
            }
            active = null
            return finish()
        } catch (cancelled: CancellationException) {
            active?.let {
                if (mayHaveSignalled) update(it.host.processName, InterfaceScopeRestart.HostState.OUTCOME_UNKNOWN)
            }
            finish(when {
                cancelled is TimeoutCancellationException -> InterfaceScopeRestart.Failure.OPERATION_TIMED_OUT
                mayHaveSignalled -> InterfaceScopeRestart.Failure.VERIFICATION_INTERRUPTED
                else -> InterfaceScopeRestart.Failure.OPERATION_CANCELLED
            }, unknown = mayHaveSignalled)
            throw cancelled
        } catch (_: PackageManager.NameNotFoundException) {
            return finish(InterfaceScopeRestart.Failure.HOST_UNAVAILABLE)
        } catch (_: SecurityException) {
            return finish(InterfaceScopeRestart.Failure.PERMISSION_DENIED, unknown = mayHaveSignalled)
        } catch (_: Exception) {
            return finish(InterfaceScopeRestart.Failure.UNEXPECTED_ERROR, unknown = mayHaveSignalled)
        }
    }

    private fun checkIdle(reports: Map<String, ModuleStatus.Report>): InterfaceScopeRestart.Failure? {
        when (environment.audioIsNormal()) {
            null -> return InterfaceScopeRestart.Failure.AUDIO_STATE_UNAVAILABLE
            false -> return InterfaceScopeRestart.Failure.ACTIVE_CALL
            true -> Unit
        }
        if (mode != InterfaceScopeRestart.Mode.INTERFACE_ONLY) {
            val bluetooth = reports[ScopeRestart.Host.BLUETOOTH.processName]
                ?: return InterfaceScopeRestart.Failure.HOST_UNAVAILABLE
            if (!valid(bluetooth, ScopeRestart.Host.BLUETOOTH.processName)) {
                return InterfaceScopeRestart.Failure.IDENTITY_CHANGED
            }
            return when (bluetooth.callState) {
                "idle" -> null
                "busy" -> InterfaceScopeRestart.Failure.ACTIVE_CALL
                else -> InterfaceScopeRestart.Failure.CALL_STATE_UNKNOWN
            }
        }
        return null
    }

    private fun valid(report: ModuleStatus.Report, name: String, uid: Int? = null): Boolean =
        report.processName == name && InterfaceScopeRestart.eligible(listOf(report), mode).size == 1 &&
            report.uid == environment.packageUid(report.packageName) && (uid == null || report.uid == uid)

    private suspend fun acceptReplacement(
        target: ScopeRestart.Target,
        report: ModuleStatus.Report,
        indirect: Boolean,
    ): Boolean {
        val name = target.host.processName
        if (!valid(report, name, target.uid) || report.pid == target.pid) {
            update(name, InterfaceScopeRestart.HostState.FAILED)
            finish(InterfaceScopeRestart.Failure.IDENTITY_CHANGED)
            return false
        }
        if (!report.matchesInstalled) {
            update(name, InterfaceScopeRestart.HostState.OLD_MODULE, report.pid)
            finish(InterfaceScopeRestart.Failure.MODULE_OUTDATED)
            return false
        }
        when (val checked = executor.discover(ScopeRestart.Preset.ALL_VERIFIED, listOf(report.evidence()))) {
            is ScopeRestart.DiscoveryResult.Rejected -> {
                rootFailed(checked.failure)
                return false
            }
            is ScopeRestart.DiscoveryResult.Ready -> Unit
        }
        update(name, if (indirect) InterfaceScopeRestart.HostState.INDIRECTLY_VERIFIED
            else InterfaceScopeRestart.HostState.VERIFIED, report.pid)
        return true
    }

    private suspend fun verifyReplacement(target: ScopeRestart.Target): Boolean {
        var last: ModuleStatus.Report? = null
        repeat(if (hosts.getValue(target.host.processName).optional) 2 else 8) {
            currentCoroutineContext().ensureActive()
            last = environment.reports(2000)[target.host.processName]
            last?.let { report ->
                if (!valid(report, target.host.processName, target.uid)) {
                    update(target.host.processName, InterfaceScopeRestart.HostState.FAILED)
                    finish(InterfaceScopeRestart.Failure.IDENTITY_CHANGED)
                    return false
                }
                if (report.pid != target.pid && report.matchesInstalled) {
                    return acceptReplacement(target, report, indirect = false)
                }
            }
        }
        val name = target.host.processName
        if (last != null) {
            update(name, if (last!!.pid == target.pid) InterfaceScopeRestart.HostState.OLD_PROCESS_PRESENT
                else InterfaceScopeRestart.HostState.OLD_MODULE, last!!.pid.takeIf { it != target.pid })
            finish(if (last!!.pid == target.pid) InterfaceScopeRestart.Failure.PROCESS_DID_NOT_EXIT
                else InterfaceScopeRestart.Failure.MODULE_OUTDATED)
            return false
        }
        val absent = originalIsAbsent(target)
        if (result != null) return false
        if (absent && hosts.getValue(name).optional) {
            update(name, InterfaceScopeRestart.HostState.AWAITING_NORMAL_RELAUNCH)
            return true
        }
        update(name, if (absent) InterfaceScopeRestart.HostState.ORIGINAL_PROCESS_ABSENT
            else InterfaceScopeRestart.HostState.MISSING_MODULE_REPORT)
        finish(InterfaceScopeRestart.Failure.RELOAD_TIMEOUT)
        return false
    }

    private suspend fun originalIsAbsent(target: ScopeRestart.Target): Boolean =
        when (val checked = executor.discover(ScopeRestart.Preset.ALL_VERIFIED, listOf(target.evidence))) {
            is ScopeRestart.DiscoveryResult.Rejected -> {
                if (checked.failure.code == ScopeRestart.FailureCode.PROCESS_VANISHED) true
                else { rootFailed(checked.failure); false }
            }
            is ScopeRestart.DiscoveryResult.Ready -> {
                if (checked.plan.targets.single().startTimeTicks != target.startTimeTicks) {
                    rootFailed(ScopeRestart.Failure(ScopeRestart.FailureCode.PID_REUSED, target.host))
                }
                false
            }
        }

    private fun rootFailed(failure: ScopeRestart.Failure): InterfaceScopeRestart.Result {
        val unknown = mayHaveSignalled && failure.code in setOf(
            ScopeRestart.FailureCode.TIMEOUT, ScopeRestart.FailureCode.IO_ERROR,
            ScopeRestart.FailureCode.INTERRUPTED, ScopeRestart.FailureCode.OUTPUT_LIMIT,
            ScopeRestart.FailureCode.UNKNOWN_RESULT, ScopeRestart.FailureCode.MALFORMED_SNAPSHOT
        )
        failure.host?.let {
            update(it.processName, if (unknown) InterfaceScopeRestart.HostState.OUTCOME_UNKNOWN
                else InterfaceScopeRestart.HostState.FAILED)
        }
        return finish(InterfaceScopeRestart.Failure.ROOT_COMMAND_FAILED, failure.code, unknown)
    }

    private fun finish(
        failure: InterfaceScopeRestart.Failure? = null,
        root: ScopeRestart.FailureCode? = null,
        unknown: Boolean = false,
    ): InterfaceScopeRestart.Result {
        val completed = hosts.values.filter { it.state in verifiedStates }.map { it.processName }
        return InterfaceScopeRestart.Result(
            completed, failure, root,
            hosts.values.filter { it.state !in verifiedStates }.map { it.processName },
            hosts.values.toList(), unknown
        ).also { result = it }
    }

    private fun update(name: String, state: InterfaceScopeRestart.HostState, replacement: Int? = null) {
        hosts[name]?.let { hosts[name] = it.copy(state = state, replacementPid = replacement ?: it.replacementPid) }
        publish()
    }

    private fun publish() = progress(active?.host?.processName, hosts.values.toList())

    private fun ModuleStatus.Report.evidence() = ScopeRestart.VerifiedHost(
        packageName, processName, pid, uid, version, revision, buildInstance, frameworkApi
    )

    private companion object {
        val verifiedStates = setOf(InterfaceScopeRestart.HostState.VERIFIED, InterfaceScopeRestart.HostState.INDIRECTLY_VERIFIED)
    }
}

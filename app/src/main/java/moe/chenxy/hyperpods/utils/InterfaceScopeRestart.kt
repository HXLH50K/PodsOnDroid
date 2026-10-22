package moe.chenxy.hyperpods.utils

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

/** The initial management surface never restarts the Bluetooth/control hosts. */
object InterfaceScopeRestart {
    enum class Failure {
        ALREADY_RUNNING, NO_VERIFIED_HOSTS, HOST_UNAVAILABLE, ACTIVE_CALL,
        AUDIO_STATE_UNAVAILABLE, IDENTITY_CHANGED, ROOT_COMMAND_FAILED,
        MODULE_OUTDATED, RELOAD_TIMEOUT, PERMISSION_DENIED, OPERATION_CANCELLED,
        VERIFICATION_INTERRUPTED
    }

    data class Result(
        val completed: List<String>,
        val failure: Failure? = null,
        val rootFailure: ScopeRestart.FailureCode? = null,
    )

    private val operation = Mutex()
    @Volatile var lastResult: Result? = null
        private set

    fun eligible(reports: Collection<ModuleStatus.Report>): List<ModuleStatus.Report> =
        reports.filter {
            ScopeRestart.Host.exact(it.packageName, it.processName)?.affectsControlIpc == false &&
                it.frameworkApi >= 102
        }.sortedBy { it.processName }

    suspend fun run(
        context: Context,
        confirmedProcesses: Set<String>,
        onProgress: (String) -> Unit,
    ): Result {
        if (!operation.tryLock()) return Result(emptyList(), Failure.ALREADY_RUNNING)
        val completed = mutableListOf<String>()
        var signalStarted = false
        lastResult = null
        fun failed(reason: Failure, root: ScopeRestart.FailureCode? = null): Result {
            Log.w("PODS-Scopes", "Interface scope restart stopped: $reason / $root")
            return Result(completed.toList(), reason, root).also { lastResult = it }
        }
        try {
            val allowed = ScopeRestart.Host.entries.filter { !it.affectsControlIpc }.map { it.processName }.toSet()
            if (confirmedProcesses.isEmpty() || !allowed.containsAll(confirmedProcesses)) {
                return failed(Failure.NO_VERIFIED_HOSTS)
            }
            val executor = ScopeRestart()
            for (process in confirmedProcesses.sorted()) {
                onProgress(process)
                val audio = context.getSystemService(AudioManager::class.java)
                    ?: return failed(Failure.AUDIO_STATE_UNAVAILABLE)
                if (audio.mode != AudioManager.MODE_NORMAL) return failed(Failure.ACTIVE_CALL)
                val report = ModuleStatusQuery.collect(context)[process]
                    ?: return failed(Failure.HOST_UNAVAILABLE)
                if (eligible(listOf(report)).isEmpty()) return failed(Failure.IDENTITY_CHANGED)
                val uid = context.packageManager.getPackageUid(report.packageName, 0)
                if (uid != report.uid) return failed(Failure.IDENTITY_CHANGED)
                val evidence = ScopeRestart.VerifiedHost(report.packageName, report.processName,
                    report.pid, uid, report.version, report.revision, report.buildInstance, report.frameworkApi)
                val plan = when (val discovery = executor.discover(ScopeRestart.Preset.UI_HOSTS, listOf(evidence))) {
                    is ScopeRestart.DiscoveryResult.Ready -> discovery.plan
                    is ScopeRestart.DiscoveryResult.Rejected ->
                        return failed(Failure.ROOT_COMMAND_FAILED, discovery.failure.code)
                }
                signalStarted = true
                when (val signal = executor.signal(plan)) {
                    is ScopeRestart.SignalResult.Stopped ->
                        return failed(Failure.ROOT_COMMAND_FAILED, signal.failure.code)
                    is ScopeRestart.SignalResult.Completed -> {
                        if (signal.signalled.size != 1) return failed(Failure.IDENTITY_CHANGED)
                    }
                }
                var replacement: ModuleStatus.Report? = null
                for (attempt in 0 until 8) {
                    val next = ModuleStatusQuery.collect(context, 2000)[process]
                    if (next != null && next.pid != report.pid && next.uid == uid) {
                        replacement = next
                        if (next.matchesInstalled) break
                    }
                }
                if (replacement == null) return failed(Failure.RELOAD_TIMEOUT)
                if (!replacement.matchesInstalled) return failed(Failure.MODULE_OUTDATED)
                completed.add(process)
                signalStarted = false
            }
            return Result(completed.toList()).also { lastResult = it }
        } catch (error: CancellationException) {
            failed(if (signalStarted) Failure.VERIFICATION_INTERRUPTED else Failure.OPERATION_CANCELLED)
            throw error
        } catch (error: PackageManager.NameNotFoundException) {
            Log.e("PODS-Scopes", "Verified host package is unavailable", error)
            return failed(Failure.HOST_UNAVAILABLE)
        } catch (error: SecurityException) {
            Log.e("PODS-Scopes", "Scope management permission denied", error)
            return failed(Failure.PERMISSION_DENIED)
        } finally {
            operation.unlock()
        }
    }
}

package moe.chenxy.hyperpods.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart.Failure
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart.HostState
import moe.chenxy.hyperpods.utils.InterfaceScopeRestart.Mode
import moe.chenxy.hyperpods.utils.ScopeRestart.Command
import moe.chenxy.hyperpods.utils.ScopeRestart.CommandResult
import moe.chenxy.hyperpods.utils.ScopeRestart.FailureCode
import moe.chenxy.hyperpods.utils.ScopeRestart.Host
import moe.chenxy.hyperpods.utils.ScopeRestart.Phase
import org.junit.Assert.*
import org.junit.Test

class ScopeReloadOperationTest {
    @Test fun selectedModeSignalsOnlyTheExplicitlyChosenSubsetInReloadOrder() = runBlocking {
        for (selection in listOf(setOf(Host.SYSTEM_UI), setOf(Host.BLUETOOTH),
            setOf(Host.XIAOMI_BLUETOOTH, Host.SYSTEM_UI))) {
            val fake = Fake()
            val names = selection.map { it.processName }.toSet()
            val result = fake.operation().run(Mode.SELECTED, names)
            assertNull(result.failure)
            assertEquals(ScopeRestart.reloadOrder.filter { it in selection }, fake.signals.map { it.host })
            assertEquals(names, result.completed.toSet())
            assertEquals(names, result.hosts.map { it.processName }.toSet())
        }
    }

    @Test fun selectedModeStillRequiresFreshCallStateAndAllSelectedIdentities() = runBlocking {
        val selected = setOf(Host.SYSTEM_UI.processName)
        val busy = Fake().apply {
            reports[Host.BLUETOOTH.processName] = reports.getValue(Host.BLUETOOTH.processName).copy(callState = "busy")
        }
        assertEquals(Failure.ACTIVE_CALL, busy.operation().run(Mode.SELECTED, selected).failure)
        assertTrue(busy.commands.isEmpty())
        val absent = Fake().apply { reports.remove(Host.SYSTEM_UI.processName) }
        assertEquals(Failure.HOST_UNAVAILABLE, absent.operation().run(Mode.SELECTED, selected).failure)
        assertTrue(absent.commands.isEmpty())
        val empty = Fake()
        assertEquals(Failure.NO_VERIFIED_HOSTS, empty.operation().run(Mode.SELECTED, emptySet()).failure)
        assertTrue(empty.commands.isEmpty())
    }

    @Test fun oldBuildsReloadAllThreeProvidersBeforeConsumersAndVerifyBetweenEverySignal() = runBlocking {
        val fake = Fake()
        val result = fake.operation().run(Mode.ALL, required)
        assertNull(result.failure)
        assertEquals(ScopeRestart.requiredReloadHosts.map { it.processName }, result.completed)
        assertTrue(result.pending.isEmpty())
        assertEquals(ScopeRestart.requiredReloadHosts, fake.signals.map { it.host })
        assertEquals(3, fake.commands.takeWhile { it.phase != Phase.SIGNAL }.size)
        val events = fake.events
        for ((index, host) in ScopeRestart.requiredReloadHosts.withIndex()) {
            val signalIndex = events.indexOf("SIGNAL:${host.processName}:old")
            val verifiedIndex = events.indexOf("SNAPSHOT:${host.processName}:new")
            assertTrue(verifiedIndex > signalIndex)
            if (index < 2) {
                assertTrue(verifiedIndex < events.indexOf("SIGNAL:${ScopeRestart.requiredReloadHosts[index + 1].processName}:old"))
            }
        }
        assertTrue(result.hosts.all { it.state == HostState.VERIFIED && it.originalPid != it.replacementPid })
    }

    @Test fun everyRequiredReportAndActualPackageUidMustPassBeforeAnyRootOrSignal() = runBlocking {
        for (host in ScopeRestart.requiredReloadHosts) {
            val absent = Fake().apply { reports.remove(host.processName) }
            assertEquals(Failure.HOST_UNAVAILABLE, absent.operation().run(Mode.ALL, required).failure)
            assertTrue(absent.commands.isEmpty())
            val wrongUid = Fake().apply {
                reports[host.processName] = reports.getValue(host.processName).copy(uid = 9999)
            }
            assertEquals(Failure.IDENTITY_CHANGED, wrongUid.operation().run(Mode.ALL, required).failure)
            assertTrue(wrongUid.commands.isEmpty())
            val unconfirmed = Fake()
            assertEquals(Failure.NO_VERIFIED_HOSTS,
                unconfirmed.operation().run(Mode.ALL, required - host.processName).failure)
            assertTrue(unconfirmed.commands.isEmpty())
        }
    }

    @Test fun lastRequiredRootPreflightFailureNeverPartiallyKillsTheFirstHost() = runBlocking {
        val fake = Fake().apply {
            overrideCommand = { command ->
                if (command.phase == Phase.SNAPSHOT && command.host == Host.BLUETOOTH) error(FailureCode.UID_MISMATCH) else null
            }
        }
        val result = fake.operation().run(Mode.ALL, required)
        assertEquals(FailureCode.UID_MISMATCH, result.rootFailure)
        assertEquals(3, fake.commands.size)
        assertTrue(fake.signals.isEmpty())
        assertTrue(result.completed.isEmpty())
        assertEquals(required, result.pending.toSet())
    }

    @Test fun busyUnknownAndUnavailableAudioBlockOldAndNewHostsWithoutRoot() = runBlocking {
        for ((call, audio, expected) in listOf(
            Triple("busy", true, Failure.ACTIVE_CALL),
            Triple("unknown", true, Failure.CALL_STATE_UNKNOWN),
            Triple("idle", false, Failure.ACTIVE_CALL),
            Triple("idle", null, Failure.AUDIO_STATE_UNAVAILABLE)
        )) {
            val fake = Fake().apply {
                reports[Host.BLUETOOTH.processName] = reports.getValue(Host.BLUETOOTH.processName).copy(callState = call)
                normalAudio = audio
            }
            assertEquals(expected, fake.operation().run(Mode.ALL, required).failure)
            assertTrue(fake.commands.isEmpty())
        }
    }

    @Test fun aCallStartingAfterPreflightStopsBeforeAnySignal() = runBlocking {
        val fake = Fake().apply { beforeReports = { count -> if (count == 2) normalAudio = false } }
        assertEquals(Failure.ACTIVE_CALL, fake.operation().run(Mode.ALL, required).failure)
        assertEquals(3, fake.commands.size)
        assertTrue(fake.signals.isEmpty())
    }

    @Test fun aLaterRequiredHostDisappearingAfterPreflightStopsEvenTheFirstSignal() = runBlocking {
        val fake = Fake().apply {
            beforeReports = { count -> if (count == 2) reports.remove(Host.SYSTEM_UI.processName) }
        }
        val result = fake.operation().run(Mode.ALL, required)
        assertEquals(Failure.HOST_UNAVAILABLE, result.failure)
        assertTrue(result.completed.isEmpty())
        assertTrue(fake.signals.isEmpty())
    }

    @Test fun eachReplacementMustHaveNewPidSameActualUidAndExactInstalledFingerprint() = runBlocking {
        val mutations: List<(ModuleStatus.Report) -> ModuleStatus.Report> = listOf(
            { it.copy(version = BuildConfig.VERSION_CODE + 1) },
            { it.copy(revision = "87654321") },
            { it.copy(buildInstance = "cached-alpha4") },
            { it.copy(uid = 9999) },
            { it.copy(frameworkApi = 101) }
        )
        for (mutate in mutations) {
            val fake = Fake().apply { afterSignal = { replace(it.host, mutate = mutate) } }
            val result = fake.operation().run(Mode.ALL, required)
            assertNotNull(result.failure)
            assertTrue(result.completed.isEmpty())
            assertEquals(listOf(Host.BLUETOOTH), fake.signals.map { it.host })
            assertEquals(required, result.pending.toSet())
        }
        val ignoredSignal = Fake().apply { afterSignal = {} }
        val result = ignoredSignal.operation().run(Mode.ALL, required)
        assertEquals(Failure.PROCESS_DID_NOT_EXIT, result.failure)
        assertEquals(HostState.OLD_PROCESS_PRESENT, result.hosts.first().state)
        assertEquals(1, ignoredSignal.signals.size)
    }

    @Test fun missingModuleAndExitedOriginalAreDistinctAndStopRemainingRequiredSignals() = runBlocking {
        for (exited in listOf(false, true)) {
            val fake = Fake().apply {
                afterSignal = { command ->
                    reports.remove(command.host.processName)
                    if (exited) identities.remove(command.pid)
                }
            }
            val result = fake.operation().run(Mode.ALL, required)
            assertEquals(Failure.RELOAD_TIMEOUT, result.failure)
            assertEquals(if (exited) HostState.ORIGINAL_PROCESS_ABSENT else HostState.MISSING_MODULE_REPORT,
                result.hosts.first().state)
            assertTrue(result.completed.isEmpty())
            assertEquals(1, fake.signals.size)
        }
    }

    @Test fun missingNextRequiredHostStopsWithEarlierCompletionAndExplicitPendingHosts() = runBlocking {
        val fake = Fake().apply {
            afterSignal = { command ->
                replace(command.host)
                if (command.host == Host.BLUETOOTH) reports.remove(Host.XIAOMI_BLUETOOTH.processName)
            }
        }
        val result = fake.operation().run(Mode.ALL, required)
        assertEquals(Failure.HOST_UNAVAILABLE, result.failure)
        assertEquals(listOf(Host.BLUETOOTH.processName), result.completed)
        assertEquals(setOf(Host.XIAOMI_BLUETOOTH.processName, Host.SYSTEM_UI.processName), result.pending.toSet())
        assertEquals(1, fake.signals.size)
    }

    @Test fun absentOrLazyOptionalUiNeverBlocksTheThreeRequiredHostsOrClaimsVerification() = runBlocking {
        for (observed in listOf(false, true)) {
            val fake = Fake(optional = observed).apply {
                afterSignal = { command ->
                    if (command.host == Host.XIAOMI_UI) {
                        reports.remove(command.host.processName)
                        identities.remove(command.pid)
                    } else replace(command.host)
                }
            }
            val result = fake.operation().run(Mode.ALL, required + Host.XIAOMI_UI.processName)
            assertNull(result.failure)
            assertEquals(required, result.completed.toSet())
            assertEquals(listOf(Host.XIAOMI_UI.processName), result.pending)
            assertEquals(HostState.AWAITING_NORMAL_RELAUNCH, result.hosts.last().state)
            assertEquals(if (observed) ScopeRestart.reloadOrder else ScopeRestart.requiredReloadHosts, fake.signals.map { it.host })
        }
    }

    @Test fun optionalUiDisappearingDuringPreflightOrIndirectRestartIsNotRequired() = runBlocking {
        for (duringPreflight in listOf(false, true)) {
            val fake = Fake(optional = true).apply {
                if (duringPreflight) identities.remove(reports.getValue(Host.XIAOMI_UI.processName).pid)
                afterSignal = { command ->
                    replace(command.host)
                    if (command.host == Host.BLUETOOTH) {
                        reports.remove(Host.XIAOMI_UI.processName)?.let { identities.remove(it.pid) }
                    }
                }
            }
            val result = fake.operation().run(Mode.ALL, required + Host.XIAOMI_UI.processName)
            assertNull(result.failure)
            assertEquals(required, result.completed.toSet())
            assertEquals(HostState.AWAITING_NORMAL_RELAUNCH, result.hosts.last().state)
            assertEquals(ScopeRestart.requiredReloadHosts, fake.signals.map { it.host })
        }
    }

    @Test fun indirectReplacementIsAcceptedOnlyWithFreshCurrentBuildProofAndNoSecondSignal() = runBlocking {
        for (currentBuild in listOf(false, true)) {
            val fake = Fake().apply {
                afterSignal = { command ->
                    replace(command.host)
                    if (command.host == Host.BLUETOOTH) replace(Host.XIAOMI_BLUETOOTH, current = currentBuild)
                }
            }
            val result = fake.operation().run(Mode.ALL, required)
            if (currentBuild) {
                assertNull(result.failure)
                assertEquals(required, result.completed.toSet())
                assertEquals(HostState.INDIRECTLY_VERIFIED, result.hosts[1].state)
                assertEquals(listOf(Host.BLUETOOTH, Host.SYSTEM_UI), fake.signals.map { it.host })
            } else {
                assertEquals(Failure.MODULE_OUTDATED, result.failure)
                assertEquals(HostState.OLD_MODULE, result.hosts[1].state)
                assertEquals(listOf(Host.BLUETOOTH), fake.signals.map { it.host })
            }
        }
    }

    @Test fun pidReuseBetweenPreflightAndSignalIsRejectedWithoutSignallingAnotherHost() = runBlocking {
        val fake = Fake().apply {
            beforeReports = { count ->
                if (count == 2) {
                    val pid = reports.getValue(Host.BLUETOOTH.processName).pid
                    identities[pid] = identities.getValue(pid).copy(start = 999)
                }
            }
        }
        val result = fake.operation().run(Mode.ALL, required)
        assertEquals(FailureCode.PID_REUSED, result.rootFailure)
        assertTrue(result.completed.isEmpty())
        assertEquals(1, fake.signals.size)
        assertEquals(Host.BLUETOOTH, fake.signals.single().host)
    }

    @Test fun unknownSignalOutcomeIsNotSuccessfulOrAutomaticallyRetried() = runBlocking {
        val fake = Fake().apply {
            overrideCommand = { command ->
                if (command.phase == Phase.SIGNAL) CommandResult.Failed(ScopeRestart.CommandFailure.TIMEOUT) else null
            }
        }
        val result = fake.operation().run(Mode.ALL, required)
        assertEquals(FailureCode.TIMEOUT, result.rootFailure)
        assertTrue(result.signalOutcomeUnknown)
        assertEquals(HostState.OUTCOME_UNKNOWN, result.hosts.first().state)
        assertEquals(1, fake.signals.size)
    }

    @Test fun finalAuditDoesNotKeepSuccessForARequiredHostThatHasSinceDisappeared() = runBlocking {
        val fake = Fake()
        var finalQueries = 0
        fake.beforeReports = {
            if (fake.signals.size == 3 && ++finalQueries == 2) fake.reports.remove(Host.BLUETOOTH.processName)
        }
        val result = fake.operation().run(Mode.ALL, required)
        assertEquals(Failure.HOST_UNAVAILABLE, result.failure)
        assertFalse(result.completed.contains(Host.BLUETOOTH.processName))
        assertTrue(result.pending.contains(Host.BLUETOOTH.processName))
    }

    @Test fun interfaceOnlyOptionRemainsAvailableWithoutControlExitApproval() = runBlocking {
        val fake = Fake(optional = true)
        val result = fake.operation().run(Mode.INTERFACE_ONLY, setOf(Host.SYSTEM_UI.processName, Host.XIAOMI_UI.processName))
        assertNull(result.failure)
        assertEquals(listOf(Host.SYSTEM_UI, Host.XIAOMI_UI), fake.signals.map { it.host })
        assertTrue(fake.signals.none { it.host.affectsControlIpc })
    }

    @Test fun cancellationBeforeFirstSignalStopsWorkAndRetainsAnExplicitResult() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val fake = Fake().apply {
            beforeReports = { if (it == 2) { entered.complete(Unit); awaitCancellation() } }
        }
        val operation = fake.operation()
        val job = launch { operation.run(Mode.ALL, required) }
        entered.await()
        job.cancelAndJoin()
        assertEquals(Failure.OPERATION_CANCELLED, operation.result?.failure)
        assertFalse(requireNotNull(operation.result).signalOutcomeUnknown)
        assertTrue(fake.signals.isEmpty())
    }

    @Test fun cancellationDuringSignalOrVerificationRecordsUnknownRecoveryAndStopsFurtherHosts() = runBlocking {
        for (duringSignal in listOf(false, true)) {
            val entered = CompletableDeferred<Unit>()
            val fake = Fake().apply {
                if (duringSignal) afterSignal = {
                    replace(it.host)
                    entered.complete(Unit)
                    awaitCancellation()
                } else beforeReports = {
                    if (signals.isNotEmpty()) { entered.complete(Unit); awaitCancellation() }
                }
            }
            val operation = fake.operation()
            val job = launch { operation.run(Mode.ALL, required) }
            entered.await()
            job.cancelAndJoin()
            assertEquals(Failure.VERIFICATION_INTERRUPTED, operation.result?.failure)
            assertTrue(requireNotNull(operation.result).signalOutcomeUnknown)
            assertEquals(HostState.OUTCOME_UNKNOWN, operation.result?.hosts?.first()?.state)
            assertEquals(1, fake.signals.size)
        }
    }

    private class Fake(optional: Boolean = false) : ScopeReloadEnvironment, ScopeRestart.CommandRunner {
        data class Identity(val host: Host, val uid: Int, val start: Long)
        val reports = linkedMapOf<String, ModuleStatus.Report>()
        val identities = mutableMapOf<Int, Identity>()
        val commands = mutableListOf<Command>()
        val signals get() = commands.filter { it.phase == Phase.SIGNAL }
        val events = mutableListOf<String>()
        var normalAudio: Boolean? = true
        var reportCount = 0
        var beforeReports: suspend (Int) -> Unit = {}
        var afterSignal: suspend (Command) -> Unit = { replace(it.host) }
        var overrideCommand: (Command) -> CommandResult? = { null }

        init {
            (if (optional) ScopeRestart.reloadOrder else ScopeRestart.requiredReloadHosts).forEach { host ->
                val pid = 400 + host.ordinal
                val uid = packageUid(host.packageName)
                reports[host.processName] = ModuleStatus.Report(
                    host.packageName, 4, "12345678", "receiver_ready", host.processName, pid, uid,
                    102, "LSPosed", "old-alpha4", if (host == Host.BLUETOOTH) "idle" else "unknown"
                )
                identities[pid] = Identity(host, uid, 55)
            }
        }

        fun operation() = ScopeReloadOperation(ScopeRestart(this), this)
        override fun audioIsNormal() = normalAudio
        override fun packageUid(packageName: String) = if (packageName == Host.SYSTEM_UI.packageName) 1000 else 1002
        override suspend fun reports(timeoutMs: Long): Map<String, ModuleStatus.Report> {
            assertTrue(timeoutMs in 100..10_000)
            beforeReports(++reportCount)
            events.add("reports")
            return reports.toMap()
        }

        fun replace(
            host: Host,
            current: Boolean = true,
            mutate: (ModuleStatus.Report) -> ModuleStatus.Report = { it },
        ) {
            val old = reports.getValue(host.processName)
            identities.remove(old.pid)
            val replacement = mutate(old.copy(
                pid = old.pid + 1000,
                version = if (current) BuildConfig.VERSION_CODE else old.version,
                revision = if (current) BuildConfig.BUILD_REVISION else old.revision,
                buildInstance = if (current) BuildConfig.BUILD_INSTANCE else old.buildInstance
            ))
            reports[host.processName] = replacement
            identities[replacement.pid] = Identity(host, replacement.uid, 155)
        }

        override suspend fun run(command: Command): CommandResult {
            commands.add(command)
            events.add("${command.phase}:${command.host.processName}:${if (command.pid < 1000) "old" else "new"}")
            overrideCommand(command)?.let { return it }
            val identity = identities[command.pid] ?: return error(FailureCode.PROCESS_VANISHED)
            if (identity.uid != command.uid) return error(FailureCode.UID_MISMATCH)
            if (identity.host != command.host) return error(FailureCode.PROCESS_NAME_MISMATCH)
            if (command.startTimeTicks != null && command.startTimeTicks != identity.start) return error(FailureCode.PID_REUSED)
            if (command.phase == Phase.SIGNAL) afterSignal(command)
            val hex = command.host.processName.map { it.code.toString(16).padStart(2, '0') }.joinToString("") + "00"
            val kind = if (command.phase == Phase.SNAPSHOT) "SNAPSHOT" else "SIGNALLED"
            return CommandResult.Exited(0, "PODS_SCOPE_1|$kind|${command.pid}|${command.uid}|${identity.start}|$hex\n")
        }
    }

    private companion object {
        val required = ScopeRestart.requiredReloadHosts.map { it.processName }.toSet()
        fun error(code: FailureCode) = CommandResult.Exited(1, "PODS_SCOPE_1|ERROR|${code.name}\n")
    }
}

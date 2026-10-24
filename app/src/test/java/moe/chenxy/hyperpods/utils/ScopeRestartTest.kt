package moe.chenxy.hyperpods.utils

import kotlinx.coroutines.runBlocking
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.ScopeRestart.Command
import moe.chenxy.hyperpods.utils.ScopeRestart.CommandFailure
import moe.chenxy.hyperpods.utils.ScopeRestart.CommandResult
import moe.chenxy.hyperpods.utils.ScopeRestart.DiscoveryResult
import moe.chenxy.hyperpods.utils.ScopeRestart.FailureCode
import moe.chenxy.hyperpods.utils.ScopeRestart.Host
import moe.chenxy.hyperpods.utils.ScopeRestart.Phase
import moe.chenxy.hyperpods.utils.ScopeRestart.Preset
import moe.chenxy.hyperpods.utils.ScopeRestart.SignalResult
import moe.chenxy.hyperpods.utils.ScopeRestart.VerifiedHost
import org.junit.Assert.*
import org.junit.Test

class ScopeRestartTest {
    private class Runner(
        val reply: (Command) -> CommandResult = { response(it) }
    ) : ScopeRestart.CommandRunner {
        val commands = ArrayList<Command>()
        override suspend fun run(command: Command): CommandResult {
            commands.add(command)
            return reply(command)
        }
    }

    @Test fun explicitExitRemainsOptInInEveryBuildVariant() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val evidence = listOf(host(Host.BLUETOOTH))
        val selected = setOf(Host.BLUETOOTH)
        val denied = core.discoverProcessExit(evidence, false, selected) as ScopeRestart.ProcessExitDiscoveryResult.Rejected
        assertEquals(FailureCode.PROCESS_EXIT_NOT_APPROVED, denied.failure.code)
        assertTrue(runner.commands.isEmpty())
        val ready = core.discoverProcessExit(evidence, true, selected) as ScopeRestart.ProcessExitDiscoveryResult.Ready
        assertTrue(core.signalProcessExit(ready.plan, ready.plan.targets.single()) is SignalResult.Completed)
    }

    @Test fun selectedApprovalCannotAddAnUnselectedHost() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val selected = setOf(Host.BLUETOOTH)
        val rejected = core.discoverProcessExit(
            listOf(host(Host.BLUETOOTH), host(Host.SYSTEM_UI)), true, selected)
            as ScopeRestart.ProcessExitDiscoveryResult.Rejected
        assertEquals(FailureCode.UNAUTHORIZED_HOST, rejected.failure.code)
        assertTrue(runner.commands.isEmpty())
        val plan = (core.discoverProcessExit(listOf(host(Host.BLUETOOTH)), true, selected)
            as ScopeRestart.ProcessExitDiscoveryResult.Ready).plan
        assertEquals(listOf(Host.BLUETOOTH), plan.targets.map { it.host })
        assertTrue(core.signalProcessExit(plan, plan.targets.single()) is SignalResult.Completed)
    }

    @Test fun explicitExitNeedsImmutableApprovalAndAllThreeRequiredHosts() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val required = ScopeRestart.requiredReloadHosts.map { host(it) }
        val unapproved = core.discoverProcessExit(required, false) as ScopeRestart.ProcessExitDiscoveryResult.Rejected
        assertEquals(FailureCode.PROCESS_EXIT_NOT_APPROVED, unapproved.failure.code)
        for (missing in ScopeRestart.requiredReloadHosts) {
            val rejected = core.discoverProcessExit(required.filter { it.processName != missing.processName }, true)
                as ScopeRestart.ProcessExitDiscoveryResult.Rejected
            assertEquals(FailureCode.MISSING_REQUIRED_HOST, rejected.failure.code)
        }
        assertTrue(runner.commands.isEmpty())
        val plan = (core.discoverProcessExit(required, true) as ScopeRestart.ProcessExitDiscoveryResult.Ready).plan
        assertEquals(ScopeRestart.requiredReloadHosts, plan.targets.map { it.host })
        try {
            (plan.targets as MutableList<*>).clear()
            fail("Process-exit approval must be immutable")
        } catch (_: UnsupportedOperationException) {
            assertEquals(3, plan.targets.size)
        }
        val target = plan.targets.first()
        assertEquals(FailureCode.INVALID_PLAN,
            (ScopeRestart(runner).signalProcessExit(plan, target) as SignalResult.Stopped).failure.code)
        assertEquals(3, runner.commands.size)
        assertTrue(core.signalProcessExit(plan, target) is SignalResult.Completed)
        assertEquals(FailureCode.PLAN_ALREADY_USED,
            (core.signalProcessExit(plan, target) as SignalResult.Stopped).failure.code)
    }

    @Test fun explicitExitDoesNotBypassNormalControlShutdownGuardOrAuthorizeUnapprovedIdentity() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val evidence = ScopeRestart.requiredReloadHosts.map { host(it) }
        val normal = (core.discover(Preset.ALL_VERIFIED, evidence) as DiscoveryResult.Ready).plan
        assertEquals(FailureCode.CONTROL_IPC_NOT_STOPPED, (core.signal(normal) as SignalResult.Stopped).failure.code)
        val approved = (core.discoverProcessExit(evidence, true) as ScopeRestart.ProcessExitDiscoveryResult.Ready).plan
        val changed = approved.targets.first().copy(pid = 900)
        assertEquals(FailureCode.INVALID_PLAN,
            (core.signalProcessExit(approved, changed) as SignalResult.Stopped).failure.code)
        assertTrue(runner.commands.none { it.phase == Phase.SIGNAL })
    }

    @Test fun signalsRevalidateOriginalStartTimeAndNeverResnapshotToAuthorizePidReuse() = runBlocking {
        val runner = Runner { if (it.phase == Phase.SIGNAL) response(it, start = 999) else response(it) }
        val core = ScopeRestart(runner)
        val plan = (core.discoverProcessExit(ScopeRestart.requiredReloadHosts.map { host(it) }, true)
            as ScopeRestart.ProcessExitDiscoveryResult.Ready).plan
        assertEquals(FailureCode.PID_REUSED,
            (core.signalProcessExit(plan, plan.targets.first()) as SignalResult.Stopped).failure.code)
    }

    @Test fun rejectsUnauthorizedNamesAndInjectionBeforeStartingRoot() = runBlocking {
        for (name in listOf(
            "system_server", "zygote", "zygote64", "su", "com.milink.service",
            "com.android.bluetooth:other", "com.xiaomi.bluetooth:ui:other",
            "com.android.systemui;reboot", "com.android.systemui\n", "com.android.systemui\u0000",
            "com.android.systemui*", "../com.android.systemui", ""
        )) {
            val runner = Runner()
            val core = ScopeRestart(runner)
            val evidence = host(Host.SYSTEM_UI).copy(processName = name)
            assertRejected(FailureCode.UNAUTHORIZED_HOST, core.discover(Preset.ALL_VERIFIED, listOf(evidence)))
            assertTrue(runner.commands.isEmpty())
        }
        val runner = Runner()
        assertRejected(FailureCode.UNAUTHORIZED_HOST, ScopeRestart(runner).discover(
            Preset.ALL_VERIFIED,
            listOf(host(Host.SYSTEM_UI).copy(packageName = "com.android.bluetooth"))
        ))
        assertTrue(runner.commands.isEmpty())
    }

    @Test fun rejectsPidOneRootUidAndInvalidEvidenceBeforeRoot() = runBlocking {
        for (pid in listOf(Int.MIN_VALUE, -1, 0, 1)) {
            val runner = Runner()
            assertRejected(FailureCode.INVALID_PID, ScopeRestart(runner).discover(
                Preset.ALL_VERIFIED, listOf(host(Host.SYSTEM_UI).copy(pid = pid))
            ))
            assertTrue(runner.commands.isEmpty())
        }
        for (uid in listOf(-1, 0)) {
            val runner = Runner()
            assertRejected(FailureCode.INVALID_UID, ScopeRestart(runner).discover(
                Preset.ALL_VERIFIED, listOf(host(Host.SYSTEM_UI).copy(expectedUid = uid))
            ))
            assertTrue(runner.commands.isEmpty())
        }
        for (evidence in listOf(
            host(Host.SYSTEM_UI).copy(revision = "x;reboot"),
            host(Host.SYSTEM_UI).copy(buildInstance = "$(id)"),
            host(Host.SYSTEM_UI).copy(buildInstance = ""),
            host(Host.SYSTEM_UI).copy(frameworkApi = 0),
            host(Host.SYSTEM_UI).copy(version = 0)
        )) {
            val runner = Runner()
            assertRejected(FailureCode.INVALID_EVIDENCE, ScopeRestart(runner).discover(
                Preset.ALL_VERIFIED, listOf(evidence)
            ))
            assertTrue(runner.commands.isEmpty())
        }
    }

    @Test fun frameworkApiMustBeWithinAuthenticatedModernBounds() = runBlocking {
        for (api in listOf(82, 101, 1001)) {
            val runner = Runner()
            assertRejected(FailureCode.INVALID_EVIDENCE, ScopeRestart(runner).discover(
                Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI).copy(frameworkApi = api))
            ))
            assertTrue(runner.commands.isEmpty())
        }
        for (api in listOf(102, 1000)) {
            val runner = Runner()
            assertTrue(ScopeRestart(runner).discover(
                Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI).copy(frameworkApi = api))
            ) is DiscoveryResult.Ready)
            assertEquals(1, runner.commands.size)
        }
    }

    @Test fun uiPresetUsesOnlyObservedUiHostsAndDoesNotRequireAacpDisconnection() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val ready = core.discover(Preset.UI_HOSTS, Host.entries.map { host(it) }) as DiscoveryResult.Ready
        assertEquals(listOf(Host.SYSTEM_UI, Host.XIAOMI_UI), ready.plan.targets.map { it.host })
        assertFalse(ready.plan.requiresControlIpcStop)
        val result = core.signal(ready.plan) as SignalResult.Completed
        assertEquals(ready.plan.targets, result.signalled)
        assertEquals(listOf(Phase.SNAPSHOT, Phase.SNAPSHOT, Phase.SIGNAL, Phase.SIGNAL),
            runner.commands.map { it.phase })
        assertTrue(runner.commands.none { it.host.affectsControlIpc })
    }

    @Test fun noPresetGuessesAbsentPackagesOrSubprocesses() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val ready = core.discover(Preset.ALL_VERIFIED, listOf(host(Host.BLUETOOTH))) as DiscoveryResult.Ready
        assertEquals(listOf(Host.BLUETOOTH), ready.plan.targets.map { it.host })
        assertTrue(ready.plan.requiresControlIpcStop)
        assertRejected(FailureCode.NO_VERIFIED_TARGETS,
            core.discover(Preset.UI_HOSTS, listOf(host(Host.XIAOMI_BLUETOOTH), host(Host.BLUETOOTH))))
        assertRejected(FailureCode.NO_VERIFIED_TARGETS, core.discover(Preset.ALL_VERIFIED, emptyList()))
        assertEquals(1, runner.commands.size)
    }

    @Test fun allVerifiedIncludesOnlyTheFourExactSupportedHosts() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val ready = core.discover(Preset.ALL_VERIFIED, Host.entries.reversed().map { host(it) })
            as DiscoveryResult.Ready
        assertEquals(Host.entries, ready.plan.targets.map { it.host })
        assertTrue(ready.plan.requiresControlIpcStop)
        val blocked = core.signal(ready.plan) as SignalResult.Stopped
        assertEquals(FailureCode.CONTROL_IPC_NOT_STOPPED, blocked.failure.code)
        assertEquals(4, runner.commands.size)
        assertEquals(4, (core.signal(ready.plan, controlIpcStopped = true) as SignalResult.Completed).signalled.size)
    }

    @Test fun duplicatePidsAndHostsAreRejectedBeforeDiscovery() = runBlocking {
        for (evidence in listOf(
            listOf(host(Host.SYSTEM_UI), host(Host.SYSTEM_UI).copy(pid = 999)),
            listOf(host(Host.SYSTEM_UI), host(Host.BLUETOOTH).copy(pid = host(Host.SYSTEM_UI).pid))
        )) {
            val runner = Runner()
            assertRejected(FailureCode.DUPLICATE_TARGET,
                ScopeRestart(runner).discover(Preset.ALL_VERIFIED, evidence))
            assertTrue(runner.commands.isEmpty())
        }
    }

    @Test fun sharedUidNeverAuthorizesAnUnrelatedCmdline() = runBlocking {
        val runner = Runner { response(it, cmdline = encode("system_server") + "00") }
        assertRejected(FailureCode.PROCESS_NAME_MISMATCH, ScopeRestart(runner).discover(
            Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI).copy(expectedUid = 1000))
        ))
        assertEquals(1, runner.commands.size)
    }

    @Test fun nulPaddingIsAcceptedButArgumentsAndPrefixesAreNot() = runBlocking {
        val name = Host.SYSTEM_UI.processName
        for (cmdline in listOf(
            encode(name), encode(name) + "00" + encode("argument") + "00",
            encode("$name:child") + "00", encode("$name\n") + "00", "00"
        )) {
            val runner = Runner { response(it, cmdline = cmdline) }
            assertRejected(FailureCode.PROCESS_NAME_MISMATCH, ScopeRestart(runner).discover(
                Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI))
            ))
        }
        val runner = Runner { response(it, cmdline = encode(name) + "00000000") }
        assertTrue(ScopeRestart(runner).discover(Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI)))
            is DiscoveryResult.Ready)
    }

    @Test fun malformedOutputAndNumericInjectionFailClosed() = runBlocking {
        val hex = encode(Host.SYSTEM_UI.processName) + "00"
        for (output in listOf(
            "", "OK", "PODS_SCOPE_1|SNAPSHOT|1;reboot|1000|55|$hex\n",
            "PODS_SCOPE_1|SNAPSHOT|+400|1000|55|$hex\n",
            "PODS_SCOPE_1|SNAPSHOT|0400|1000|55|$hex\n",
            "PODS_SCOPE_1|SNAPSHOT|400|1000|9223372036854775808|$hex\n",
            "PODS_SCOPE_1|SNAPSHOT|400|1000|0|$hex\n",
            "PODS_SCOPE_1|SNAPSHOT|400|1000|55|abc\n",
            "PODS_SCOPE_1|SNAPSHOT|400|1000|55|zz\n",
            "PODS_SCOPE_1|SNAPSHOT|400|1000|55|$hex\nuntrusted\n",
            "PODS_SCOPE_1|SNAPSHOT|400|1000|55|$hex\r\n"
        )) {
            val runner = Runner { CommandResult.Exited(0, output) }
            assertRejected(FailureCode.MALFORMED_SNAPSHOT, ScopeRestart(runner).discover(
                Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI))
            ))
        }
    }

    @Test fun pidUidAndStartTimeMismatchPreventSignalAcknowledgement() = runBlocking {
        for ((code, alter) in listOf<Pair<FailureCode, (Command) -> CommandResult>>(
            FailureCode.INVALID_PID to { response(it, pid = 1) },
            FailureCode.INVALID_PID to { response(it, pid = it.pid + 1) },
            FailureCode.UID_MISMATCH to { response(it, uid = it.uid + 1) },
            FailureCode.PID_REUSED to { response(it, start = 999) }
        )) {
            val runner = Runner { if (it.phase == Phase.SIGNAL) alter(it) else response(it) }
            val core = ScopeRestart(runner)
            val ready = core.discover(Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI))) as DiscoveryResult.Ready
            val result = core.signal(ready.plan) as SignalResult.Stopped
            assertEquals(code, result.failure.code)
            assertTrue(result.signalled.isEmpty())
        }
        val runner = Runner { response(it, uid = it.uid + 1) }
        assertRejected(FailureCode.UID_MISMATCH, ScopeRestart(runner).discover(
            Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI))
        ))
    }

    @Test fun rootDeniedTimeoutIoAndOutputLimitDoNotProduceFallbackSuccess() = runBlocking {
        for (reason in CommandFailure.entries) {
            val runner = Runner { CommandResult.Failed(reason) }
            assertRejected(FailureCode.valueOf(reason.name), ScopeRestart(runner).discover(
                Preset.ALL_VERIFIED, listOf(host(Host.SYSTEM_UI), host(Host.BLUETOOTH))
            ))
            assertEquals(1, runner.commands.size)
        }
        val excessive = Runner { CommandResult.Exited(0, "x".repeat(16_385)) }
        assertRejected(FailureCode.OUTPUT_LIMIT, ScopeRestart(excessive).discover(
            Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI))
        ))
        for (exit in listOf(124, 137)) {
            val runner = Runner { CommandResult.Exited(exit, "") }
            assertRejected(FailureCode.TIMEOUT, ScopeRestart(runner).discover(
                Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI))
            ))
        }
    }

    @Test fun suDenialAndUnknownExitAreNeverTreatedAsSignalAcknowledgement() = runBlocking {
        for ((output, expected) in listOf(
            "su: Permission denied\n" to FailureCode.ROOT_DENIED,
            "not allowed\n" to FailureCode.ROOT_DENIED,
            "unrecognized failure\n" to FailureCode.ROOT_UNAVAILABLE,
            "" to FailureCode.ROOT_UNAVAILABLE
        )) {
            val runner = Runner { CommandResult.Exited(1, output) }
            assertRejected(expected, ScopeRestart(runner).discover(
                Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI))
            ))
        }
    }

    @Test fun vanishedDeniedAndReusedProcessErrorsAreTypedAndStopDiscovery() = runBlocking {
        for (code in listOf(FailureCode.PROCESS_VANISHED, FailureCode.ROOT_DENIED,
            FailureCode.UID_MISMATCH, FailureCode.PID_REUSED, FailureCode.PROCESS_NAME_MISMATCH)) {
            val runner = Runner { error(code) }
            assertRejected(code, ScopeRestart(runner).discover(Preset.ALL_VERIFIED, Host.entries.map { host(it) }))
            assertEquals(1, runner.commands.size)
        }
    }

    @Test fun signalFailureStopsSequentialExecutionAndReportsOnlyEarlierAcknowledgements() = runBlocking {
        for (failure in listOf(
            error(FailureCode.PROCESS_VANISHED), error(FailureCode.PID_REUSED),
            error(FailureCode.UID_MISMATCH), error(FailureCode.SIGNAL_FAILED),
            CommandResult.Failed(CommandFailure.ROOT_DENIED),
            CommandResult.Failed(CommandFailure.TIMEOUT),
            CommandResult.Exited(0, "unknown")
        )) {
            val runner = Runner {
                if (it.phase == Phase.SIGNAL && it.host == Host.XIAOMI_UI) failure else response(it)
            }
            val core = ScopeRestart(runner)
            val ready = core.discover(Preset.ALL_VERIFIED, Host.entries.map { host(it) }) as DiscoveryResult.Ready
            val result = core.signal(ready.plan, controlIpcStopped = true) as SignalResult.Stopped
            assertEquals(listOf(Host.SYSTEM_UI), result.signalled.map { it.host })
            assertEquals(listOf(Host.SYSTEM_UI, Host.XIAOMI_UI),
                runner.commands.filter { it.phase == Phase.SIGNAL }.map { it.host })
            assertEquals(FailureCode.PLAN_ALREADY_USED,
                (core.signal(ready.plan, controlIpcStopped = true) as SignalResult.Stopped).failure.code)
        }
    }

    @Test fun plansCannotBeMutatedReusedOrPassedToAnotherExecutor() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val evidence = arrayListOf(host(Host.SYSTEM_UI))
        val ready = core.discover(Preset.UI_HOSTS, evidence) as DiscoveryResult.Ready
        evidence.clear()
        assertEquals(1, ready.plan.targets.size)
        try {
            (ready.plan.targets as MutableList<*>).clear()
            fail("Targets must be immutable")
        } catch (_: UnsupportedOperationException) {
            assertEquals(1, ready.plan.targets.size)
        }
        val otherRunner = Runner()
        assertEquals(FailureCode.INVALID_PLAN,
            (ScopeRestart(otherRunner).signal(ready.plan) as SignalResult.Stopped).failure.code)
        assertTrue(otherRunner.commands.isEmpty())
        assertTrue(core.signal(ready.plan) is SignalResult.Completed)
        assertEquals(FailureCode.PLAN_ALREADY_USED,
            (core.signal(ready.plan) as SignalResult.Stopped).failure.code)
        assertEquals(2, runner.commands.size)
    }

    @Test fun generatedScriptUsesFixedToolsExactPidAndRootSideDeadline() = runBlocking {
        val runner = Runner()
        val core = ScopeRestart(runner)
        val ready = core.discover(Preset.UI_HOSTS, listOf(host(Host.SYSTEM_UI))) as DiscoveryResult.Ready
        core.signal(ready.plan)
        val snapshot = ScopeRestartScript.generate(runner.commands[0])
        val signal = ScopeRestartScript.generate(runner.commands[1])
        assertTrue(signal.startsWith("exec /system/bin/toybox timeout -s KILL 6 /system/bin/sh -c '"))
        assertTrue(signal.contains("proc=/proc/400"))
        assertTrue(signal.contains("uid=1000"))
        assertTrue(signal.contains("kill -TERM \"${'$'}pid\""))
        assertTrue(signal.contains("fail PID_REUSED"))
        assertTrue(signal.contains("fail UID_MISMATCH"))
        assertTrue(signal.contains("stat_rest=${'$'}{stat_line##*) }"))
        assertTrue(signal.indexOf("initial_start=${'$'}start") < signal.indexOf("kill -TERM"))
        assertFalse(snapshot.contains("kill -TERM"))
        for (forbidden in listOf("killall", "pkill", "force-stop", "reboot", "milink")) {
            assertFalse(signal.contains(forbidden))
        }
        assertFalse(signal.contains("build-123"))
    }

    companion object {
        private fun host(host: Host) = VerifiedHost(
            host.packageName, host.processName, 400 + host.ordinal,
            if (host == Host.SYSTEM_UI) 1000 else 1002 + host.ordinal,
            28, "12345678", "build-123", 102
        )

        private fun encode(value: String) = value.map { it.code.toString(16).padStart(2, '0') }.joinToString("")

        private fun response(
            command: Command,
            pid: Int = command.pid,
            uid: Int = command.uid,
            start: Long = command.startTimeTicks ?: 55,
            cmdline: String = encode(command.host.processName) + "00"
        ): CommandResult = CommandResult.Exited(
            0, "PODS_SCOPE_1|${if (command.phase == Phase.SNAPSHOT) "SNAPSHOT" else "SIGNALLED"}|" +
                "$pid|$uid|$start|$cmdline\n"
        )

        private fun error(code: FailureCode): CommandResult =
            CommandResult.Exited(1, "PODS_SCOPE_1|ERROR|${code.name}\n")

        private fun assertRejected(code: FailureCode, result: DiscoveryResult) {
            assertTrue("Expected rejection, got $result", result is DiscoveryResult.Rejected)
            assertEquals(code, (result as DiscoveryResult.Rejected).failure.code)
        }
    }
}

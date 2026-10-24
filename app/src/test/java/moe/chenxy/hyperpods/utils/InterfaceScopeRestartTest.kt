package moe.chenxy.hyperpods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterfaceScopeRestartTest {
    private fun report(host: String, process: String = host, api: Int = 102) =
        ModuleStatus.Report(host, 1, "12345678", "receiver_ready", process, 1234, 1000,
            api, "LSPosed", "test-build", "unknown")

    @Test fun theSecondaryInterfaceOptionNeverOffersBluetoothControlOrUnhookedMiLink() {
        val reports = listOf(report(HyperPodsBroadcasts.SYSTEM_UI),
            report(HyperPodsBroadcasts.XIAOMI_BLUETOOTH),
            report(HyperPodsBroadcasts.BLUETOOTH), report("com.milink.service"))
        assertEquals(listOf(HyperPodsBroadcasts.SYSTEM_UI),
            InterfaceScopeRestart.eligible(reports).map { it.processName })
    }

    @Test fun theXiaomiUiHostMustHaveItsOwnAuthenticatedReport() {
        val host = HyperPodsBroadcasts.XIAOMI_BLUETOOTH
        assertTrue(InterfaceScopeRestart.eligible(listOf(report(host))).isEmpty())
        assertEquals(listOf("$host:ui"),
            InterfaceScopeRestart.eligible(listOf(report(host, "$host:ui"))).map { it.processName })
    }

    @Test fun legacyFrameworkEvidenceCannotExposeAProcessRestart() {
        assertTrue(InterfaceScopeRestart.eligible(listOf(report(HyperPodsBroadcasts.SYSTEM_UI, api = 101))).isEmpty())
    }

    @Test fun fullReloadAcceptsOldAlphaReportsWithoutRequiringNewCapabilities() {
        val reports = ScopeRestart.Host.entries.map { report(it.packageName, it.processName).copy(
            version = 4, buildInstance = "old-alpha4", callState = if (it == ScopeRestart.Host.BLUETOOTH) "idle" else "unknown"
        ) }
        assertEquals(ScopeRestart.reloadOrder.map { it.processName },
            InterfaceScopeRestart.eligible(reports, InterfaceScopeRestart.Mode.ALL).map { it.processName })
    }

    @Test fun fullReloadRequiresExactProcessesValidPidUidAndModernApi() {
        val valid = report(HyperPodsBroadcasts.BLUETOOTH)
        for (invalid in listOf(valid.copy(processName = "${valid.processName}:other"),
            valid.copy(pid = 1), valid.copy(uid = 0), valid.copy(frameworkApi = 101),
            valid.copy(frameworkApi = 1001), valid.copy(packageName = "system_server"))) {
            assertTrue(InterfaceScopeRestart.eligible(listOf(invalid), InterfaceScopeRestart.Mode.ALL).isEmpty())
        }
    }
}

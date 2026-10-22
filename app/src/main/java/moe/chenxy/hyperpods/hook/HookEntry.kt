package moe.chenxy.hyperpods.hook

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts

class HookEntry : XposedModule() {
    private lateinit var processName: String
    private val installedPackages = mutableSetOf<String>()

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageReady(param: PackageReadyParam) {
        val host = param.packageName
        if (host !in setOf(HyperPodsBroadcasts.SYSTEM_UI, HyperPodsBroadcasts.BLUETOOTH,
                HyperPodsBroadcasts.XIAOMI_BLUETOOTH) ||
            !param.isFirstPackage || !installedPackages.add(host)) return

        // Do not filter by processName == packageName: Bluetooth services can live in subprocesses.
        installHook("$host self-check") { ModuleStatusHook(this, host, processName).install() }
        when (host) {
            HyperPodsBroadcasts.SYSTEM_UI ->
                installHook("SystemUI plugin discovery") { SystemUIPluginHook.install(this, param.classLoader) }
            HyperPodsBroadcasts.BLUETOOTH ->
                installHook("Bluetooth headset dispatcher") { HeadsetStateDispatcher.install(this, param.classLoader) }
            HyperPodsBroadcasts.XIAOMI_BLUETOOTH ->
                installHook("Xiaomi Bluetooth notifications") { MiBluetoothToastHook.install(this, param.classLoader) }
        }
    }
}

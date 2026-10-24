package moe.chenxy.hyperpods.utils

import android.annotation.SuppressLint
import android.content.Context

internal object ScopeRestartSelection {
    private const val PREFERENCES = "scope_restart_selection"

    fun available(profile: SystemProfile = SystemEnvironment.current): List<ScopeRestart.Host> =
        ScopeRestart.reloadOrder.filter { it.processName in profile.restartProcesses }

    fun selected(hosts: List<ScopeRestart.Host>, saved: Map<String, *>): Set<String> =
        hosts.filter { host ->
            val value = saved[host.processName]
            require(value == null || value is Boolean) { "Invalid saved scope selection" }
            value != false
        }.map { it.processName }.toSet()

    fun read(context: Context, hosts: List<ScopeRestart.Host>): Set<String> =
        selected(hosts, context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).all)

    @SuppressLint("UseKtx") // Keep commit's result so the UI can report persistence failures.
    fun save(context: Context, hosts: List<ScopeRestart.Host>, selected: Set<String>): Boolean {
        require(selected.isNotEmpty() && hosts.map { it.processName }.containsAll(selected))
        val editor = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
        hosts.forEach { editor.putBoolean(it.processName, it.processName in selected) }
        return editor.commit()
    }
}

package moe.chenxy.hyperpods.utils

import android.content.Context

internal object SystemUiContext {
    fun resolve(context: Context): Context =
        context.applicationContext?.takeIf { it.packageName == HyperPodsBroadcasts.SYSTEM_UI }
            ?: context.createPackageContext(HyperPodsBroadcasts.SYSTEM_UI, 0)
}

package moe.chenxy.hyperpods.utils

import android.content.Context
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.*

class SystemUiContextTest {
    @Test fun nativeApplicationContextIsReused() {
        val plugin = mock(Context::class.java)
        val app = mock(Context::class.java)
        `when`(plugin.applicationContext).thenReturn(app)
        `when`(app.packageName).thenReturn(HyperPodsBroadcasts.SYSTEM_UI)
        assertSame(app, SystemUiContext.resolve(plugin))
        verify(plugin, never()).createPackageContext(anyString(), anyInt())
    }

    @Test fun nullPluginApplicationContextResolvesRealHostContext() {
        val plugin = mock(Context::class.java)
        val host = mock(Context::class.java)
        `when`(plugin.applicationContext).thenReturn(null)
        `when`(plugin.createPackageContext(HyperPodsBroadcasts.SYSTEM_UI, 0)).thenReturn(host)
        assertSame(host, SystemUiContext.resolve(plugin))
        verify(plugin).createPackageContext(HyperPodsBroadcasts.SYSTEM_UI, 0)
    }

    @Test fun foreignApplicationIdentityIsNotUsedForTheHostReceiver() {
        val plugin = mock(Context::class.java)
        val app = mock(Context::class.java)
        val host = mock(Context::class.java)
        `when`(plugin.applicationContext).thenReturn(app)
        `when`(app.packageName).thenReturn("miui.systemui.plugin")
        `when`(plugin.createPackageContext(HyperPodsBroadcasts.SYSTEM_UI, 0)).thenReturn(host)
        assertSame(host, SystemUiContext.resolve(plugin))
    }

    @Test(expected = android.content.pm.PackageManager.NameNotFoundException::class)
    fun missingHostIsExplicitFailureRatherThanAPluginFallback() {
        val plugin = mock(Context::class.java)
        `when`(plugin.createPackageContext(HyperPodsBroadcasts.SYSTEM_UI, 0))
            .thenThrow(android.content.pm.PackageManager.NameNotFoundException())
        SystemUiContext.resolve(plugin)
    }
}

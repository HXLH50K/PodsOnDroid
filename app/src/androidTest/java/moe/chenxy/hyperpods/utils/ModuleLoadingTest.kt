package moe.chenxy.hyperpods.utils

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ModuleLoadingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun applicationIdentityAndLauncherAreIndependentOfTheUpstreamApp() {
        assertEquals("com.hxlh50k.pods.debug", context.packageName)
        assertEquals("P.O.D.S.", context.getString(R.string.app_name))
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(context.packageName)
        assertNotNull(context.packageManager.resolveActivity(launcher, 0))
    }

    @Test fun englishAndSimplifiedChineseResourcesAreAvailable() {
        fun localized(locale: Locale) = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocales(LocaleList(locale)) })
        assertEquals("Module loading self-check", localized(Locale.ENGLISH).getString(R.string.module_self_check))
        assertEquals("模块加载自检", localized(Locale.SIMPLIFIED_CHINESE).getString(R.string.module_self_check))
    }

    @Test fun allRequiredHostsReportTheActualApi102Build() = runBlocking {
        val reports = ModuleStatusQuery.collect(context, 5000)
        for (host in ModuleStatus.packages) {
            val report = reports[host]
            assertNotNull("No authenticated module report from $host", report)
            val actual = requireNotNull(report)
            assertTrue("$host has an unsupported framework", actual.frameworkApi >= 102)
            assertEquals(BuildConfig.BUILD_INSTANCE, actual.buildInstance)
            assertTrue("$host is running a different module build", actual.matchesInstalled)
            instrumentation.sendStatus(0, Bundle().apply {
                putString("stream", "HOST ${actual.processName}: API ${actual.frameworkApi}, " +
                    "PID ${actual.pid}, build ${actual.buildInstance}, ${actual.detail}\n")
            })
        }
    }
}

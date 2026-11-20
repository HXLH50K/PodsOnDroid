package moe.chenxy.hyperpods.utils

import android.app.BroadcastOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BadParcelableException
import android.os.Bundle
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.data.EarDetectionParams
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class HyperPodsBroadcastsTest {
    private val context = mock(Context::class.java)
    private val packages = mock(PackageManager::class.java).also {
        `when`(context.packageManager).thenReturn(it)
    }
    private val receiver = mock(BroadcastReceiver::class.java)

    @Test fun unknownSenderIsRejectedWithoutLookingUpPackages() {
        `when`(receiver.sentFromUid).thenReturn(-1)
        assertFalse(HyperPodsBroadcasts.isTrusted(context, receiver, BuildConfig.APPLICATION_ID))
        verifyNoInteractions(packages)
    }

    @Test fun matchingActionDoesNotAuthorizeAnotherAppsUid() {
        `when`(receiver.sentFromUid).thenReturn(12345)
        `when`(packages.getPackageUid(BuildConfig.APPLICATION_ID, 0)).thenReturn(23456)
        assertFalse(HyperPodsBroadcasts.isTrusted(context, receiver, BuildConfig.APPLICATION_ID))
    }

    @Test fun onlyAnAllowedPackageUidCanSendCommands() {
        `when`(receiver.sentFromUid).thenReturn(23456)
        `when`(packages.getPackageUid(BuildConfig.APPLICATION_ID, 0)).thenReturn(23456)
        assertTrue(HyperPodsBroadcasts.isTrusted(context, receiver, BuildConfig.APPLICATION_ID))
        `when`(packages.getPackageUid(HyperPodsBroadcasts.SYSTEM_UI, 0)).thenReturn(1000)
        assertFalse(HyperPodsBroadcasts.isTrusted(context, receiver, HyperPodsBroadcasts.SYSTEM_UI))
    }

    @Test fun missingAllowedPackageIsRejected() {
        `when`(receiver.sentFromUid).thenReturn(12345)
        `when`(packages.getPackageUid(BuildConfig.APPLICATION_ID, 0))
            .thenThrow(PackageManager.NameNotFoundException())
        assertFalse(HyperPodsBroadcasts.isTrusted(context, receiver, BuildConfig.APPLICATION_ID))
    }

    @Test fun oldHostParcelableIsIgnoredAndNewReportStillDecodes() {
        val intent = mock(Intent::class.java)
        val report = EarDetectionParams(0, 1)
        `when`(intent.getParcelableExtra("status", EarDetectionParams::class.java))
            .thenThrow(BadParcelableException("Old host class name")).thenReturn(report)

        assertNull(HyperPodsBroadcasts.readParcelable(intent, "status", EarDetectionParams::class.java))
        assertSame(report, HyperPodsBroadcasts.readParcelable(intent, "status", EarDetectionParams::class.java))
    }

    @Test fun sendsArePackageScopedAndExposeTheirActualUidToTheReceiver() {
        mockStatic(BroadcastOptions::class.java).use { factory ->
            val options = mock(BroadcastOptions::class.java, RETURNS_SELF)
            val bundle = mock(Bundle::class.java)
            `when`(options.toBundle()).thenReturn(bundle)
            factory.`when`<BroadcastOptions> { BroadcastOptions.makeBasic() }.thenReturn(options)
            val intent = mock(Intent::class.java)
            HyperPodsBroadcasts.send(context, intent, HyperPodsBroadcasts.BLUETOOTH)
            verify(intent).setPackage(HyperPodsBroadcasts.BLUETOOTH)
            verify(options).setShareIdentityEnabled(true)
            verify(context).sendBroadcast(intent, null, bundle)
        }
    }
}

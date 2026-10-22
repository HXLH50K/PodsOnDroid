package moe.chenxy.hyperpods.hook

import android.app.Application
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.AudioConnectionNotifications
import moe.chenxy.hyperpods.utils.BatteryNotificationHost
import moe.chenxy.hyperpods.utils.HostReflection

object MiBluetoothToastHook {
    @Volatile private var receiverRegistered = false
    val notificationReceiverReady: Boolean get() = receiverRegistered
    private var batteryHost: BatteryNotificationHost? = null

    fun install(framework: XposedInterface, classLoader: ClassLoader) {
        fun register(context: Context) {
            if (receiverRegistered || !AudioConnectionNotifications.ownsPresentation(Application.getProcessName())) return
            try {
                val host = BatteryNotificationHost(context.applicationContext)
                host.register()
                batteryHost = host
                receiverRegistered = true
                Log.i("PODS-Battery", "Dual-ear battery notification owner registered")
            } catch (error: Exception) {
                Log.e("PODS-Battery", "Unable to register battery notification owner", error)
            }
        }

        installHook("Xiaomi Bluetooth application receiver") {
            val application = Class.forName("com.android.bluetooth.ble.MiuiApplication", false, classLoader)
            framework.intercept(HostReflection.method(application, "onCreate")) { chain ->
                val result = chain.proceed()
                register(chain.thisObject as Context)
                result
            }
        }
        // Keep the inherited old-model controller from competing with the single AACP owner.
        // Discovery-mode native pairing remains a separate, deferred feature.
        val scan = try {
            Class.forName("b1.d", false, classLoader).also {
                check(it.getDeclaredMethod("U", ScanResult::class.java).returnType == Void.TYPE)
            }
        } catch (error: ReflectiveOperationException) {
            Log.w("PODS-Battery", "Official AirPods scan signature unavailable", error)
            null
        }
        if (scan != null) installHook("official AirPods scan interception") {
            framework.intercept(HostReflection.method(scan, "U", ScanResult::class.java)) {
                if (BuildConfig.DEBUG) Log.v("PODS-Battery", "Old-model AirPods scan controller suppressed")
                null
            }
        }
        installHook("Xiaomi notification context") {
            val notification = Class.forName("com.android.bluetooth.ble.app.MiuiBluetoothNotification", false, classLoader)
            notification.declaredConstructors.filter { it.parameterCount == 2 }.forEach { constructor ->
                framework.intercept(constructor) { chain ->
                    val result = chain.proceed()
                    register(HostReflection.field(requireNotNull(chain.thisObject), "mContext") as Context)
                    result
                }
            }
        }
    }
}

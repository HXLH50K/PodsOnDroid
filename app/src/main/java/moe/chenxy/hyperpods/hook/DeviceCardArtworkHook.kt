package moe.chenxy.hyperpods.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import io.github.libxposed.api.XposedInterface
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.AirPodsModels
import moe.chenxy.hyperpods.utils.HostReflection
import moe.chenxy.hyperpods.utils.HyperPodsBroadcasts
import moe.chenxy.hyperpods.utils.data.HyperPodsAction
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.WeakHashMap

internal object DeviceCardArtworkHook {
    private data class Artwork(val tint: ColorStateList?, val filter: ColorFilter?)

    fun install(framework: XposedInterface, loader: ClassLoader) {
        val holder = Class.forName(
            "miui.systemui.controlcenter.panel.main.devicecenter.devices.DeviceItemViewHolder", false, loader)
        val bind = HostReflection.methods(holder, "bindDeviceInfo", 2).single()
        val callback = HostReflection.methods(holder, "bindDeviceInfo\$lambda\$7\$lambda\$4\$lambda\$3", 2).single()
        check(ImageView::class.java.isAssignableFrom(callback.parameterTypes[0]) &&
            callback.parameterTypes[1] == Drawable::class.java)
        val handler = Handler(Looper.getMainLooper())
        val bindings = WeakHashMap<Any, String>()
        val owned = WeakHashMap<ImageView, Artwork>()
        val requests = mutableMapOf<String, (Intent) -> Unit>()
        var registered = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!HyperPodsBroadcasts.isTrusted(context, this, HyperPodsBroadcasts.BLUETOOTH)) return
                requests.remove(intent.getStringExtra("request_id"))?.invoke(intent)
            }
        }
        framework.intercept(callback) { chain ->
            if (owned.containsKey(chain.args[0])) null else chain.proceed()
        }
        framework.intercept(bind) { chain ->
            if (Looper.myLooper() != Looper.getMainLooper()) return@intercept chain.proceed()
            val instance = requireNotNull(chain.thisObject)
            val binding = HostReflection.call(instance, "getBinding")!!
            val images = listOf("fullIcon", "textIcon").map { HostReflection.field(binding, it) as ImageView }
            images.forEach { image ->
                owned.remove(image)?.let {
                    image.imageTintList = it.tint
                    image.colorFilter = it.filter
                    image.setImageDrawable(null)
                }
            }
            val token = UUID.randomUUID().toString()
            bindings[instance] = token
            val result = chain.proceed()
            try {
                val info = HostReflection.call(chain.args[0], "getDeviceInfo")!!
                if (HostReflection.call(info, "getDeviceType") != "third_headset" || requests.size >= 8)
                    return@intercept result
                val id = HostReflection.call(info, "getId") as String
                val context = images.first().context.applicationContext
                if (!registered) {
                    context.registerReceiver(receiver, IntentFilter(HyperPodsAction.ACTION_PODS_MAC_RECEIVED),
                        null, handler, Context.RECEIVER_EXPORTED)
                    registered = true
                }
                val weakHolder = WeakReference(instance)
                val weakImages = images.map(::WeakReference)
                val timeout = Runnable { requests.remove(token) }
                requests[token] = { reply ->
                    handler.removeCallbacks(timeout)
                    if (bindings[weakHolder.get()] == token && id.equals(reply.getStringExtra("mac"), true)) {
                        val model = reply.getStringExtra("model_number")?.let(AirPodsModels::getModelByModelNumber)
                        if (model != null) {
                            try {
                                val resources = context.packageManager.getResourcesForApplication(BuildConfig.APPLICATION_ID)
                                weakImages.mapNotNull { it.get() }.forEach { image ->
                                    val drawable = resources.getDrawable(model.budsRes, null)
                                    owned[image] = Artwork(image.imageTintList, image.colorFilter)
                                    image.imageTintList = null
                                    image.clearColorFilter()
                                    image.setImageDrawable(drawable)
                                }
                            } catch (error: Exception) {
                                Log.e("Art_Chen", "Unable to load device-card artwork", error)
                            }
                        }
                    }
                }
                check(handler.postDelayed(timeout, 500))
                HyperPodsBroadcasts.send(context, Intent(HyperPodsAction.ACTION_GET_PODS_MAC)
                    .putExtra("request_id", token), HyperPodsBroadcasts.BLUETOOTH)
            } catch (error: Exception) {
                requests.remove(token)
                Log.e("Art_Chen", "Unable to request device-card artwork", error)
            }
            result
        }
    }
}

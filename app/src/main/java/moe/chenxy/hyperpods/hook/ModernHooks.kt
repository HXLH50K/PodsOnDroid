package moe.chenxy.hyperpods.hook

import android.util.Log
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable

/** Each callback is synchronous; callers must copy values before scheduling asynchronous work. */
internal fun XposedInterface.intercept(
    executable: Executable,
    callback: (XposedInterface.Chain) -> Any?
): XposedInterface.HookHandle =
    hook(executable).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(callback)

internal fun installHook(feature: String, install: () -> Unit) {
    runCatching(install).onFailure { Log.e("Art_Chen", "Unable to install $feature", it) }
}

package moe.chenxy.hyperpods.utils

import com.android.systemui.plugins.miui.dynamicisland.DynamicIslandContent
import miui.systemui.dynamicisland.window.DynamicIslandWindowViewController
import miui.systemui.dynamicisland.window.DynamicIslandWindowViewCreator
import miui.systemui.dynamicisland.window.`DynamicIslandWindowViewCreator$dynamicIslandContentProxy$2$1` as ContentProxy
import org.junit.Assert.*
import org.junit.Test

class NativeBatteryIslandEndpointTest {
    @Test fun directControllerRemainsSupported() {
        val controller = DynamicIslandWindowViewController()
        val endpoint = NativeBatteryIslandEndpoint(controller)
        assertSame(controller, endpoint.current())
        assertSame(controller, endpoint.resolve())
    }

    @Test fun pluginLoadAcceptsUnboundProxyWithoutCreatingAWindow() {
        val creator = DynamicIslandWindowViewCreator()
        val endpoint = NativeBatteryIslandEndpoint(ContentProxy(creator))
        assertNull(endpoint.current())
        assertEquals(0, creator.creations)
    }

    @Test fun firstPresentationCreatesOnceAndReusesTheBoundController() {
        val creator = DynamicIslandWindowViewCreator()
        val endpoint = NativeBatteryIslandEndpoint(ContentProxy(creator))
        assertSame(creator.next, endpoint.resolve())
        assertSame(creator.next, endpoint.resolve())
        assertEquals(1, creator.creations)
    }

    @Test fun alreadyBoundAndReplacedWindowsAreResolvedWithoutStaleCaching() {
        val creator = DynamicIslandWindowViewCreator()
        val first = DynamicIslandWindowViewController()
        creator.current = first
        val endpoint = NativeBatteryIslandEndpoint(ContentProxy(creator))
        assertSame(first, endpoint.resolve())
        val replacement = DynamicIslandWindowViewController()
        creator.current = replacement
        assertSame(replacement, endpoint.resolve())
        assertEquals(0, creator.creations)
    }

    @Test fun disabledNativeWindowFailsWithoutSyntheticFallback() {
        val creator = DynamicIslandWindowViewCreator().apply { next = null }
        val endpoint = NativeBatteryIslandEndpoint(ContentProxy(creator))
        assertThrows(IllegalArgumentException::class.java) { endpoint.resolve() }
        assertNull(endpoint.current())
        assertEquals(1, creator.creations)
    }

    @Test fun creationMustBindTheExactReturnedController() {
        val creator = DynamicIslandWindowViewCreator().apply { bind = false }
        val endpoint = NativeBatteryIslandEndpoint(ContentProxy(creator))
        assertThrows(IllegalStateException::class.java) { endpoint.resolve() }
    }

    @Test fun unknownWrappersAndUnexpectedDelegatesFailClosed() {
        val unknown = object : DynamicIslandContent {}
        assertThrows(IllegalStateException::class.java) { NativeBatteryIslandEndpoint(unknown) }
        val creator = DynamicIslandWindowViewCreator().apply { current = unknown }
        val endpoint = NativeBatteryIslandEndpoint(ContentProxy(creator))
        assertThrows(IllegalStateException::class.java) { endpoint.resolve() }
        assertEquals(0, creator.creations)
    }
}

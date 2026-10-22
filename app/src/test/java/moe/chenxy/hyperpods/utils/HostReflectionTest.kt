package moe.chenxy.hyperpods.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class HostReflectionTest {
    open class Parent {
        private val hidden = 42
        private fun inherited(value: Boolean): Boolean = !value
        open fun overrideMe(): String = "parent"
    }

    class Host : Parent() {
        override fun overrideMe(): String = "host"
        private fun choose(value: Any): String = "object"
        private fun choose(value: CharSequence): String = "text"
        private fun choose(value: String): String = value
        private fun number(value: Int): Int = value + 1
        private fun ambiguous(value: String?): String = "text"
        private fun ambiguous(value: Number?): String = "number"
        private fun fails(): Nothing = throw IllegalStateException("host error")

        companion object {
            @JvmField val STATIC = "static"
            @JvmStatic private fun twice(value: Int): Int = value * 2
        }
    }

    @Test fun readsInheritedPrivateField() {
        assertEquals(42, HostReflection.field(Host(), "hidden"))
    }

    @Test fun invokesInheritedPrivateMethodAndUnboxesPrimitive() {
        assertFalse(HostReflection.call(Host(), "inherited", true) as Boolean)
        assertEquals(4, HostReflection.call(Host(), "number", 3))
    }

    @Test fun prefersMostSpecificCompatibleOverload() {
        assertEquals("hello", HostReflection.call(Host(), "choose", "hello"))
        assertEquals("text", HostReflection.call(Host(), "choose", StringBuilder()))
        assertEquals("object", HostReflection.call(Host(), "choose", Any()))
    }

    @Test fun nullSelectsMostSpecificReferenceType() {
        // Passing null to the String method is permitted by reflection, despite Kotlin's annotation.
        assertEquals(null, HostReflection.call(Host(), "choose", null))
    }

    @Test fun ambiguousNullFailsClosed() {
        assertThrows(NoSuchMethodException::class.java) { HostReflection.call(Host(), "ambiguous", null) }
    }

    @Test fun nullNeverMatchesPrimitive() {
        assertThrows(NoSuchMethodException::class.java) { HostReflection.call(Host(), "number", null) }
    }

    @Test fun invokesStaticMethodsAndReadsStaticFields() {
        assertEquals("static", HostReflection.staticField(Host::class.java, "STATIC"))
        assertEquals(6, HostReflection.callStatic(Host::class.java, "twice", 3))
    }

    @Test fun unwrapsHostException() {
        val error = assertThrows(IllegalStateException::class.java) { HostReflection.call(Host(), "fails") }
        assertEquals("host error", error.message)
    }

    @Test fun findsExactPrivateSignature() {
        val method = HostReflection.method(Host::class.java, "inherited", Boolean::class.javaPrimitiveType!!)
        assertEquals(Parent::class.java, method.declaringClass)
        assertEquals(false, method.invoke(Host(), true))
    }

    @Test fun doesNotInstallDuplicateOverriddenMethods() {
        val methods = HostReflection.methods(Host::class.java, "overrideMe", 0)
        assertEquals(1, methods.size)
        assertEquals(Host::class.java, methods.single().declaringClass)
        assertEquals("host", HostReflection.call(Host(), "overrideMe"))
    }
}

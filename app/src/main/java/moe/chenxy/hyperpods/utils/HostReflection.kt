package moe.chenxy.hyperpods.utils

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Reflection on host implementation classes, never on the Xposed framework. */
object HostReflection {
    fun method(type: Class<*>, name: String, vararg parameters: Class<*>): Method =
        hierarchy(type).mapNotNull { owner ->
            runCatching { owner.getDeclaredMethod(name, *parameters) }.getOrNull()
        }.firstOrNull()?.apply { isAccessible = true }
            ?: throw NoSuchMethodException("${type.name}.$name")

    fun methods(type: Class<*>, name: String, parameterCount: Int? = null): List<Method> =
        hierarchy(type).flatMap { it.declaredMethods.asSequence() }
            .filter { it.name == name && (parameterCount == null || it.parameterCount == parameterCount) }
            .distinctBy { it.parameterTypes.toList() }
            .map { it.apply { isAccessible = true } }.toList()
            .also { if (it.isEmpty()) throw NoSuchMethodException("${type.name}.$name") }

    fun field(instance: Any, name: String): Any? = findField(instance.javaClass, name).get(instance)

    fun staticField(type: Class<*>, name: String): Any? = findField(type, name).get(null)

    fun call(instance: Any?, name: String, vararg arguments: Any?): Any? {
        requireNotNull(instance) { "Missing receiver for $name" }
        return invoke(resolve(instance.javaClass, name, arguments, false), instance, arguments)
    }

    fun callStatic(type: Class<*>, name: String, vararg arguments: Any?): Any? =
        invoke(resolve(type, name, arguments, true), null, arguments)

    private fun hierarchy(type: Class<*>): Sequence<Class<*>> = generateSequence(type) { it.superclass }

    private fun findField(type: Class<*>, name: String): Field =
        hierarchy(type).mapNotNull { owner ->
            runCatching { owner.getDeclaredField(name) }.getOrNull()
        }.firstOrNull()?.apply { isAccessible = true }
            ?: throw NoSuchFieldException("${type.name}.$name")

    private fun resolve(type: Class<*>, name: String, arguments: Array<out Any?>, static: Boolean): Method {
        val candidates = methods(type, name, arguments.size).filter { method ->
            Modifier.isStatic(method.modifiers) == static && method.parameterTypes.indices.all { index ->
                val argument = arguments[index]
                val parameter = method.parameterTypes[index]
                if (argument == null) !parameter.isPrimitive else boxed(parameter).isInstance(argument)
            }
        }
        val mostSpecific = candidates.filter { candidate ->
            candidates.all { other ->
                candidate == other || candidate.parameterTypes.indices.all { index ->
                    boxed(other.parameterTypes[index]).isAssignableFrom(boxed(candidate.parameterTypes[index]))
                }
            }
        }
        return mostSpecific.singleOrNull()
            ?: throw NoSuchMethodException("Missing or ambiguous ${type.name}.$name")
    }

    private fun boxed(type: Class<*>): Class<*> = when (type) {
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        java.lang.Integer.TYPE -> java.lang.Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        else -> type
    }

    private fun invoke(method: Method, instance: Any?, arguments: Array<out Any?>): Any? =
        try {
            method.invoke(instance, *arguments)
        } catch (error: InvocationTargetException) {
            throw error.targetException
        }
}

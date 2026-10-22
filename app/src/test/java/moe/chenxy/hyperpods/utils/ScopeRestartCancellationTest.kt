package moe.chenxy.hyperpods.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class ScopeRestartCancellationTest {
    @Test fun cancellationInterruptsPendingSignalAndClosesProcessWithoutReturningAResult() = runBlocking {
        assertCancellation(signalAlreadySent = false)
    }

    @Test fun cancellationAfterPossibleSignalPropagatesInsteadOfReturningAnAcknowledgement() = runBlocking {
        assertCancellation(signalAlreadySent = true)
    }

    private suspend fun assertCancellation(signalAlreadySent: Boolean) = kotlinx.coroutines.coroutineScope {
        val process = PendingProcess(signalAlreadySent)
        val root = RootCommandRunner { script ->
            assertTrue(script.contains("kill -TERM"))
            process
        }
        val core = ScopeRestart { command ->
            if (command.phase == ScopeRestart.Phase.SIGNAL) {
                root.run(command)
            } else {
                val encoded = command.host.processName.map {
                    it.code.toString(16).padStart(2, '0')
                }.joinToString("") + "00"
                ScopeRestart.CommandResult.Exited(
                    0, "PODS_SCOPE_1|SNAPSHOT|${command.pid}|${command.uid}|55|$encoded\n"
                )
            }
        }
        val host = ScopeRestart.Host.SYSTEM_UI
        val plan = (core.discover(
            ScopeRestart.Preset.UI_HOSTS,
            listOf(ScopeRestart.VerifiedHost(
                host.packageName, host.processName, 400, 1000, 28,
                "12345678", "build-123", 102
            ))
        ) as ScopeRestart.DiscoveryResult.Ready).plan
        val returned = AtomicReference<ScopeRestart.SignalResult?>()
        val operation = async(Dispatchers.Default) {
            core.signal(plan).also { returned.set(it) }
        }
        try {
            assertTrue("The pending command never started", process.waitEntered.await(2, TimeUnit.SECONDS))
            operation.cancel()
            withTimeout(2_000) { operation.join() }
            try {
                operation.await()
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
                assertTrue(operation.isCancelled)
            }
            assertTrue("Blocking wait must be interrupted promptly", process.waitInterrupted.get())
            assertTrue("The su process must be destroyed", process.destroyed.get())
            assertTrue(process.stdinClosed.get())
            assertTrue(process.stdoutClosed.get())
            assertTrue(process.stderrClosed.get())
            assertNull("Cancellation must not return any signal result", returned.get())
            process.allowExit.countDown()
            assertEquals(signalAlreadySent, process.signalSent.get())
            assertEquals(ScopeRestart.FailureCode.PLAN_ALREADY_USED,
                (core.signal(plan) as ScopeRestart.SignalResult.Stopped).failure.code)
        } finally {
            process.destroyForcibly()
            operation.cancelAndJoin()
        }
    }

    private class PendingProcess(signalAlreadySent: Boolean) : Process() {
        val waitEntered = CountDownLatch(1)
        val allowExit = CountDownLatch(1)
        val waitInterrupted = AtomicBoolean()
        val destroyed = AtomicBoolean()
        val signalSent = AtomicBoolean(signalAlreadySent)
        val stdinClosed = AtomicBoolean()
        val stdoutClosed = AtomicBoolean()
        val stderrClosed = AtomicBoolean()
        private val stdin = object : ByteArrayOutputStream() {
            override fun close() {
                stdinClosed.set(true)
                super.close()
            }
        }
        private val stdout = object : ByteArrayInputStream(byteArrayOf()) {
            override fun close() {
                stdoutClosed.set(true)
                super.close()
            }
        }
        private val stderr = object : ByteArrayInputStream(byteArrayOf()) {
            override fun close() {
                stderrClosed.set(true)
                super.close()
            }
        }

        override fun getOutputStream() = stdin
        override fun getInputStream() = stdout
        override fun getErrorStream() = stderr

        override fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
            waitEntered.countDown()
            try {
                if (!allowExit.await(timeout, unit)) return false
            } catch (interrupted: InterruptedException) {
                waitInterrupted.set(true)
                throw interrupted
            }
            if (!destroyed.get()) signalSent.set(true)
            return true
        }

        override fun waitFor(): Int {
            waitFor(8, TimeUnit.SECONDS)
            return exitValue()
        }

        override fun exitValue(): Int {
            if (!destroyed.get() && allowExit.count > 0) throw IllegalThreadStateException()
            return 0
        }

        override fun destroy() {
            destroyed.set(true)
            allowExit.countDown()
        }

        override fun destroyForcibly(): Process {
            destroy()
            return this
        }

        override fun isAlive() = !destroyed.get()
    }
}

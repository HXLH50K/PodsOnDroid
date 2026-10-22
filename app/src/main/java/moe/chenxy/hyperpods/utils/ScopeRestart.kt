package moe.chenxy.hyperpods.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

/**
 * Explicit, user-confirmed recovery only. The caller authenticates module reports, obtains each
 * package UID from PackageManager, and verifies a fresh module report after signalling.
 */
class ScopeRestart(private val runner: CommandRunner = RootCommandRunner()) {
    enum class Preset { UI_HOSTS, ALL_VERIFIED }

    enum class Host(val packageName: String, val processName: String, val affectsControlIpc: Boolean) {
        SYSTEM_UI("com.android.systemui", "com.android.systemui", false),
        XIAOMI_UI("com.xiaomi.bluetooth", "com.xiaomi.bluetooth:ui", false),
        XIAOMI_BLUETOOTH("com.xiaomi.bluetooth", "com.xiaomi.bluetooth", true),
        BLUETOOTH("com.android.bluetooth", "com.android.bluetooth", true);

        companion object {
            fun exact(packageName: String, processName: String): Host? = entries.firstOrNull {
                it.packageName == packageName && it.processName == processName
            }
        }
    }

    /** Never construct from an unverified broadcast, package guesses, or persisted/stale reports. */
    data class VerifiedHost(
        val packageName: String,
        val processName: String,
        val pid: Int,
        val expectedUid: Int,
        val version: Int,
        val revision: String,
        val buildInstance: String,
        val frameworkApi: Int
    )

    data class Target internal constructor(
        val host: Host,
        val pid: Int,
        val uid: Int,
        val startTimeTicks: Long,
        val evidence: VerifiedHost
    )

    /** An opaque, immutable, single-use capability belonging to the instance that discovered it. */
    sealed interface Plan {
        val targets: List<Target>
        val requiresControlIpcStop: Boolean
    }

    enum class FailureCode {
        NO_VERIFIED_TARGETS, UNAUTHORIZED_HOST, INVALID_EVIDENCE, INVALID_PID, INVALID_UID,
        DUPLICATE_TARGET, INVALID_PLAN, PLAN_ALREADY_USED, CONTROL_IPC_NOT_STOPPED,
        ROOT_DENIED, ROOT_UNAVAILABLE, TIMEOUT, OUTPUT_LIMIT, IO_ERROR, INTERRUPTED,
        UNKNOWN_RESULT, MALFORMED_SNAPSHOT, PROCESS_VANISHED, PROCESS_NAME_MISMATCH,
        UID_MISMATCH, PID_REUSED, SIGNAL_FAILED
    }

    data class Failure(val code: FailureCode, val host: Host? = null)

    sealed interface DiscoveryResult {
        data class Ready(val plan: Plan) : DiscoveryResult
        data class Rejected(val failure: Failure) : DiscoveryResult
    }

    sealed interface SignalResult {
        /** Only kill(2) acknowledgement; neither process replacement nor module load is proven. */
        data class Completed(val signalled: List<Target>) : SignalResult
        /** A timeout/IO failure can have an unknown signal outcome. Never retry automatically. */
        data class Stopped(val signalled: List<Target>, val failure: Failure) : SignalResult
    }

    enum class Phase { SNAPSHOT, SIGNAL }

    /** Callers can inspect commands in tests, but cannot construct arbitrary root commands. */
    sealed interface Command {
        val phase: Phase
        val host: Host
        val pid: Int
        val uid: Int
        val startTimeTicks: Long?
    }

    sealed interface CommandResult {
        data class Exited(val exitCode: Int, val output: String) : CommandResult
        data class Failed(val reason: CommandFailure) : CommandResult
    }

    enum class CommandFailure { ROOT_DENIED, ROOT_UNAVAILABLE, TIMEOUT, OUTPUT_LIMIT, IO_ERROR, INTERRUPTED }

    fun interface CommandRunner {
        suspend fun run(command: Command): CommandResult
    }

    private val owner = Any()

    suspend fun discover(preset: Preset, verifiedHosts: List<VerifiedHost>): DiscoveryResult {
        if (verifiedHosts.isEmpty()) return rejected(FailureCode.NO_VERIFIED_TARGETS)
        if (verifiedHosts.size > Host.entries.size) return rejected(FailureCode.INVALID_EVIDENCE)
        val candidates = ArrayList<Pair<Host, VerifiedHost>>()
        val pids = HashSet<Int>()
        val hosts = HashSet<Host>()
        for (evidence in verifiedHosts.toList()) {
            val host = Host.exact(evidence.packageName, evidence.processName)
                ?: return rejected(FailureCode.UNAUTHORIZED_HOST)
            if (evidence.pid <= 1) return rejected(FailureCode.INVALID_PID, host)
            if (evidence.expectedUid <= 0) return rejected(FailureCode.INVALID_UID, host)
            if (evidence.version !in 1..1_000_000 || evidence.frameworkApi !in 102..1000 ||
                !evidence.revision.matches(Regex("(local|[0-9a-f]{8}|[0-9a-f]{40})")) ||
                !evidence.buildInstance.matches(Regex("[A-Za-z0-9._:-]{1,128}"))) {
                return rejected(FailureCode.INVALID_EVIDENCE, host)
            }
            if (!pids.add(evidence.pid) || !hosts.add(host)) {
                return rejected(FailureCode.DUPLICATE_TARGET, host)
            }
            if (preset == Preset.ALL_VERIFIED || !host.affectsControlIpc) {
                candidates.add(host to evidence)
            }
        }
        if (candidates.isEmpty()) return rejected(FailureCode.NO_VERIFIED_TARGETS)
        val targets = ArrayList<Target>()
        for ((host, evidence) in candidates.sortedBy { it.first.ordinal }) {
            val command = SnapshotCommand(host, evidence.pid, evidence.expectedUid)
            when (val response = ScopeRestartProtocol.read(runner.run(command), command)) {
                is ScopeRestartResponse.Rejected -> return DiscoveryResult.Rejected(response.failure)
                is ScopeRestartResponse.Snapshot -> targets.add(
                    Target(host, evidence.pid, evidence.expectedUid, response.startTimeTicks, evidence)
                )
                ScopeRestartResponse.Signalled -> return rejected(FailureCode.UNKNOWN_RESULT, host)
            }
        }
        return DiscoveryResult.Ready(ApprovedPlan(owner, immutable(targets)))
    }

    /**
     * Stop the app's control IPC first only when plan.requiresControlIpcStop is true.
     * UI_HOSTS does not disconnect AACP. All targets are revalidated immediately before TERM.
     */
    suspend fun signal(plan: Plan, controlIpcStopped: Boolean = false): SignalResult {
        val approved = plan as? ApprovedPlan
        if (approved == null || approved.owner !== owner) {
            return stopped(emptyList(), Failure(FailureCode.INVALID_PLAN))
        }
        if (approved.requiresControlIpcStop && !controlIpcStopped) {
            return stopped(emptyList(), Failure(FailureCode.CONTROL_IPC_NOT_STOPPED))
        }
        if (!approved.used.compareAndSet(false, true)) {
            return stopped(emptyList(), Failure(FailureCode.PLAN_ALREADY_USED))
        }
        val signalled = ArrayList<Target>()
        for (target in approved.targets) {
            val command = SignalCommand(target)
            when (val response = ScopeRestartProtocol.read(runner.run(command), command)) {
                ScopeRestartResponse.Signalled -> signalled.add(target)
                is ScopeRestartResponse.Rejected -> return stopped(signalled, response.failure)
                is ScopeRestartResponse.Snapshot -> return stopped(
                    signalled, Failure(FailureCode.UNKNOWN_RESULT, target.host)
                )
            }
        }
        return SignalResult.Completed(immutable(signalled))
    }

    private fun rejected(code: FailureCode, host: Host? = null) =
        DiscoveryResult.Rejected(Failure(code, host))

    private fun stopped(targets: List<Target>, failure: Failure) =
        SignalResult.Stopped(immutable(targets), failure)
}

private fun <T> immutable(values: List<T>): List<T> = Collections.unmodifiableList(ArrayList(values))

private class ApprovedPlan(
    val owner: Any,
    override val targets: List<ScopeRestart.Target>
) : ScopeRestart.Plan {
    val used = AtomicBoolean()
    override val requiresControlIpcStop = targets.any { it.host.affectsControlIpc }
}

private class SnapshotCommand(
    override val host: ScopeRestart.Host,
    override val pid: Int,
    override val uid: Int
) : ScopeRestart.Command {
    override val phase = ScopeRestart.Phase.SNAPSHOT
    override val startTimeTicks: Long? = null
}

private class SignalCommand(target: ScopeRestart.Target) : ScopeRestart.Command {
    override val phase = ScopeRestart.Phase.SIGNAL
    override val host = target.host
    override val pid = target.pid
    override val uid = target.uid
    override val startTimeTicks = target.startTimeTicks
}

private sealed interface ScopeRestartResponse {
    data class Snapshot(val startTimeTicks: Long) : ScopeRestartResponse
    data object Signalled : ScopeRestartResponse
    data class Rejected(val failure: ScopeRestart.Failure) : ScopeRestartResponse
}

private object ScopeRestartProtocol {
    const val PREFIX = "PODS_SCOPE_1"
    const val MAX_OUTPUT_BYTES = 16_384
    private val positive = Regex("[1-9][0-9]*")
    private val hex = Regex("[0-9a-f]+")
    private val scriptFailures = setOf(
        ScopeRestart.FailureCode.ROOT_DENIED, ScopeRestart.FailureCode.MALFORMED_SNAPSHOT,
        ScopeRestart.FailureCode.PROCESS_VANISHED, ScopeRestart.FailureCode.PROCESS_NAME_MISMATCH,
        ScopeRestart.FailureCode.UID_MISMATCH, ScopeRestart.FailureCode.PID_REUSED,
        ScopeRestart.FailureCode.SIGNAL_FAILED
    )

    fun read(result: ScopeRestart.CommandResult, command: ScopeRestart.Command): ScopeRestartResponse {
        fun failure(code: ScopeRestart.FailureCode) =
            ScopeRestartResponse.Rejected(ScopeRestart.Failure(code, command.host))
        if (result is ScopeRestart.CommandResult.Failed) {
            return failure(ScopeRestart.FailureCode.valueOf(result.reason.name))
        }
        result as ScopeRestart.CommandResult.Exited
        if (result.output.length > MAX_OUTPUT_BYTES) return failure(ScopeRestart.FailureCode.OUTPUT_LIMIT)
        val line = result.output.removeSuffix("\n")
        val fields = line.split('|')
        if (fields.size == 3 && fields[0] == PREFIX && fields[1] == "ERROR" &&
            result.exitCode != 0) {
            val code = scriptFailures.firstOrNull { it.name == fields[2] }
                ?: return failure(ScopeRestart.FailureCode.UNKNOWN_RESULT)
            return failure(code)
        }
        if (result.exitCode != 0) {
            if (result.exitCode == 124 || result.exitCode == 137) {
                return failure(ScopeRestart.FailureCode.TIMEOUT)
            }
            val lower = line.lowercase(Locale.ROOT)
            if (!line.startsWith(PREFIX) &&
                listOf("permission denied", "access denied", "not allowed").any { it in lower }) {
                return failure(ScopeRestart.FailureCode.ROOT_DENIED)
            }
            return failure(if (!line.startsWith(PREFIX)) ScopeRestart.FailureCode.ROOT_UNAVAILABLE
                else ScopeRestart.FailureCode.UNKNOWN_RESULT)
        }
        if (fields.size != 6 || fields[0] != PREFIX) {
            return failure(ScopeRestart.FailureCode.MALFORMED_SNAPSHOT)
        }
        val expectedKind = if (command.phase == ScopeRestart.Phase.SNAPSHOT) "SNAPSHOT" else "SIGNALLED"
        if (fields[1] != expectedKind || fields.slice(2..4).any { !positive.matches(it) }) {
            return failure(ScopeRestart.FailureCode.MALFORMED_SNAPSHOT)
        }
        val pid = fields[2].toIntOrNull() ?: return failure(ScopeRestart.FailureCode.MALFORMED_SNAPSHOT)
        val uid = fields[3].toIntOrNull() ?: return failure(ScopeRestart.FailureCode.MALFORMED_SNAPSHOT)
        val start = fields[4].toLongOrNull() ?: return failure(ScopeRestart.FailureCode.MALFORMED_SNAPSHOT)
        if (pid <= 1 || pid != command.pid) return failure(ScopeRestart.FailureCode.INVALID_PID)
        if (uid != command.uid) return failure(ScopeRestart.FailureCode.UID_MISMATCH)
        if (command.startTimeTicks != null && command.startTimeTicks != start) {
            return failure(ScopeRestart.FailureCode.PID_REUSED)
        }
        val encoded = fields[5]
        if (encoded.length !in 2..8192 || encoded.length % 2 != 0 || !hex.matches(encoded)) {
            return failure(ScopeRestart.FailureCode.MALFORMED_SNAPSHOT)
        }
        val bytes = encoded.chunked(2).map { it.toInt(16) }
        val nul = bytes.indexOf(0)
        if (nul <= 0 || bytes.drop(nul).any { it != 0 }) {
            return failure(ScopeRestart.FailureCode.PROCESS_NAME_MISMATCH)
        }
        if (bytes.take(nul) != command.host.processName.map { it.code }) {
            return failure(ScopeRestart.FailureCode.PROCESS_NAME_MISMATCH)
        }
        return if (command.phase == ScopeRestart.Phase.SNAPSHOT) ScopeRestartResponse.Snapshot(start)
        else ScopeRestartResponse.Signalled
    }
}

/**
 * The only production runner. It accepts opaque policy-issued commands, never scripts or paths.
 * A short-lived root shell rechecks identity before signalling; Linux shell kill is not pidfd-based,
 * so the kernel cannot atomically bind the final identity check and signal.
 * Cancellation tears down the process and propagates; a signal already delivered remains unknown.
 */
class RootCommandRunner internal constructor(
    private val startProcess: (String) -> Process
) : ScopeRestart.CommandRunner {
    constructor() : this({ script ->
        ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
    })

    override suspend fun run(command: ScopeRestart.Command): ScopeRestart.CommandResult =
        runInterruptible(Dispatchers.IO) {
            runBlockingCommand(command)
        }

    private fun runBlockingCommand(command: ScopeRestart.Command): ScopeRestart.CommandResult {
        if (Thread.currentThread().isInterrupted) throw InterruptedException()
        val script = ScopeRestartScript.generate(command)
        val process = try {
            startProcess(script)
        } catch (_: IOException) {
            return ScopeRestart.CommandResult.Failed(ScopeRestart.CommandFailure.ROOT_UNAVAILABLE)
        } catch (_: SecurityException) {
            return ScopeRestart.CommandResult.Failed(ScopeRestart.CommandFailure.ROOT_DENIED)
        }
        val output = ByteArrayOutputStream()
        val readFailure = AtomicReference<ScopeRestart.CommandFailure?>()
        val reader = Thread({
            try {
                process.inputStream.use { input ->
                    val buffer = ByteArray(1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > ScopeRestartProtocol.MAX_OUTPUT_BYTES) {
                            readFailure.compareAndSet(null, ScopeRestart.CommandFailure.OUTPUT_LIMIT)
                            process.destroyForcibly()
                            break
                        }
                        output.write(buffer, 0, count)
                    }
                }
            } catch (_: IOException) {
                readFailure.compareAndSet(null, ScopeRestart.CommandFailure.IO_ERROR)
            }
        }, "scope-restart-output").apply { isDaemon = true }
        return try {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            process.outputStream.close()
            reader.start()
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                ScopeRestart.CommandResult.Failed(ScopeRestart.CommandFailure.TIMEOUT)
            } else {
                reader.join(READER_JOIN_MILLIS)
                val failure = readFailure.get()
                when {
                    failure != null -> ScopeRestart.CommandResult.Failed(failure)
                    reader.isAlive -> ScopeRestart.CommandResult.Failed(ScopeRestart.CommandFailure.TIMEOUT)
                    else -> ScopeRestart.CommandResult.Exited(
                        process.exitValue(), output.toString(Charsets.UTF_8.name())
                    )
                }
            }
        } catch (_: IOException) {
            ScopeRestart.CommandResult.Failed(ScopeRestart.CommandFailure.IO_ERROR)
        } finally {
            process.destroyForcibly()
            closeQuietly(process.inputStream)
            closeQuietly(process.errorStream)
            closeQuietly(process.outputStream)
        }
    }

    private fun closeQuietly(stream: java.io.Closeable) {
        try {
            stream.close()
        } catch (_: IOException) {
            // Cleanup cannot turn an already failed or acknowledged command into a different result.
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 8L
        const val READER_JOIN_MILLIS = 250L
    }
}

internal object ScopeRestartScript {
    fun generate(command: ScopeRestart.Command): String {
        check(command is SnapshotCommand || command is SignalCommand)
        check(command.pid > 1 && command.uid > 0)
        check(command.startTimeTicks == null || command.startTimeTicks!! > 0)
        val expectedHex = command.host.processName.map { it.code.toString(16).padStart(2, '0') }.joinToString("")
        val expectedStart = command.startTimeTicks?.toString().orEmpty()
        val action = if (command.phase == ScopeRestart.Phase.SIGNAL) """
            [ "${'$'}start" = "$expectedStart" ] || fail PID_REUSED
            kill -TERM "${'$'}pid" 2>/dev/null || {
                [ -d "${'$'}proc" ] || fail PROCESS_VANISHED
                fail SIGNAL_FAILED
            }
            printf 'PODS_SCOPE_1|SIGNALLED|%s|%s|%s|%s\n' "${'$'}pid" "${'$'}uid" "${'$'}start" "${'$'}cmdline"
        """ else """
            printf 'PODS_SCOPE_1|SNAPSHOT|%s|%s|%s|%s\n' "${'$'}pid" "${'$'}uid" "${'$'}start" "${'$'}cmdline"
        """
        val body = """
            set -f
            export LC_ALL=C
            tool=/system/bin/toybox
            fail() { printf 'PODS_SCOPE_1|ERROR|%s\n' "${'$'}1"; exit 1; }
            [ "${'$'}("${'$'}tool" id -u 2>/dev/null)" = 0 ] || fail ROOT_DENIED
            pid=${command.pid}
            uid=${command.uid}
            proc=/proc/${command.pid}
            number() { case "${'$'}1" in ''|*[!0-9]*) return 1;; esac; }
            read_start() {
                stat_line=${'$'}("${'$'}tool" cat "${'$'}proc/stat" 2>/dev/null) || fail PROCESS_VANISHED
                [ "${'$'}{stat_line%% *}" = "${'$'}pid" ] || fail MALFORMED_SNAPSHOT
                stat_rest=${'$'}{stat_line##*) }
                [ "${'$'}stat_rest" != "${'$'}stat_line" ] || fail MALFORMED_SNAPSHOT
                set -- ${'$'}stat_rest
                [ "${'$'}#" -ge 20 ] || fail MALFORMED_SNAPSHOT
                case "${'$'}1" in Z|X|x) fail PROCESS_VANISHED;; esac
                shift 19
                number "${'$'}1" || fail MALFORMED_SNAPSHOT
                [ "${'$'}1" != 0 ] || fail MALFORMED_SNAPSHOT
                start=${'$'}1
            }
            read_identity() {
                [ -d "${'$'}proc" ] || fail PROCESS_VANISHED
                uid_seen=0
                tgid_seen=0
                while read -r key a b c d extra; do
                    case "${'$'}key" in
                        Uid:)
                            [ "${'$'}a" = "${'$'}uid" ] && [ "${'$'}b" = "${'$'}uid" ] &&
                            [ "${'$'}c" = "${'$'}uid" ] && [ "${'$'}d" = "${'$'}uid" ] &&
                            [ -z "${'$'}extra" ] || fail UID_MISMATCH
                            uid_seen=1;;
                        Tgid:)
                            [ "${'$'}a" = "${'$'}pid" ] && [ -z "${'$'}b" ] || fail MALFORMED_SNAPSHOT
                            tgid_seen=1;;
                    esac
                done < "${'$'}proc/status" 2>/dev/null || fail PROCESS_VANISHED
                [ "${'$'}uid_seen" = 1 ] && [ "${'$'}tgid_seen" = 1 ] || fail MALFORMED_SNAPSHOT
                cmdline=${'$'}("${'$'}tool" head -c 4097 "${'$'}proc/cmdline" 2>/dev/null |
                    "${'$'}tool" od -An -v -tx1 | "${'$'}tool" tr -d ' \n')
                [ "${'$'}{#cmdline}" -le 8192 ] || fail MALFORMED_SNAPSHOT
                case "${'$'}cmdline" in
                    ${expectedHex}00*) padding=${'$'}{cmdline#${expectedHex}};;
                    *) [ -d "${'$'}proc" ] || fail PROCESS_VANISHED; fail PROCESS_NAME_MISMATCH;;
                esac
                while [ -n "${'$'}padding" ]; do
                    case "${'$'}padding" in 00*) padding=${'$'}{padding#00};; *) fail PROCESS_NAME_MISMATCH;; esac
                done
            }
            read_start
            initial_start=${'$'}start
            read_identity
            read_start
            [ "${'$'}start" = "${'$'}initial_start" ] || fail PID_REUSED
            $action
        """.trimIndent()
        // The root-side deadline also terminates the shell if the app-side su client is destroyed.
        return "exec /system/bin/toybox timeout -s KILL 6 /system/bin/sh -c '" +
            body.replace("'", "'\\''") + "'"
    }
}

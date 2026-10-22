package moe.chenxy.hyperpods.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.system.Os
import android.util.AtomicFile
import android.util.Log
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.data.HyperPodsAction
import moe.chenxy.hyperpods.utils.data.HyperPodsPrefsKey as Key
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

/** App-owned policy only. Earbud settings still come from AACP, not this store. */
object AppPreferences {
    const val AUTHORITY = BuildConfig.APPLICATION_ID + ".preferences"
    const val READ = "read"
    const val EXTRA = "app_settings"
    private const val FILE = "app_settings.json"
    private const val PRIVATE_MARKER = "hyperpods_private_migration"
    private var legacySecuredDir: File? = null
    private var remoteReading = false
    private var pendingRemoteRead: (() -> Unit)? = null
    private val changeListeners = CopyOnWriteArraySet<(Snapshot) -> Unit>()

    data class Snapshot(
        val earDetection: Boolean = true,
        val switchSpeaker: Boolean = true,
        val conversationVolume: Boolean = true,
        val lowBattery: LowBatterySettings = LowBatterySettings(),
        val headGestures: Boolean = false,
        val batteryNotifications: BatteryNotificationSettings = BatteryNotificationSettings(),
        val batteryNotificationsMigrationRequired: Boolean = false,
    ) {
        fun encode(): String {
            require(batteryNotifications.valid) { "Battery thresholds must satisfy 1 <= low < medium <= 99" }
            return JSONObject(mapOf(
                Key.EAR_DETECTION to earDetection,
                Key.EAR_DETECTION_SWITCH_SPEAKER to switchSpeaker,
                Key.CONVERSATION_PHONE_VOLUME to conversationVolume,
                Key.HEAD_GESTURES to headGestures,
                Key.LOW_BATTERY_EARS to lowBattery.earsEnabled,
                Key.LOW_BATTERY_EARS_THRESHOLD to lowBattery.earsThreshold,
                Key.LOW_BATTERY_CASE to lowBattery.caseEnabled,
                Key.LOW_BATTERY_CASE_THRESHOLD to lowBattery.caseThreshold,
                Key.BATTERY_NOTIFICATION_LOW_THRESHOLD to batteryNotifications.lowThreshold,
                Key.BATTERY_NOTIFICATION_MEDIUM_THRESHOLD to batteryNotifications.mediumThreshold,
                Key.BATTERY_NOTIFICATION_LOW_REMINDER to batteryNotifications.lowReminderEnabled,
                Key.BATTERY_NOTIFICATION_MEDIUM_REMINDER to batteryNotifications.mediumReminderEnabled,
                Key.BATTERY_NOTIFICATIONS_MIGRATION_REQUIRED to batteryNotificationsMigrationRequired,
            )).toString()
        }
    }

    fun decode(value: String?): Snapshot? {
        value ?: return null
        return try {
            val json = JSONObject(value)
            val legacyThreshold = json.opt(Key.LOW_BATTERY_EARS_THRESHOLD)
            val battery = LowBatterySettings(
                json.opt(Key.LOW_BATTERY_EARS) as? Boolean ?: true,
                legacyThreshold as? Int ?: 20,
                json.opt(Key.LOW_BATTERY_CASE) as? Boolean ?: true,
                json.opt(Key.LOW_BATTERY_CASE_THRESHOLD) as? Int ?: 20,
            )
            val newKeys = listOf(Key.BATTERY_NOTIFICATION_LOW_THRESHOLD,
                Key.BATTERY_NOTIFICATION_MEDIUM_THRESHOLD, Key.BATTERY_NOTIFICATION_LOW_REMINDER,
                Key.BATTERY_NOTIFICATION_MEDIUM_REMINDER)
            val hasNewSettings = newKeys.any { json.has(it) }
            val migrated = migrateBattery(battery)
            val notifications = if (hasNewSettings) BatteryNotificationSettings(
                json.get(Key.BATTERY_NOTIFICATION_LOW_THRESHOLD) as Int,
                json.get(Key.BATTERY_NOTIFICATION_MEDIUM_THRESHOLD) as Int,
                json.get(Key.BATTERY_NOTIFICATION_LOW_REMINDER) as Boolean,
                json.get(Key.BATTERY_NOTIFICATION_MEDIUM_REMINDER) as Boolean,
            ) else migrated
            // Invalid new settings are rejected at the IPC boundary, never normalized.
            if (!notifications.valid) {
                Log.w("HyperPods-Preferences", "Rejected battery notification settings: invalid low/medium threshold pair")
                return null
            }
            Snapshot(
                json.get(Key.EAR_DETECTION) as Boolean,
                json.get(Key.EAR_DETECTION_SWITCH_SPEAKER) as Boolean,
                json.get(Key.CONVERSATION_PHONE_VOLUME) as Boolean,
                battery,
                // Missing only in pre-gesture private files. Do not reset any old policy.
                if (json.has(Key.HEAD_GESTURES)) json.get(Key.HEAD_GESTURES) as Boolean else false,
                notifications,
                if (hasNewSettings) {
                    if (json.has(Key.BATTERY_NOTIFICATIONS_MIGRATION_REQUIRED))
                        json.get(Key.BATTERY_NOTIFICATIONS_MIGRATION_REQUIRED) as Boolean else false
                } else !BatteryNotificationSettings(lowThreshold = battery.earsThreshold).valid ||
                    (json.has(Key.LOW_BATTERY_EARS_THRESHOLD) && legacyThreshold !is Int) ||
                    (json.has(Key.LOW_BATTERY_EARS) && json.opt(Key.LOW_BATTERY_EARS) !is Boolean),
            )
        } catch (_: Exception) {
            Log.w("HyperPods-Preferences", "Rejected app settings: malformed JSON, missing field, or invalid field type")
            null
        }
    }

    private fun file(context: Context) = AtomicFile(File(context.filesDir, FILE))

    private fun storedFileExists(store: AtomicFile) =
        store.baseFile.exists() || File(store.baseFile.path + ".bak").exists()

    /** filesDir is not redirected by NSP; future framework upgrades keep this data. */
    @Synchronized
    fun read(context: Context): Snapshot {
        val store = file(context)
        var readFailed = false
        val source = try { store.openRead().bufferedReader().use { it.readText() } }
            catch (_: FileNotFoundException) {
                readFailed = storedFileExists(store)
                null
            }
            catch (_: IOException) { readFailed = true; null }
        if (readFailed) Log.w("HyperPods-Preferences", "Unable to read stored app settings; original storage retained")
        val saved = decode(source)
        if (saved != null && legacySecuredDir == context.filesDir) return saved
        // Import any legacy XML still accessible here; already migrated JSON remains authoritative
        // after NSP redirection is disabled. Never call Yuki's world-readable prefs bridge.
        val legacy = context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE)
        val snapshot = saved ?: run {
            val fallback = fromLegacy(legacy)
            when {
                source != null -> {
                    Log.w("HyperPods-Preferences", "Stored app settings require recovery; fallback is not saved and original storage is retained")
                    recoverStored(source, fallback)
                }
                readFailed -> fallback.copy(
                    batteryNotifications = fallback.batteryNotifications.copy(lowThreshold = 20, mediumThreshold = 50),
                    batteryNotificationsMigrationRequired = true)
                else -> fallback
            }
        }
        val persisted = saved != null || (source == null && !readFailed && write(store, snapshot))
        if (persisted && legacySecuredDir != context.filesDir && secureLegacy(legacy)) legacySecuredDir = context.filesDir
        return snapshot
    }

    /** Local recovery only: IPC decoding must continue to reject invalid snapshots. */
    private fun recoverStored(source: String, fallback: Snapshot): Snapshot {
        val json = try { JSONObject(source) } catch (_: Exception) {
            return fallback.copy(
                batteryNotifications = fallback.batteryNotifications.copy(lowThreshold = 20, mediumThreshold = 50),
                batteryNotificationsMigrationRequired = true)
        }
        fun boolean(key: String, default: Boolean) = json.opt(key) as? Boolean ?: default
        val battery = LowBatterySettings(
            boolean(Key.LOW_BATTERY_EARS, fallback.lowBattery.earsEnabled),
            json.opt(Key.LOW_BATTERY_EARS_THRESHOLD) as? Int ?: fallback.lowBattery.earsThreshold,
            boolean(Key.LOW_BATTERY_CASE, fallback.lowBattery.caseEnabled),
            json.opt(Key.LOW_BATTERY_CASE_THRESHOLD) as? Int ?: fallback.lowBattery.caseThreshold,
        )
        val notifications = BatteryNotificationSettings(
            lowReminderEnabled = boolean(Key.BATTERY_NOTIFICATION_LOW_REMINDER, battery.earsEnabled),
            mediumReminderEnabled = boolean(Key.BATTERY_NOTIFICATION_MEDIUM_REMINDER, true),
        )
        return Snapshot(
            boolean(Key.EAR_DETECTION, fallback.earDetection),
            boolean(Key.EAR_DETECTION_SWITCH_SPEAKER, fallback.switchSpeaker),
            boolean(Key.CONVERSATION_PHONE_VOLUME, fallback.conversationVolume),
            battery,
            boolean(Key.HEAD_GESTURES, fallback.headGestures),
            notifications,
            batteryNotificationsMigrationRequired = true,
        )
    }

    internal fun fromLegacy(legacy: SharedPreferences): Snapshot {
        val values = legacy.all
        fun boolean(key: String) = values[key] as? Boolean ?: true
        fun threshold(key: String) = values[key] as? Int ?: 20
        val battery = LowBatterySettings(boolean(Key.LOW_BATTERY_EARS),
            threshold(Key.LOW_BATTERY_EARS_THRESHOLD), boolean(Key.LOW_BATTERY_CASE),
            threshold(Key.LOW_BATTERY_CASE_THRESHOLD))
        return Snapshot(boolean(Key.EAR_DETECTION), boolean(Key.EAR_DETECTION_SWITCH_SPEAKER),
            boolean(Key.CONVERSATION_PHONE_VOLUME), battery,
            batteryNotifications = migrateBattery(battery),
            batteryNotificationsMigrationRequired =
                !BatteryNotificationSettings(lowThreshold = battery.earsThreshold).valid ||
                    (values.containsKey(Key.LOW_BATTERY_EARS_THRESHOLD) &&
                        values[Key.LOW_BATTERY_EARS_THRESHOLD] !is Int) ||
                    (values.containsKey(Key.LOW_BATTERY_EARS) && values[Key.LOW_BATTERY_EARS] !is Boolean))
    }

    private fun migrateBattery(legacy: LowBatterySettings): BatteryNotificationSettings {
        val migrated = BatteryNotificationSettings(lowThreshold = legacy.earsThreshold,
            lowReminderEnabled = legacy.earsEnabled)
        return if (migrated.valid) migrated else migrated.copy(lowThreshold = 20, mediumThreshold = 50)
    }

    internal fun secureLegacy(legacy: SharedPreferences): Boolean {
        if (legacy.all.isEmpty()) return true // Fresh installs must not create a legacy XML.
        // Force a private-mode rewrite, including removal of the old .bak file.
        // Retain all values for recovery; do not delete or clear the old settings.
        val secured = legacy.edit().putBoolean(PRIVATE_MARKER, !legacy.getBoolean(PRIVATE_MARKER, false)).commit()
        if (!secured) Log.w("HyperPods-Preferences", "Unable to privatize legacy settings; will retry")
        return secured
    }

    @Synchronized
    fun save(context: Context, snapshot: Snapshot): Boolean {
        if (!write(file(context), snapshot)) return false
        changeListeners.forEach { listener ->
            try { listener(snapshot) }
            catch (_: Exception) { Log.w("HyperPods-Preferences", "Unable to refresh a settings observer") }
        }
        return true
    }

    fun addChangeListener(listener: (Snapshot) -> Unit) { changeListeners.add(listener) }

    fun removeChangeListener(listener: (Snapshot) -> Unit) { changeListeners.remove(listener) }

    internal fun write(store: AtomicFile, snapshot: Snapshot): Boolean {
        if (!snapshot.batteryNotifications.valid) {
            Log.w("HyperPods-Preferences", "Rejected invalid battery threshold pair; settings were not saved")
            return false
        }
        // Reads leave the original JSON untouched. Before the first later save, retain
        // an owner-only recovery copy; the legacy XML and keys are also never cleared.
        val original = try { store.openRead().bufferedReader().use { it.readText() } }
            catch (_: FileNotFoundException) {
                if (storedFileExists(store)) {
                    Log.w("HyperPods-Preferences", "Save aborted: existing app settings could not be read for recovery")
                    return false
                }
                null
            }
            catch (_: IOException) {
                Log.w("HyperPods-Preferences", "Save aborted: unable to read existing app settings for recovery")
                return false
            }
        val previous = decode(original)
        if (original != null && (previous == null || previous.batteryNotificationsMigrationRequired)) {
            val recovery = AtomicFile(File(store.baseFile.parentFile,
                store.baseFile.nameWithoutExtension +
                    if (previous == null) ".invalid-recovery.json" else ".battery-recovery.json"))
            if (!storedFileExists(recovery)) {
                if (!writePrivate(recovery, original)) return false
            } else if (previous == null) {
                val retained = try { recovery.openRead().bufferedReader().use { it.readText() } }
                    catch (_: IOException) {
                        Log.w("HyperPods-Preferences", "Save aborted: unable to verify invalid-settings recovery copy")
                        return false
                    }
                if (retained != original) {
                    Log.w("HyperPods-Preferences", "Save aborted: recovery copy differs; invalid original settings remain untouched")
                    return false
                }
            }
        }
        return writePrivate(store, snapshot.encode())
    }

    private fun writePrivate(store: AtomicFile, value: String): Boolean {
        val output = try { store.startWrite() } catch (_: IOException) {
            Log.w("HyperPods-Preferences", "Unable to start private app-settings write; settings were not saved")
            return false
        }
        try {
            Os.fchmod(output.fd, 0x180) // 0600: owner read/write only.
            output.write(value.toByteArray(Charsets.UTF_8))
            store.finishWrite(output)
        } catch (_: Exception) {
            Log.w("HyperPods-Preferences", "Private app-settings write failed; settings were not saved")
            try { store.failWrite(output) }
            catch (_: Exception) { Log.w("HyperPods-Preferences", "Unable to roll back private app-settings write") }
            return false
        }
        return try {
            val persisted = store.openRead().bufferedReader().use { it.readText() } == value
            if (!persisted) Log.w("HyperPods-Preferences", "Private app-settings write verification failed; save was not confirmed")
            persisted
        } catch (_: IOException) {
            Log.w("HyperPods-Preferences", "Unable to verify private app-settings write; save was not confirmed")
            false
        }
    }

    /** Only the provider can read the app's file; the Bluetooth process receives a fixed snapshot. */
    fun readRemote(context: Context): Snapshot? = try {
        decode(context.contentResolver.call(AUTHORITY, READ, null, null)?.getString(EXTRA))
    } catch (_: Exception) {
        Log.w("HyperPods-Preferences", "Settings provider unavailable; retaining current policy")
        null
    }

    /** Binder IPC may ignore interruption. Keep one active read and only the latest
     * queued request. Each response retains its original session/revision callback;
     * a newer request must read fresh policy, not inherit an older response. */
    fun requestRemote(context: Context, callback: (Snapshot?) -> Unit) {
        synchronized(this) {
            pendingRemoteRead = { callback(readRemote(context)) }
            if (remoteReading) return
            remoteReading = true
        }
        Thread({
            while (true) {
                val request = synchronized(this) {
                    pendingRemoteRead?.also { pendingRemoteRead = null } ?: run {
                        remoteReading = false
                        return@Thread
                    }
                }
                try { request() } catch (_: Exception) {
                    Log.w("HyperPods-Preferences", "Unable to apply remote policy")
                }
            }
        }, "HyperPods-settings-reader").apply { isDaemon = true }.start()
    }

    fun publish(context: Context, snapshot: Snapshot) {
        HyperPodsBroadcasts.send(context, Intent(HyperPodsAction.ACTION_APP_SETTINGS_CHANGED)
            .putExtra(EXTRA, snapshot.encode()), HyperPodsBroadcasts.BLUETOOTH)
    }
}

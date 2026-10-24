package moe.chenxy.hyperpods.utils.data

object HyperPodsAction {
    const val ACTION_PODS_UI_INIT = "com.hxlh50k.pods.action.ui_init"
    const val ACTION_PODS_CONNECTED = "com.hxlh50k.pods.action.pods_connected"
    const val ACTION_PODS_DISCONNECTED = "com.hxlh50k.pods.action.pods_disconnected"
    const val ACTION_PODS_BATTERY_CHANGED = "com.hxlh50k.pods.action.pods_battery_changed"

    const val ACTION_ANC_SELECT = "com.hxlh50k.pods.action.anc_select"
    const val ACTION_PODS_RENAME = "com.hxlh50k.pods.action.rename"
    const val ACTION_PODS_ANC_CHANGED = "com.hxlh50k.pods.action.pods_anc_select"
    const val ACTION_EAR_DETECTION_STATUS_CHANGED = "com.hxlh50k.pods.action.ear_detection_status_changed"
    @Deprecated("Use ACTION_PODS_SETTINGS_CHANGED instead")
    const val ACTION_EAR_DETECTION_SWITCH_CHANGED = "com.hxlh50k.pods.action.ear_detection_switch_changed"
    const val ACTION_GET_PODS_MAC = "com.hxlh50k.pods.action.get_pods_mac"
    const val ACTION_PODS_MAC_RECEIVED = "com.hxlh50k.pods.action.got_pods_mac"
    const val ACTION_PODS_SETTINGS_CHANGED = "com.hxlh50k.pods.action.preference_changed"
    const val ACTION_APP_SETTINGS_CHANGED = "com.hxlh50k.pods.action.app_settings_changed"
    const val ACTION_PODS_SETTINGS_STATE = "com.hxlh50k.pods.action.settings_state"
    const val ACTION_PODS_SETTING_RESULT = "com.hxlh50k.pods.action.setting_result"
    const val ACTION_PODS_DIAGNOSTICS = "com.hxlh50k.pods.action.diagnostics"
    const val ACTION_PODS_DIAGNOSTICS_RECORD = "com.hxlh50k.pods.action.diagnostics_record"
    const val ACTION_PODS_DIAGNOSTICS_EVENT = "com.hxlh50k.pods.action.diagnostics_event"
    const val ACTION_MODULE_STATUS_REQUEST = "com.hxlh50k.pods.action.module_status_request"
    const val ACTION_MODULE_STATUS = "com.hxlh50k.pods.action.module_status"
    const val ACTION_PODS_DIAGNOSTICS_REQUEST = "com.hxlh50k.pods.action.diagnostics_request"
    const val ACTION_PODS_STATUS_RETRY = "com.hxlh50k.pods.action.status_retry"
    const val ACTION_HEAD_TRACKING_COMMAND = "com.hxlh50k.pods.action.head_tracking_command"
    const val ACTION_HEAD_TRACKING_SAMPLE = "com.hxlh50k.pods.action.head_tracking_sample"
    const val ACTION_REQUEST_CONTROL_OWNERSHIP = "com.hxlh50k.pods.action.request_control_ownership"
    const val ACTION_PODS_STATUS_CHANGED = "com.hxlh50k.pods.action.pods_status_changed"
}

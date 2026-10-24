package moe.chenxy.hyperpods.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.AppPreferences

@Composable
fun UnfinishedFeaturesSettings(enabled: Boolean) {
    val context = LocalContext.current
    val policy by rememberAppPreferences()
    DashboardToggle(
        title = stringResource(R.string.unfinished_features_title),
        summary = stringResource(R.string.unfinished_features_warning),
        checked = policy.allowUnfinishedFeatures,
        enabled = enabled,
        onChange = {
            val updated = AppPreferences.read(context).copy(allowUnfinishedFeatures = it)
            if (AppPreferences.save(context, updated)) {
                AppPreferences.publish(context, updated)
            } else {
                Toast.makeText(context, R.string.setting_write_failed, Toast.LENGTH_LONG).show()
            }
        },
    )
}

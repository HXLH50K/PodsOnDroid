package moe.chenxy.hyperpods.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.DashboardGroup
import moe.chenxy.hyperpods.ui.components.DashboardLink
import moe.chenxy.hyperpods.ui.components.PageScaffold
import moe.chenxy.hyperpods.ui.components.DashboardSection
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutPage(padding: PaddingValues, onBack: (() -> Unit)? = null,
              listState: LazyListState = rememberLazyListState(), isActive: Boolean = true) {
    val context = LocalContext.current
    val title = stringResource(R.string.about_hyperpods)
    fun open(url: String) { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    PageScaffold(title, padding, onBack, isActive) { bodyPadding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = bodyPadding.calculateBottomPadding() + 24.dp)) {
            item {
                DashboardGroup(Modifier.padding(horizontal = 16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(64.dp),
                            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.primary))
                        Text(stringResource(R.string.app_name), fontSize = 28.sp, fontWeight = FontWeight.Medium)
                        Text("${BuildConfig.VERSION_NAME} · ${BuildConfig.BUILD_REVISION}", fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Text(stringResource(R.string.about_project_description), fontSize = 15.sp, lineHeight = 23.sp)
                        Text(stringResource(R.string.about_project_maintainer), fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    DashboardLink(stringResource(R.string.project_repository)) {
                        open("https://github.com/HXLH50K/PodsOnDroid")
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    DashboardSection(stringResource(R.string.about_sources))
                    DashboardGroup {
                        Text(stringResource(R.string.about_sources_description), fontSize = 14.sp,
                            lineHeight = 22.sp, modifier = Modifier.padding(20.dp))
                        DashboardLink(stringResource(R.string.about_upstream_source)) {
                            open("https://github.com/Art-Chen/HyperPods")
                        }
                    }
                    DashboardSection(stringResource(R.string.about_licenses))
                    DashboardGroup {
                        Text(stringResource(R.string.about_license_description), fontSize = 14.sp,
                            lineHeight = 22.sp, modifier = Modifier.padding(20.dp))
                        DashboardLink(stringResource(R.string.about_view_license)) {
                            open("https://github.com/HXLH50K/PodsOnDroid/blob/main/LICENSE")
                        }
                    }
                }
            }
        }
    }
}

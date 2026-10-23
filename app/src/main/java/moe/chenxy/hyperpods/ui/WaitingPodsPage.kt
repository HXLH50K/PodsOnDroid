package moe.chenxy.hyperpods.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.ui.components.PageScaffold
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WaitingPodsPage(padding: PaddingValues = PaddingValues()) {
    PageScaffold(stringResource(R.string.dashboard_headphones), padding) { bodyPadding ->
        Column(Modifier.fillMaxSize().padding(bodyPadding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(painterResource(R.drawable.dashboard_headphones), null,
                tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.waiting_for_pod), fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

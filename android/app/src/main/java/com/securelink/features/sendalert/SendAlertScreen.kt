package com.securelink.features.sendalert

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securelink.R
import com.securelink.features.ProvisionalScreen
import com.securelink.ui.format.ConnectivityText

// provisional: spec 108 (formulario, mi alerta y cuenta regresiva) la reemplaza.
@Composable
fun SendAlertScreen(connectivity: ConnectivityText, modifier: Modifier = Modifier) {
    ProvisionalScreen(
        title = stringResource(R.string.sendalert_title),
        subtitle = stringResource(R.string.sendalert_subtitle),
        spec = "108",
        connectivity = connectivity,
        modifier = modifier,
    )
}

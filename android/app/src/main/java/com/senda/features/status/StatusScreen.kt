package com.senda.features.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.senda.R
import com.senda.features.ProvisionalScreen
import com.senda.ui.format.ConnectivityText

// provisional: spec 109 (estado de la red) la reemplaza.
@Composable
fun StatusScreen(connectivity: ConnectivityText, modifier: Modifier = Modifier) {
    ProvisionalScreen(
        title = stringResource(R.string.status_title),
        subtitle = stringResource(R.string.status_subtitle),
        spec = "109",
        connectivity = connectivity,
        modifier = modifier,
    )
}

package com.senda.features.nearby

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.senda.R
import com.senda.features.ProvisionalScreen
import com.senda.ui.format.ConnectivityText

// provisional: spec 109 (lista y detalle de alertas cercanas) la reemplaza.
@Composable
fun NearbyScreen(connectivity: ConnectivityText, modifier: Modifier = Modifier) {
    ProvisionalScreen(
        title = stringResource(R.string.nearby_title),
        subtitle = stringResource(R.string.nearby_subtitle),
        spec = "109",
        connectivity = connectivity,
        modifier = modifier,
    )
}

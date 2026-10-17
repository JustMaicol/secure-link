package com.securelink.features.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.securelink.R
import com.securelink.features.ProvisionalScreen
import com.securelink.ui.format.ConnectivityText

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

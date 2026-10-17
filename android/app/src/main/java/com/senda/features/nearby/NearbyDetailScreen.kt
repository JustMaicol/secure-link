package com.senda.features.nearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.ui.components.ButtonStyle
import com.senda.ui.components.EmergencyBadge
import com.senda.ui.components.InfoRow
import com.senda.ui.components.ScreenScaffold
import com.senda.ui.components.SendaButton
import com.senda.ui.components.SendaCard
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.format.ConnectivityText
import com.senda.ui.format.classifyElapsed
import com.senda.ui.format.elapsedText
import com.senda.ui.format.emergencyLevelText
import com.senda.ui.format.vulnerableText
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * C2 · Detalle de alerta cercana (Figma). Sin estado: dibuja [alert] y avisa de cada acción. Arriba, la brújula con la
 * distancia y el rumbo (sin internet); abajo, "Voy en camino" y el atajo a Google Maps, que solo sirve con datos.
 */
@Composable
fun NearbyDetailScreen(
    alert: NearbyAlertUi,
    connectivity: ConnectivityText,
    onBack: () -> Unit,
    onOnMyWay: () -> Unit,
    onOpenMaps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Senda.colors
    val resources = LocalResources.current
    ScreenScaffold(
        title = stringResource(R.string.nearby_detail_title),
        subtitle = stringResource(
            R.string.nearby_detail_subtitle,
            alert.reference,
            resources.elapsedText(classifyElapsed(alert.receivedAgeMs)),
        ),
        modifier = modifier,
        connectivity = connectivity,
        identityIcon = SendaIcons.ArrowLeft,
        identityLabel = stringResource(R.string.nearby_detail_back),
        onIdentityClick = onBack,
        contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = 16.dp),
    ) {
        CompassView(distanceM = alert.distanceM, bearingDeg = alert.bearingDeg)
        SendaCard(verticalSpacing = 10.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmergencyBadge(alert.type, resources.emergencyLevelText(alert.type))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    SendaIcon(SendaIcons.Users, size = 15.dp, tint = colors.accent)
                    Text(
                        pluralStringResource(R.plurals.nearby_volunteers_on_the_way, alert.volunteers, alert.volunteers),
                        style = SendaText.captionMedium,
                        color = colors.accent,
                    )
                }
            }
            Text(alert.coordinates, style = SendaText.cardTitle.copy(lineHeight = SendaText.subtitle.lineHeight), color = colors.textPrimary)
            InfoRow(
                label = stringResource(R.string.nearby_detail_people_label),
                value = stringResource(
                    R.string.nearby_detail_people_value,
                    alert.people,
                    resources.vulnerableText(alert.vulnerableCount),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(
                    R.string.nearby_detail_location_note,
                    resources.elapsedText(classifyElapsed(alert.locationAgeMs)),
                    alert.accuracyM,
                ),
                style = SendaText.caption,
                color = colors.textSecondary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SendaButton(
                text = stringResource(R.string.nearby_detail_on_my_way),
                onClick = onOnMyWay,
                icon = SendaIcons.Navigation,
            )
            SendaButton(
                text = stringResource(R.string.nearby_detail_open_maps),
                onClick = onOpenMaps,
                style = ButtonStyle.Secondary,
                icon = SendaIcons.Map,
            )
            Text(
                stringResource(R.string.nearby_detail_maps_note),
                style = SendaText.small,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

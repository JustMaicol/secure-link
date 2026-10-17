package com.senda.features.nearby

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.ui.components.EmergencyBadge
import com.senda.ui.components.EmptyState
import com.senda.ui.components.ScreenScaffold
import com.senda.ui.components.SendaCard
import com.senda.ui.components.SendaIcon
import com.senda.ui.components.SendaIcons
import com.senda.ui.components.arrowIcon
import com.senda.ui.format.ConnectivityText
import com.senda.ui.format.cardinalOf
import com.senda.ui.format.cardinalShortText
import com.senda.ui.format.classifyDistance
import com.senda.ui.format.classifyElapsed
import com.senda.ui.format.distanceText
import com.senda.ui.format.elapsedText
import com.senda.ui.format.emergencyLevelText
import com.senda.ui.format.peopleAndVulnerableText
import com.senda.ui.theme.Dimens
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * C1 · Alertas cercanas, lista (Figma). Sin estado: dibuja [state] y avisa de la alerta que se toca. El orden (más
 * cercanas primero) lo decide quien arma el estado; esta pantalla solo lo dice en texto.
 */
@Composable
fun NearbyListScreen(
    state: NearbyListUiState,
    connectivity: ConnectivityText,
    onOpenAlert: (NearbyAlertUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Senda.colors
    val resources = LocalResources.current
    ScreenScaffold(
        title = stringResource(R.string.nearby_title),
        subtitle = stringResource(R.string.nearby_subtitle),
        modifier = modifier,
        connectivity = connectivity,
        contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = 18.dp),
    ) {
        if (state.alerts.isEmpty()) {
            EmptyState(
                icon = SendaIcons.Radar,
                title = stringResource(R.string.nearby_empty_title),
                body = stringResource(R.string.nearby_empty_body),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    pluralStringResource(R.plurals.nearby_alert_count, state.alerts.size, state.alerts.size),
                    style = SendaText.sectionTitle,
                    color = colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    SendaIcon(SendaIcons.ListFilter, size = 16.dp, tint = colors.accent)
                    Text(stringResource(R.string.nearby_sort), style = SendaText.captionMedium, color = colors.accent)
                }
            }
            state.alerts.forEach { alert -> AlertCard(alert, onClick = { onOpenAlert(alert) }) }
            Text(stringResource(R.string.nearby_footnote), style = SendaText.caption, color = colors.textSecondary)
            state.updatedAgeMs?.let { age ->
                val ago = resources.elapsedText(classifyElapsed(age))
                Text(
                    text = stringResource(if (state.demoData) R.string.nearby_demo_data else R.string.nearby_updated, ago),
                    style = SendaText.smallPlain,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** Una alerta de la lista (Figma, "Alerta cercana"): nivel y antigüedad arriba; datos y distancia con rumbo abajo. */
@Composable
private fun AlertCard(alert: NearbyAlertUi, onClick: () -> Unit) {
    val colors = Senda.colors
    val resources = LocalResources.current
    val cardinal = cardinalOf(alert.bearingDeg)
    SendaCard(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmergencyBadge(alert.type, resources.emergencyLevelText(alert.type))
            Text(resources.elapsedText(classifyElapsed(alert.receivedAgeMs)), style = SendaText.captionPlain, color = colors.textSecondary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(alert.coordinates, style = SendaText.cardTitle, color = colors.textPrimary)
                Text(
                    resources.peopleAndVulnerableText(alert.people, alert.vulnerableCount),
                    style = SendaText.captionPlain,
                    color = colors.textSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    SendaIcon(SendaIcons.Users, size = 14.dp, tint = colors.accent)
                    Text(
                        pluralStringResource(R.plurals.nearby_volunteers_on_the_way, alert.volunteers, alert.volunteers),
                        style = SendaText.captionMedium,
                        color = colors.accent,
                    )
                }
            }
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (cardinal != null) SendaIcon(cardinal.arrowIcon(), size = 24.dp, tint = colors.accent)
                Text(resources.distanceText(classifyDistance(alert.distanceM)), style = SendaText.distance, color = colors.accent)
                Text(
                    stringResource(R.string.nearby_bearing, cardinal?.let { resources.cardinalShortText(it) } ?: stringResource(R.string.format_unknown)),
                    style = SendaText.smallPlain,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

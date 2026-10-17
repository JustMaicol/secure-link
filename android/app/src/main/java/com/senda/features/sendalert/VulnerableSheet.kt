package com.senda.features.sendalert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.senda.R
import com.senda.core.protocol.Vulnerable
import com.senda.ui.components.SendaButton
import com.senda.ui.components.SendaCheckbox
import com.senda.ui.theme.Senda
import com.senda.ui.theme.SendaText

/**
 * Hoja con las cuatro banderas de personas vulnerables (decisión del equipo, 2026-10-17). No está en Figma: el diseño
 * solo dibuja la fila que la abre. Se construye con la casilla, el texto y el botón del diseño.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VulnerableSheet(
    vulnerable: Vulnerable,
    onChange: (Vulnerable) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Senda.colors
    ModalBottomSheet(
        onDismissRequest = onDone,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.sendalert_vulnerable_sheet_title),
                style = SendaText.headline,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Flag(stringResource(R.string.sendalert_vulnerable_children), vulnerable.children) { onChange(vulnerable.copy(children = it)) }
            Flag(stringResource(R.string.sendalert_vulnerable_elderly), vulnerable.elderly) { onChange(vulnerable.copy(elderly = it)) }
            Flag(stringResource(R.string.sendalert_vulnerable_injured), vulnerable.injured) { onChange(vulnerable.copy(injured = it)) }
            Flag(stringResource(R.string.sendalert_vulnerable_reduced_mobility), vulnerable.reducedMobility) {
                onChange(vulnerable.copy(reducedMobility = it))
            }
            SendaButton(
                text = stringResource(R.string.sendalert_vulnerable_done),
                onClick = onDone,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
            )
        }
    }
}

/** Una bandera: toda la fila es el área de toque (48 dp como mínimo), con la casilla del diseño a la izquierda. */
@Composable
private fun Flag(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // La fila lleva el toque y el estado; la casilla solo se dibuja (si no, TalkBack la leería dos veces).
        SendaCheckbox(checked = checked, onCheckedChange = null, modifier = Modifier.size(22.dp).clearAndSetSemantics { })
        Text(label, style = SendaText.labelSemiBold, color = Senda.colors.textPrimary)
    }
}

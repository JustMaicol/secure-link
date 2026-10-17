package com.securelink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.securelink.ui.theme.Dimens
import com.securelink.ui.theme.Senda
import com.securelink.ui.theme.SendaText

@Immutable
data class BottomNavItem(val label: String, val icon: Int)

/**
 * Barra inferior de 3 pestañas (Figma, "Navegación principal"). La pestaña activa lleva una píldora de fondo y
 * texto en negrita con el color de acento. Deja libre el espacio de la barra de navegación del sistema.
 */
@Composable
fun SendaBottomBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Senda.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(thickness = Dimens.borderWidth, color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.navHeight)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val contentColor = if (selected) colors.accent else colors.textSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = Dimens.navPillWidth, height = Dimens.navPillHeight)
                            .background(
                                color = if (selected) colors.surfaceMuted else colors.surface,
                                shape = RoundedCornerShape(100.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        SendaIcon(item.icon, size = 22.dp, tint = contentColor)
                    }
                    Text(
                        text = item.label,
                        style = if (selected) SendaText.captionBold else SendaText.captionMedium,
                        color = contentColor,
                    )
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

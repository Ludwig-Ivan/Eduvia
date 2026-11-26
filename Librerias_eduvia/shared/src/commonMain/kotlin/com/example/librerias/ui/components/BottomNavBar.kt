package com.example.librerias.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.ui.theme.EduviaColors

/**
 * Barra inferior de navegación de la app: define las secciones Inicio, Tareas,
 * Calendario y Juntas, y permite cambiar entre ellas durante el flujo principal.
 */
enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Outlined.Home),
    TASKS("Tareas", Icons.Outlined.CheckBox),
    CALENDAR("Calendario", Icons.Outlined.CalendarToday),
    MEETINGS("Juntas", Icons.Outlined.People)
}

@Composable
fun BottomNavBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(EduviaColors.Surface)) {
        // línea superior (border-top del HTML)
        Box(Modifier.fillMaxWidth().height(0.8.dp).background(EduviaColors.Border))

        Row(Modifier.fillMaxWidth().height(72.dp)) {
            BottomTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val color = if (isSelected) EduviaColors.Primary else EduviaColors.TextSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tab.label,
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
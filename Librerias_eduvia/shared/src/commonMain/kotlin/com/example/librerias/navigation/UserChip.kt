package com.example.librerias.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.model.Student
import com.example.librerias.ui.theme.EduviaColors

/**
 * Cabecera del rol padre: muestra la inicial del usuario y despliega el selector de hijos
 * y la opción de cerrar sesión.
 *
 * No tiene ViewModel propio: `AppNavHost` conecta `onChildSelected` con el hijo activo (que
 * determina las claves de `MeetingsViewModel`, `TasksViewModel` y `CalendarViewModel`) y
 * `onLogoutClick` con `AppContainer.logout`.
 */
@Composable
fun UserChip(
    name: String = "María",
    children: List<Student> = emptyList(),
    selectedChildId: Long? = null,
    onChildSelected: (Student) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .height(36.dp)
                .background(EduviaColors.Surface, RoundedCornerShape(18.dp))
                .border(0.8.dp, EduviaColors.Border, RoundedCornerShape(18.dp))
                .clickable { expanded = true }
                .padding(start = 4.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(EduviaColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(name.first().uppercase(), color = EduviaColors.PrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(name.substringBefore(" "), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = EduviaColors.TextPrimary)
            Icon(Icons.Outlined.ExpandMore, null, tint = EduviaColors.TextSecondary, modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text(
                text = "Mis hijos",
                fontSize = 12.sp,
                color = EduviaColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 👇 RECORRE A LOS HIJOS REALES
            children.forEach { child ->
                DropdownMenuItem(
                    text = {
                        Text(
                            child.name,
                            fontWeight = if (child.id == selectedChildId) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onChildSelected(child) // 👈 Cambia de hijo
                        expanded = false
                    },
                    leadingIcon = {
                        if (child.id == selectedChildId) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = EduviaColors.Primary)
                        }
                    }
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(EduviaColors.Border))

            DropdownMenuItem(
                text = { Text("Cerrar sesión", color = Color.Red) },
                onClick = {
                    expanded = false
                    onLogoutClick()
                },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = Color.Red) }
            )
        }
    }
}
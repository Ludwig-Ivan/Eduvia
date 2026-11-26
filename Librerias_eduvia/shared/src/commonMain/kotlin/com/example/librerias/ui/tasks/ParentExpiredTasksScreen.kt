package com.example.librerias.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

// Colores exactos de la imagen
private val TabActiveBg = Color(0xFF2563EB)
private val TagTomorrowBg = Color(0xFFDBEAFE) // Para el círculo "M"
private val TagExpiredBg = Color(0xFFFBFFAF) // Amarillo verdoso claro
private val TagExpiredText = Color(0xFF898A1E) // Texto oliva oscuro

/**
 * Lista de tareas vencidas del hijo en el flujo de padres, con acceso a las
 * pestañas de tareas pendientes y entregadas.
 */
@Composable
fun ParentExpiredTasksScreen(
    onTabSelected: (BottomTab) -> Unit = {},
    onNavigateToPending: () -> Unit = {},
    onNavigateToDelivered: () -> Unit = {}
) {
    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(Modifier.fillMaxSize().background(EduviaColors.Background)) {
            // HEADER (Título y Chip de María)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tareas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.height(44.dp).background(Color.White, RoundedCornerShape(22.dp))
                        .border(0.8.dp, EduviaColors.Border, RoundedCornerShape(22.dp)).padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(TagTomorrowBg), contentAlignment = Alignment.Center) {
                        Text("M", color = TabActiveBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("María", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            // TABS (Pendientes / Entregadas / Vencidas)
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabButton("Pendientes", isSelected = false, onClick = onNavigateToPending, modifier = Modifier.weight(1f))
                TabButton("Entregadas", isSelected = false, onClick = onNavigateToDelivered, modifier = Modifier.weight(1f))
                TabButton("Vencidas", isSelected = true, onClick = {}, modifier = Modifier.weight(1f))
            }

            // LISTA DE VENCIDAS
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpiredTaskCard(
                    title = "Ciencias · Ciclo del agua",
                    subtitle = "Dibujo con foto"
                )
                Spacer(Modifier.height(16.dp))
            }

            BottomNavBar(selected = BottomTab.TASKS, onSelect = onTabSelected)
        }
    }
}

@Composable
private fun ExpiredTaskCard(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox vacío (porque no se entregó)
        Box(Modifier.size(24.dp).border(1.6.dp, Color(0xFF94A3B8), RoundedCornerShape(7.dp)))

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = EduviaColors.TextSecondary)
        }

        // Etiqueta "Vencida" en amarillo
        Box(
            modifier = Modifier.background(TagExpiredBg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Vencida", color = TagExpiredText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// Necesitamos copiar TabButton aquí porque en el otro archivo es private
@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) TabActiveBg else Color.White)
            .then(if (isSelected) Modifier else Modifier.border(0.8.dp, EduviaColors.Border, RoundedCornerShape(20.dp)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else EduviaColors.TextPrimary, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun ParentExpiredTasksPreview() {
    ParentExpiredTasksScreen()
}
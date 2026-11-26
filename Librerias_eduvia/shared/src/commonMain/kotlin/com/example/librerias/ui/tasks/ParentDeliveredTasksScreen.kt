package com.example.librerias.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle // <-- IMPORT AGREGADO
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview // <-- IMPORT AGREGADO
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

private val TabActiveBg = Color(0xFF2563EB)
private val TagTomorrowBg = Color(0xFFDBEAFE)
private val CardDeliveredBg = Color(0xFFF0FDF4)
private val CardDeliveredBorder = Color(0xFFBBF7D0)
private val TextDeliveredGreen = Color(0xFF166534)

/**
 * Lista de tareas entregadas del hijo en el flujo de padres, con acceso a las
 * pestañas de tareas pendientes y vencidas.
 */
@Composable
fun ParentDeliveredTasksScreen(
    onTabSelected: (BottomTab) -> Unit = {},
    onNavigateToPending: () -> Unit = {},
    onNavigateToExpired: () -> Unit = {}
) {
    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(Modifier.fillMaxSize().background(EduviaColors.Background)) {
            // HEADER
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

            // TABS
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabButton("Pendientes", isSelected = false, onClick = onNavigateToPending, modifier = Modifier.weight(1f))
                TabButton("Entregadas", isSelected = true, onClick = {}, modifier = Modifier.weight(1f))
                TabButton("Vencidas", isSelected = false, onClick = onNavigateToExpired, modifier = Modifier.weight(1f))
            }

            // LISTA DE ENTREGADAS
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeliveredTaskCard("Historia · Mapa de México", "Entregada el 8 de octubre")
                Spacer(Modifier.height(16.dp))
            }

            BottomNavBar(selected = BottomTab.TASKS, onSelect = onTabSelected)
        }
    }
}

@Composable
private fun DeliveredTaskCard(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(CardDeliveredBg, RoundedCornerShape(12.dp))
            .border(0.8.dp, CardDeliveredBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF16A34A)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = TextDeliveredGreen)
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
fun ParentDeliveredTasksPreview() {
    ParentDeliveredTasksScreen()
}
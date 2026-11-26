package com.example.librerias.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val ProgressBg = Color(0xFFE2E8F0)
private val ProgressFill = Color(0xFF16A34A)
private val TabActiveBg = Color(0xFF2563EB)
private val TagMissingBg = Color(0xFFFEE2E2)
private val TagMissingText = Color(0xFF991B1B)
private val TagCompleteBg = Color(0xFFDCFCE7)
private val TagCompleteText = Color(0xFF166534)

/**
 * Vista de tareas pasadas del docente para su grupo, con alternancia entre
 * tareas activas y pasadas y acceso a la creación de nuevas tareas.
 */
@Composable
fun PastTasksScreen(
    modifier: Modifier = Modifier,
    onTabSelected: (BottomTab) -> Unit = {},
    onCreateTask: () -> Unit = {}
) {
    // 0 = Activas, 1 = Pasadas. Inicia en Pasadas (1) por esta pantalla
    var selectedTab by remember { mutableStateOf(1) }

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(EduviaColors.Background)
                .then(modifier)
        ) {
            Column(Modifier.fillMaxSize()) {

                // HEADER
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                    Text(text = "Tareas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "GRUPO 3B · Código 3B-7K2P",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduviaColors.TextSecondary
                    )
                }

                // TABS (Activas / Pasadas)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton("Activas", isSelected = selectedTab == 0) { selectedTab = 0 }
                    TabButton("Pasadas", isSelected = selectedTab == 1) { selectedTab = 1 }
                }

                // LISTA DE TAREAS PASADAS
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0) {
                        Text("No hay tareas activas por ahora", color = EduviaColors.TextSecondary)
                    } else {
                        PastTaskCard(
                            title = "Historia · Mapa de México",
                            subtitle = "Venció el 8 de octubre",
                            tagText = "Faltan 5",
                            tagBg = TagMissingBg,
                            tagTextColor = TagMissingText,
                            delivered = 23,
                            total = 28
                        )
                        PastTaskCard(
                            title = "Matemáticas · Pág. 38",
                            subtitle = "Venció el 6 de octubre",
                            tagText = "Completa",
                            tagBg = TagCompleteBg,
                            tagTextColor = TagCompleteText,
                            delivered = 28,
                            total = 28
                        )
                        PastTaskCard(
                            title = "Español · Lectura cap. 4",
                            subtitle = "Venció el 2 de octubre",
                            tagText = "Faltan 2",
                            tagBg = TagMissingBg,
                            tagTextColor = TagMissingText,
                            delivered = 26,
                            total = 28
                        )
                    }
                    Spacer(Modifier.height(80.dp))
                }

                BottomNavBar(selected = BottomTab.TASKS, onSelect = onTabSelected)
            }

            // BOTÓN FLOTANTE "+ Crear"
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 88.dp)
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = EduviaColors.Primary.copy(alpha = 0.3f), spotColor = EduviaColors.Primary.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(26.dp))
                    .background(EduviaColors.Primary)
                    .clickable(onClick = onCreateTask)
                    .padding(start = 16.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text("Crear", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) TabActiveBg else Color.White)
            .then(if (isSelected) Modifier else Modifier.border(0.8.dp, EduviaColors.Border, RoundedCornerShape(20.dp)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else EduviaColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun PastTaskCard(
    title: String,
    subtitle: String,
    tagText: String,
    tagBg: Color,
    tagTextColor: Color,
    delivered: Int,
    total: Int
) {
    val fraction = if (total == 0) 0f else delivered.toFloat() / total

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 13.sp, color = EduviaColors.TextSecondary)
            }
            Box(
                modifier = Modifier.background(tagBg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tagText, color = tagTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entregadas", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("$delivered de $total", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ProgressBg)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(ProgressFill)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PastTasksPreview() {
    PastTasksScreen()
}
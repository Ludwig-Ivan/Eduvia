package com.example.librerias.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
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
import com.example.librerias.model.CalendarDate
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.Student
import com.example.librerias.model.TaskStatus
import com.example.librerias.model.daysUntil
import com.example.librerias.model.monthNames
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.components.UserChip
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

private val TabActiveBg = Color(0xFF2563EB)
private val TagTodayBg = Color(0xFFFEE2E2)
private val TagTodayText = Color(0xFF991B1B)
private val TagTomorrowBg = Color(0xFFDBEAFE)
private val TagTomorrowText = Color(0xFF1E3A8A)
private val TagDeliveredBg = Color(0xFFDCFCE7)
private val TagDeliveredText = Color(0xFF166534)
private val TagExpiredBg = Color(0xFFFEF3C7)
private val TagExpiredText = Color(0xFF92400E)

/** Flujo del padre: consulta las tareas del hijo por estado (pendientes, entregadas o vencidas). */
@Composable
fun ParentTasksScreen(
    modifier: Modifier = Modifier,
    tasks: List<SchoolTask>,
    today: CalendarDate = CalendarDate(2026, 10, 12),
    userName: String = "María",
    children: List<Student> = emptyList(),
    selectedChildId: Long? = null,
    onChildSelected: (Student) -> Unit = {},
    // 👇 AQUÍ FALTABA EL studentId PARA LAS TAREAS
    studentId: Long = selectedChildId ?: 1L,
    onTabSelected: (BottomTab) -> Unit = {},
    onTaskClick: (SchoolTask) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

    val filteredTasks = when (selectedTab) {
        0 -> tasks.filter { it.statusFor(today, studentId) == TaskStatus.PENDING }
        1 -> tasks.filter { it.statusFor(today, studentId) == TaskStatus.DELIVERED }
        2 -> tasks.filter { it.statusFor(today, studentId) == TaskStatus.EXPIRED }
        else -> tasks
    }

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Box(Modifier.fillMaxSize().background(EduviaColors.Background).then(modifier)) {
            Column(Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tareas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    UserChip(
                        name = userName,
                        children = children,
                        selectedChildId = selectedChildId,
                        onChildSelected = onChildSelected,
                        onLogoutClick = onLogoutClick
                    )
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabButton("Pendientes", selectedTab == 0, { selectedTab = 0 }, Modifier.weight(1f))
                    TabButton("Entregadas", selectedTab == 1, { selectedTab = 1 }, Modifier.weight(1f))
                    TabButton("Vencidas", selectedTab == 2, { selectedTab = 2 }, Modifier.weight(1f))
                }

                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (filteredTasks.isEmpty()) {
                        Text("No hay tareas en esta sección", color = EduviaColors.TextSecondary, modifier = Modifier.padding(top = 16.dp))
                    }
                    filteredTasks.forEach { task ->
                        ParentTaskCard(
                            task = task,
                            statusTab = selectedTab,
                            today = today,
                            onClick = { onTaskClick(task) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                BottomNavBar(selected = BottomTab.TASKS, onSelect = onTabSelected)
            }
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (isSelected) TabActiveBg else Color.White).then(if (isSelected) Modifier else Modifier.border(0.8.dp, EduviaColors.Border, RoundedCornerShape(20.dp))).clickable(onClick = onClick).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (isSelected) Color.White else EduviaColors.TextPrimary, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun ParentTaskCard(task: SchoolTask, statusTab: Int, today: CalendarDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow).background(Color.White, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (statusTab == 1) {
            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF16A34A)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        } else {
            Box(modifier = Modifier.size(24.dp).border(1.6.dp, Color(0xFF94A3B8), RoundedCornerShape(7.dp)))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${task.subject.label} · ${task.title}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

            val subtitleText = if (statusTab == 0) {
                "Vence el ${task.dueDate.day} de ${monthNames[task.dueDate.month - 1].lowercase()}"
            } else {
                task.description
            }
            Text(subtitleText, fontSize = 13.sp, color = EduviaColors.TextSecondary)
        }

        val daysDifference = today.daysUntil(task.dueDate)

        val (tagText, tagBg, tagTextColor) = when {
            statusTab == 1 -> Triple("Entregada", TagDeliveredBg, TagDeliveredText)
            statusTab == 2 -> Triple("Vencida", TagExpiredBg, TagExpiredText)
            daysDifference == 0 -> Triple("Hoy", TagTodayBg, TagTodayText)
            daysDifference == 1 -> Triple("Mañana", TagTomorrowBg, TagTomorrowText)
            else -> Triple("${task.dueDate.day} ${monthNames[task.dueDate.month - 1].take(3)}", TagTomorrowBg, TagTomorrowText)
        }

        Box(modifier = Modifier.background(tagBg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text(tagText, color = tagTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ParentTasksPreview() {
    ParentTasksScreen(tasks = emptyList())
}
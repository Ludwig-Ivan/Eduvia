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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import com.example.librerias.model.monthNames
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

/** Flujo del maestro: revisa tareas activas y pasadas del grupo y crea, edita o elimina tareas. */
@Composable
fun TeacherTasksScreen(
    modifier: Modifier = Modifier,
    tasks: List<SchoolTask>,
    today: CalendarDate = CalendarDate(2026, 10, 12),
    groupName: String = "3° B",
    groupSize: Int = 28, // 👈 CANTIDAD DE ALUMNOS
    onTabSelected: (BottomTab) -> Unit = {},
    onCreateTask: () -> Unit = {},
    onEditTask: (SchoolTask) -> Unit = {},
    onDeleteTask: (SchoolTask) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

    val filteredTasks = when (selectedTab) {
        0 -> tasks.filter { it.isActive(today) }
        1 -> tasks.filter { !it.isActive(today) }
        else -> tasks
    }

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Box(Modifier.fillMaxSize().background(EduviaColors.Background).then(modifier)) {
            Column(Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                    Text(text = "Tareas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$groupName · $groupSize alumnos", fontSize = 13.sp, color = EduviaColors.TextSecondary)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabButton(text = "Activas", isSelected = selectedTab == 0, onClick = { selectedTab = 0 })
                    TabButton(text = "Pasadas", isSelected = selectedTab == 1, onClick = { selectedTab = 1 })
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredTasks.isEmpty()) {
                        Text("No hay tareas en esta sección", color = EduviaColors.TextSecondary, modifier = Modifier.padding(top = 16.dp))
                    } else {
                        filteredTasks.forEach { task ->
                            if (selectedTab == 0) {
                                ActiveTaskCard(
                                    task = task,
                                    groupSize = groupSize, // 👈 LE PASAMOS EL TAMAÑO REAL
                                    onEdit = { onEditTask(task) },
                                    onDelete = { onDeleteTask(task) }
                                )
                            } else {
                                PastTaskCard(
                                    task = task,
                                    groupSize = groupSize, // 👈 LE PASAMOS EL TAMAÑO REAL
                                    onEdit = { onEditTask(task) },
                                    onDelete = { onDeleteTask(task) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }

                BottomNavBar(selected = BottomTab.TASKS, onSelect = onTabSelected)
            }

            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 88.dp)
                    .height(52.dp).shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = EduviaColors.Primary.copy(alpha = 0.3f), spotColor = EduviaColors.Primary.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(26.dp)).background(EduviaColors.Primary).clickable(onClick = onCreateTask).padding(start = 16.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text("Crear tarea", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) TabActiveBg else EduviaColors.Surface)
            .then(if (isSelected) Modifier else Modifier.border(0.8.dp, EduviaColors.BorderStrong, RoundedCornerShape(20.dp)))
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else EduviaColors.TextPrimary, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun ActiveTaskCard(task: SchoolTask, groupSize: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    // 👇 USAMOS EL groupSize REAL EN LUGAR DEL task.totalStudents
    val totalReal = if (task.totalStudents > 0) task.totalStudents else groupSize
    val fraction = if (totalReal == 0) 0f else task.deliveredCount.toFloat() / totalReal

    Column(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(Color.White, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("${task.subject.label} · ${task.title}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(task.description, fontSize = 13.sp, color = EduviaColors.TextSecondary)
            }
            Row {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, "Editar", tint = EduviaColors.IconMuted, modifier = Modifier.size(20.dp))
                }
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entregadas", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                // 👇 MOSTRAMOS EL TOTAL REAL
                Text("${task.deliveredCount} de $totalReal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ProgressBg)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(ProgressFill))
            }
        }
    }
}

@Composable
private fun PastTaskCard(task: SchoolTask, groupSize: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    // 👇 USAMOS EL groupSize REAL EN LUGAR DEL task.totalStudents
    val totalReal = if (task.totalStudents > 0) task.totalStudents else groupSize
    val missing = totalReal - task.deliveredCount
    val isComplete = missing <= 0
    val fraction = if (totalReal == 0) 0f else task.deliveredCount.toFloat() / totalReal

    val tagBg = if (isComplete) TagCompleteBg else TagMissingBg
    val tagText = if (isComplete) TagCompleteText else TagMissingText
    val tagLabel = if (isComplete) "Completa" else "Faltan $missing"

    Column(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${task.subject.label} · ${task.title}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Venció el ${task.dueDate.day} de ${monthNames[task.dueDate.month - 1].lowercase()}", fontSize = 13.sp, color = EduviaColors.TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(tagBg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Text(tagLabel, color = tagText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entregadas", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("${task.deliveredCount} de $totalReal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ProgressBg)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(ProgressFill))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun TeacherTasksPreview() {
    TeacherTasksScreen(tasks = emptyList(), groupName = "3° B", groupSize = 28)
}
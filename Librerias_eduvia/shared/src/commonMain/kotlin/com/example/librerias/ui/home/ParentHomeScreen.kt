package com.example.librerias.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.Student
import com.example.librerias.model.TaskStatus
import com.example.librerias.model.dateTimeLabel
import com.example.librerias.model.daysUntil
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

private val SummaryBg = Color(0xFFEFF6FF)
private val SummaryText = Color(0xFF1E40AF)
private val TagTodayBg = Color(0xFFFEE2E2)
private val TagTodayText = Color(0xFF991B1B)
private val TagNextBg = Color(0xFFFEF3C7)
private val TagNextText = Color(0xFF92400E)
private val ConfirmedGreen = Color(0xFF166534)

/** Inicio del padre: resumen de tareas y juntas, selección del hijo y navegación a las demás secciones. */
@Composable
fun ParentHomeScreen(
    modifier: Modifier = Modifier,
    tasks: List<SchoolTask>,
    meetings: List<Meeting>,
    today: CalendarDate = CalendarDate(2026, 10, 12),
    userName: String = "María",
    children: List<Student> = emptyList(),
    selectedChildId: Long? = null,
    onChildSelected: (Student) -> Unit = {},
    // 👇 AGREGAMOS EL studentId AQUÍ
    studentId: Long = selectedChildId ?: 1L,
    onTabSelected: (BottomTab) -> Unit = {},
    onTaskClick: (SchoolTask) -> Unit = {},
    onSeeAllTasksClick: () -> Unit = {},
    onMeetingClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    // 👇 FILTRAMOS LAS TAREAS PENDIENTES DEL HIJO ACTUAL
    val pendingTasks = tasks.filter { it.statusFor(today, studentId) == TaskStatus.PENDING }
    val pendingCount = pendingTasks.size

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(EduviaColors.Background)
                .then(modifier)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Inicio", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Box {
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .background(EduviaColors.Surface, RoundedCornerShape(16.dp))
                            .border(0.8.dp, EduviaColors.Border, RoundedCornerShape(16.dp))
                            .clickable { expanded = true }
                            .padding(start = 4.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(EduviaColors.PrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userName.first().uppercase(), color = EduviaColors.PrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(userName.substringBefore(" "), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Outlined.ExpandMore, null, tint = EduviaColors.TextSecondary, modifier = Modifier.size(14.dp))
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

                        children.forEach { child ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        child.name,
                                        fontWeight = if (child.id == selectedChildId) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onChildSelected(child)
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
                            leadingIcon = { Icon(Icons.Outlined.Logout, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Hola, ${userName.substringBefore(" ")}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Tu día escolar, de un vistazo.", fontSize = 14.sp, color = EduviaColors.TextSecondary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SummaryBg, RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryItem(number = tasks.size.toString(), label = "Tareas")
                    SummaryItem(number = pendingCount.toString(), label = "Pendientes") // 👈 CONTADOR CORREGIDO
                    SummaryItem(number = meetings.size.toString(), label = "Juntas")
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Tareas pendientes", onSeeAllClick = onSeeAllTasksClick)

                    // 👇 SOLO MOSTRAMOS LAS PENDIENTES
                    if (pendingTasks.isEmpty()) {
                        Text("No hay tareas pendientes", color = EduviaColors.TextSecondary, fontSize = 13.sp)
                    } else {
                        pendingTasks.take(3).forEach { task ->
                            val days = today.daysUntil(task.dueDate)
                            val tagText = when {
                                days == 0 -> "Hoy"
                                days == 1 -> "Mañana"
                                days > 1 -> "En $days días"
                                else -> "Vencida"
                            }
                            TaskCard(
                                title = "${task.subject.label} · ${task.title}",
                                subtitle = task.description,
                                tagText = tagText,
                                tagBg = if (days <= 1) TagTodayBg else TagNextBg,
                                tagTextColor = if (days <= 1) TagTodayText else TagNextText,
                                onClick = { onTaskClick(task) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Próximas reuniones")
                    meetings.take(2).forEach { meeting ->
                        MeetingCard(
                            title = meeting.title,
                            dateText = meeting.dateTimeLabel(),
                            location = meeting.location,
                            hasVirtualLink = meeting.hasVirtualLink,
                            tagText = meeting.relativeLabel,
                            tagBg = TagNextBg,
                            tagTextColor = TagNextText,
                            isConfirmed = meeting.response == MeetingResponse.ATTENDING,
                            onClick = onMeetingClick
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            BottomNavBar(selected = BottomTab.HOME, onSelect = onTabSelected)
        }
    }
}

@Composable
private fun SummaryItem(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(number, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SummaryText)
        Text(label, fontSize = 12.sp, color = SummaryText)
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAllClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (onSeeAllClick != null) {
            Text(
                "Ver todas",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = EduviaColors.Primary,
                modifier = Modifier.clickable(onClick = onSeeAllClick)
            )
        }
    }
}

@Composable
private fun TaskCard(
    title: String,
    subtitle: String,
    tagText: String? = null,
    tagBg: Color = Color.Transparent,
    tagTextColor: Color = Color.Transparent,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(EduviaColors.Surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.5.dp, EduviaColors.BorderStrong, RoundedCornerShape(6.dp))
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = EduviaColors.TextSecondary)
        }

        if (tagText != null) {
            Box(
                modifier = Modifier.background(tagBg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tagText, color = tagTextColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MeetingCard(
    title: String,
    dateText: String,
    location: String?,
    hasVirtualLink: Boolean,
    tagText: String? = null,
    tagBg: Color = Color.Transparent,
    tagTextColor: Color = Color.Transparent,
    isConfirmed: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(EduviaColors.Surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (tagText != null) {
                Box(
                    modifier = Modifier.background(tagBg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(tagText, color = tagTextColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text(dateText, fontSize = 13.sp, color = EduviaColors.TextSecondary)

        if (location != null) {
            Text(location, fontSize = 13.sp, color = EduviaColors.TextSecondary)
        }

        if (hasVirtualLink) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Videocam, null, tint = EduviaColors.Primary, modifier = Modifier.size(16.dp))
                Text("Enlace virtual disponible", color = EduviaColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (isConfirmed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Check, null, tint = ConfirmedGreen, modifier = Modifier.size(16.dp))
                Text("Confirmaste asistencia", color = ConfirmedGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ParentHomePreview() {
    ParentHomeScreen(tasks = emptyList(), meetings = emptyList())
}
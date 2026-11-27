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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.model.CalendarDate
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.SchoolGroup
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.longLabel
import com.example.librerias.model.monthNames
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.meetings.MeetingsViewModel
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

/** Inicio del maestro: resumen del grupo y sus próximas juntas/tareas, con acceso a crear juntas. */
@Composable
fun TeacherHomeScreen(
    modifier: Modifier = Modifier,
    meetings: List<Meeting> = emptyList(),
    tasks: List<SchoolTask> = emptyList(),
    viewModel: MeetingsViewModel,
    today: CalendarDate,
    userName: String = "",
    groupName: String = "",
    groupSize: Int = 0, // 👈 CAMBIAMOS EL 28 POR 0
    groups: List<SchoolGroup> = emptyList(),
    onGroupSelected: (SchoolGroup) -> Unit = {},
    onTabSelected: (BottomTab) -> Unit = {},
    onCreateMeeting: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val tasksThisWeek = tasks.size
    val nextMeeting = meetings.find { it.isNext } ?: meetings.firstOrNull()
    val deliveriesToday = tasks.take(3)

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Box(Modifier.fillMaxSize().background(EduviaColors.Background).then(modifier)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(today.longLabel(), fontSize = 13.sp, color = EduviaColors.TextSecondary)
                        Text("Hola, ${userName.substringBefore(" ")}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Box {
                        Row(
                            modifier = Modifier.height(36.dp).background(EduviaColors.Surface, RoundedCornerShape(18.dp))
                                .border(0.8.dp, EduviaColors.Border, RoundedCornerShape(18.dp))
                                .clickable { expanded = true }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(groupName.take(2), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Outlined.ExpandMore, null, tint = EduviaColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            Text("Mis grupos", fontSize = 12.sp, color = EduviaColors.TextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            group.name,
                                            fontWeight = if (group.name == groupName) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onGroupSelected(group)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        if (group.name == groupName) {
                                            Icon(Icons.Outlined.Check, contentDescription = null, tint = EduviaColors.Primary)
                                        }
                                    }
                                )
                            }

                            Box(Modifier.fillMaxWidth().height(1.dp).background(EduviaColors.Border))
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión", color = Color.Red) },
                                onClick = { expanded = false; onLogoutClick() },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(number = tasksThisWeek.toString(), label = "Tareas publicadas esta semana", numberColor = Color(0xFF1D4ED8), modifier = Modifier.weight(1f))
                        SummaryCard(number = meetings.size.toString(), label = "Juntas activas", numberColor = Color(0xFFB45309), modifier = Modifier.weight(1f))
                    }
                    NextMeetingCard(meeting = nextMeeting, viewModel = viewModel, groupSize = groupSize) // 👈 AGREGA ESTO
                    TodayDeliveriesCard(tasks = deliveriesToday, groupSize = groupSize) // 👈 LE PASAMOS EL TAMAÑO REAL
                    Spacer(Modifier.height(80.dp))
                }

                BottomNavBar(selected = BottomTab.HOME, onSelect = onTabSelected)
            }

            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 88.dp).height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = EduviaColors.Primary.copy(alpha = 0.3f), spotColor = EduviaColors.Primary.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(26.dp)).background(EduviaColors.Primary).clickable(onClick = onCreateMeeting).padding(start = 16.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text("Crear", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    number: String,
    label: String,
    numberColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(EduviaColors.Surface, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(number, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = numberColor)
        Text(label, fontSize = 13.sp, color = EduviaColors.TextSecondary, lineHeight = 16.sp)
    }
}

@Composable
private fun NextMeetingCard(meeting: Meeting?, viewModel: MeetingsViewModel, groupSize: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(EduviaColors.Surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Próxima junta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                meeting?.relativeLabel ?: "Sin juntas próximas",
                fontSize = 13.sp,
                color = EduviaColors.TextSecondary
            )
        }

        if (meeting == null) {
            Text(
                "Aún no tienes juntas programadas.",
                fontSize = 14.sp,
                color = EduviaColors.TextSecondary
            )
        } else {
            val attendance by viewModel.getAttendanceFlow(meeting.id).collectAsState(initial = emptyList())
            val confirmed = attendance.count { it.response == MeetingResponse.ATTENDING }

            // 👇 USAMOS EL groupSize SI EL attendance ESTÁ VACÍO
            val totalReal = if (attendance.isNotEmpty()) attendance.size else groupSize
            val fraction = if (totalReal == 0) 0f else confirmed.toFloat() / totalReal

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            monthNames[meeting.date.month - 1].take(3).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                        Text(
                            meeting.date.day.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(meeting.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${meeting.time} · ${meeting.location}",
                        fontSize = 13.sp,
                        color = EduviaColors.TextSecondary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Confirmados", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    // 👇 AQUÍ USAMOS totalReal
                    Text("$confirmed de $totalReal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(Modifier.fillMaxWidth(fraction).height(8.dp).background(Color(0xFF16A34A)))
                }
            }
        }
    }
}

@Composable
private fun TodayDeliveriesCard(tasks: List<SchoolTask>, groupSize: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(EduviaColors.Surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Entregas de hoy", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        if (tasks.isEmpty()) {
            Text(
                "No hay entregas programadas para hoy.",
                fontSize = 13.sp,
                color = EduviaColors.TextSecondary
            )
        } else {
            tasks.take(3).forEach { task ->
                // 👇 SI EL TOTAL ES 0, AHORA MUESTRA 0 (NO EL 28)
                val totalReal = if (task.totalStudents > 0) task.totalStudents else groupSize
                DeliveryRow(
                    subject = "${task.subject.label} · ${task.title}",
                    count = "${task.deliveredCount} de $totalReal"
                )
            }
        }
    }
}

@Composable
private fun DeliveryRow(subject: String, count: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(subject, fontSize = 14.sp)
        Text(count, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

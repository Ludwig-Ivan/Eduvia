package com.example.librerias.ui.meetings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.dateTimeLabel
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private val NextTagBg = Color(0xFFFEF3C7)
private val NextTagText = Color(0xFF78350F)
private val ProgressBg = Color(0xFFE2E8F0)
private val ProgressFill = Color(0xFF16A34A)

/** Flujo del maestro: gestiona las juntas del grupo (crear, editar, recordar y eliminar) y ve la asistencia. */
@Composable
fun TeacherMeetingsScreen(
    modifier: Modifier = Modifier,
    meetings: List<Meeting>,
    viewModel: MeetingsViewModel,
    groupName: String = "3° B",
    groupCode: String = "3B-7K2P",
    groupSize: Int = 0,
    message: String? = null,
    onTabSelected: (BottomTab) -> Unit = {},
    onCreate: () -> Unit = {},
    onEditMeeting: (Meeting) -> Unit = {},
    onRemind: (Meeting) -> Unit = {},
    onDeleteMeeting: (Meeting) -> Unit = {}
) {
    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Box(Modifier.fillMaxSize().background(EduviaColors.Background).then(modifier)) {
            Column(Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                    Text(text = "Juntas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$groupName · Código $groupCode", fontSize = 13.sp, color = EduviaColors.TextSecondary)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (meetings.isEmpty()) {
                        Text("No hay juntas programadas", color = EduviaColors.TextSecondary)
                    }
                    meetings.forEach { meeting ->
                        if (meeting.isNext) {
                            TeacherNextMeetingCard(
                                meeting = meeting,
                                viewModel = viewModel,
                                groupSize = groupSize, // 👈 LE PASAMOS EL TAMAÑO
                                onEdit = { onEditMeeting(meeting) },
                                onRemind = { onRemind(meeting) },
                                onDelete = { onDeleteMeeting(meeting) }
                            )
                        } else {
                            TeacherPastMeetingCard(
                                meeting = meeting,
                                viewModel = viewModel,
                                groupSize = groupSize, // 👈 LE PASAMOS EL TAMAÑO
                                onEdit = { onEditMeeting(meeting) },
                                onDelete = { onDeleteMeeting(meeting) }
                            )
                        }
                    }
                }

                BottomNavBar(selected = BottomTab.MEETINGS, onSelect = onTabSelected)
            }

            TeacherCreateFab(
                onClick = onCreate,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 88.dp)
            )
            if (message != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(12.dp))
                        .background(Color(0xFF166534), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(message, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TeacherNextMeetingCard(
    meeting: Meeting,
    viewModel: MeetingsViewModel,
    groupSize: Int, // 👈 NUEVO
    onEdit: () -> Unit,
    onRemind: () -> Unit,
    onDelete: () -> Unit
) {
    val attendance by viewModel.getAttendanceFlow(meeting.id).collectAsState(initial = emptyList())
    val confirmed = attendance.count { it.response == MeetingResponse.ATTENDING }
    val declined = attendance.count { it.response == MeetingResponse.DECLINED }
    val pending = attendance.count { it.response == MeetingResponse.PENDING }
    var reminded by remember(meeting.id) { mutableStateOf(false) }
    val canRemind = pending > 0 && !reminded

    // 👇 USAMOS EL groupSize SI EL attendance ESTÁ VACÍO
    val totalReal = if (attendance.isNotEmpty()) attendance.size else groupSize
    val fraction = if (totalReal == 0) 0f else confirmed.toFloat() / totalReal

    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier.fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(Color.White, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.height(24.dp).background(NextTagBg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text("Próxima", color = NextTagText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

            Row {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, "Editar", tint = EduviaColors.IconMuted, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(meeting.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(meeting.dateTimeLabel(), color = EduviaColors.TextSecondary, fontSize = 14.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Confirmados", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                // 👇 USAMOS totalReal AQUÍ
                Text("$confirmed de $totalReal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ProgressBg)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(ProgressFill))
            }
            // 👇 USAMOS totalReal PARA LOS PENDIENTES
            Text("$declined dijeron que no pueden · $pending sin responder", color = EduviaColors.TextSecondary, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (canRemind) Color.Transparent else Color(0xFFF1F5F9))
                .border(0.8.dp, EduviaColors.BorderStrong, RoundedCornerShape(22.dp))
                .clickable(enabled = canRemind) { reminded = true; onRemind() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    pending == 0 -> "Todos ya respondieron"
                    reminded -> "Recordatorio enviado ✓"
                    else -> "Recordar a quienes no responden"
                },
                color = if (canRemind) EduviaColors.TextPrimary else EduviaColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TeacherPastMeetingCard(
    meeting: Meeting,
    viewModel: MeetingsViewModel,
    groupSize: Int, // 👈 NUEVO
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val attendance by viewModel.getAttendanceFlow(meeting.id).collectAsState(initial = emptyList())
    val confirmed = attendance.count { it.response == MeetingResponse.ATTENDING }

    // 👇 USAMOS EL groupSize SI EL attendance ESTÁ VACÍO
    val totalReal = if (attendance.isNotEmpty()) attendance.size else groupSize

    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier.fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(Color.White, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(meeting.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, "Editar", tint = EduviaColors.IconMuted, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, "Eliminar", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
        Text(meeting.dateTimeLabel(), color = EduviaColors.TextSecondary, fontSize = 14.sp)
        // 👇 USAMOS totalReal AQUÍ
        Text("Confirmados $confirmed de $totalReal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TeacherCreateFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    val glow = EduviaColors.Primary.copy(alpha = 0.35f)
    Row(
        modifier = modifier.height(52.dp)
            .shadow(8.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape).background(EduviaColors.Primary)
            .clickable(onClick = onClick).padding(start = 16.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text("Crear", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

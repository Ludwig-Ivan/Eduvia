package com.example.librerias.ui.meetings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.data.MeetingSampleData
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.Student
import com.example.librerias.model.dateTimeLabel
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.components.UserChip
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

private val ConfirmedGreen = Color(0xFF166534)
private val DeclinedRed = Color(0xFF991B1B)

/** Flujo del padre/alumno: consulta juntas y responde a la convocatoria (asiste o no puede). */
@Composable
fun MeetingsScreen(
    modifier: Modifier = Modifier,
    meetings: List<Meeting>,
    userName: String = "María",
    children: List<Student> = emptyList(), // 👈 NUEVO
    selectedChildId: Long? = null,        // 👈 NUEVO
    onChildSelected: (Student) -> Unit = {}, // 👈 NUEVO
    onTabSelected: (BottomTab) -> Unit = {},
    onRespond: (Meeting, MeetingResponse) -> Unit = { _, _ -> },
    onCreate: (() -> Unit)? = null,
    onLogoutClick: () -> Unit = {}
) {
    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(EduviaColors.Background)
                .then(modifier)
        ) {
            Column(Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Juntas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    UserChip(
                        name = userName,
                        children = children,
                        selectedChildId = selectedChildId,
                        onChildSelected = onChildSelected,
                        onLogoutClick = onLogoutClick
                    ) // 👈 LE PASAMOS LOS HIJOS
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = if (onCreate != null) 88.dp else 16.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (meetings.isEmpty()) {
                        Text(
                            text = "No hay juntas programadas",
                            color = EduviaColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    meetings.forEach { meeting ->
                        MeetingCard(meeting = meeting, onRespond = onRespond)
                    }
                }

                BottomNavBar(selected = BottomTab.MEETINGS, onSelect = onTabSelected)
            }

            if (onCreate != null) {
                MeetingsCreateFab(
                    onClick = onCreate,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 88.dp)
                )
            }
        }
    }
}

// ===========================================================================
// TARJETA DE JUNTA
// ===========================================================================

@Composable
private fun MeetingCard(meeting: Meeting, onRespond: (Meeting, MeetingResponse) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = shape,
                ambientColor = EduviaColors.Shadow,
                spotColor = EduviaColors.Shadow
            )
            .background(EduviaColors.Surface, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (meeting.isNext) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NextTag()
                meeting.relativeLabel?.let {
                    Text(text = it, color = EduviaColors.TextSecondary, fontSize = 13.sp)
                }
            }
        }

        Text(
            text = meeting.title,
            fontSize = if (meeting.isNext) 18.sp else 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (meeting.isNext) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow(Icons.Outlined.CalendarToday, meeting.dateTimeLabel())
                InfoRow(Icons.Outlined.LocationOn, meeting.location)
                if (meeting.hasVirtualLink) {
                    InfoRow(Icons.Outlined.Videocam, "Enlace virtual disponible")
                }
            }
        } else {
            Text(
                text = "${meeting.dateTimeLabel()} · ${meeting.location}",
                color = EduviaColors.TextSecondary,
                fontSize = 14.sp
            )
        }

        ResponseSection(meeting = meeting, onRespond = onRespond)
    }
}

@Composable
private fun NextTag() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .background(EduviaColors.MeetingSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Próxima",
            color = EduviaColors.MeetingDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EduviaColors.IconMuted,
            modifier = Modifier.size(18.dp)
        )
        Text(text = text, color = EduviaColors.IconMuted, fontSize = 14.sp)
    }
}

// ===========================================================================
// RESPUESTA: botones o estado
// ===========================================================================

@Composable
private fun ResponseSection(meeting: Meeting, onRespond: (Meeting, MeetingResponse) -> Unit) {
    when (meeting.response) {
        MeetingResponse.PENDING -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeetingPillButton(
                text = "Asistiré",
                filled = true,
                onClick = { onRespond(meeting, MeetingResponse.ATTENDING) },
                modifier = Modifier.weight(1f)
            )
            MeetingPillButton(
                text = "No puedo",
                filled = false,
                onClick = { onRespond(meeting, MeetingResponse.DECLINED) },
                modifier = Modifier.weight(1f)
            )
        }

        MeetingResponse.ATTENDING -> ResponseStatus(
            icon = Icons.Outlined.Check,
            text = "Confirmaste asistencia",
            color = ConfirmedGreen,
            onChange = { onRespond(meeting, MeetingResponse.PENDING) }
        )

        MeetingResponse.DECLINED -> ResponseStatus(
            icon = Icons.Outlined.Close,
            text = "Avisaste que no puedes asistir",
            color = DeclinedRed,
            onChange = { onRespond(meeting, MeetingResponse.PENDING) }
        )
    }
}

@Composable
private fun ResponseStatus(icon: ImageVector, text: String, color: Color, onChange: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(text = text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onChange)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cambiar",
                color = EduviaColors.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MeetingPillButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (filled) EduviaColors.Primary else EduviaColors.Surface)
            .then(if (filled) Modifier else Modifier.border(0.8.dp, EduviaColors.BorderStrong, shape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (filled) Color.White else EduviaColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ===========================================================================
// BOTÓN FLOTANTE "+ Crear"
// ===========================================================================

@Composable
private fun MeetingsCreateFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    val glow = EduviaColors.Primary.copy(alpha = 0.35f)
    Row(
        modifier = modifier
            .height(52.dp)
            .shadow(elevation = 8.dp, shape = shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(EduviaColors.Primary)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Text(text = "Crear", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ===========================================================================
// PREVIEW
// ===========================================================================

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun MeetingsScreenPreview() {
    MeetingsScreen(meetings = MeetingSampleData.meetings)
}
package com.example.librerias.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.model.CalendarDate
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.CalendarEventType
import com.example.librerias.model.Meeting
import com.example.librerias.model.MeetingResponse
import com.example.librerias.model.Student
import com.example.librerias.model.daysInMonth
import com.example.librerias.model.longLabel
import com.example.librerias.model.monthNames
import com.example.librerias.model.weekdayIndexMondayFirst
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.components.UserChip
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

private val ConfirmedGreen = Color(0xFF166534)
private val DeclinedRed = Color(0xFF991B1B)

/** Calendario del padre/alumno: navega por meses, consulta eventos y confirma o rechaza la asistencia. */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    events: List<CalendarEvent>,
    meetings: List<Meeting>,
    today: CalendarDate,
    initialSelected: CalendarDate = today,
    userName: String = "María",
    children: List<Student> = emptyList(), // 👈 NUEVO
    selectedChildId: Long? = null,        // 👈 NUEVO
    onChildSelected: (Student) -> Unit = {}, // 👈 NUEVO
    onTabSelected: (BottomTab) -> Unit = {},
    onRsvp: (event: CalendarEvent, attending: Boolean) -> Unit = { _, _ -> },
    onEventClick: (event: CalendarEvent) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var visibleYear by remember { mutableIntStateOf(initialSelected.year) }
    var visibleMonth by remember { mutableIntStateOf(initialSelected.month) }
    var selected by remember { mutableStateOf(initialSelected) }

    val eventsByDate = remember(events) { events.groupBy { it.date } }

    val shiftMonth: (Int) -> Unit = { delta ->
        val index = visibleYear * 12 + (visibleMonth - 1) + delta
        visibleYear = index / 12
        visibleMonth = index % 12 + 1
    }

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(EduviaColors.Background)
                .then(modifier)
        ) {
            CalendarHeader(
                userName = userName,
                children = children,
                selectedChildId = selectedChildId,
                onChildSelected = onChildSelected,
                onLogoutClick = onLogoutClick
            )

            MonthSwitcher(
                title = "${monthNames[visibleMonth - 1]} $visibleYear",
                onPrevious = { shiftMonth(-1) },
                onNext = { shiftMonth(1) }
            )

            WeekdayHeader()

            MonthGrid(
                year = visibleYear,
                month = visibleMonth,
                today = today,
                selected = selected,
                eventsByDate = eventsByDate,
                onDayClick = { selected = it }
            )

            Legend()

            SelectedDaySection(
                date = selected,
                events = eventsByDate[selected].orEmpty(),
                meetings = meetings,
                onRsvp = onRsvp,
                onEventClick = onEventClick,
                modifier = Modifier.weight(1f)
            )

            BottomNavBar(selected = BottomTab.CALENDAR, onSelect = onTabSelected)
        }
    }
}

@Composable
private fun CalendarHeader(
    userName: String,
    children: List<Student>,
    selectedChildId: Long?,
    onChildSelected: (Student) -> Unit,
    onLogoutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Calendario",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        UserChip(
            name = userName,
            children = children,
            selectedChildId = selectedChildId,
            onChildSelected = onChildSelected,
            onLogoutClick = onLogoutClick
        ) // 👈 LE PASAMOS LOS HIJOS AL CHIP
    }
}

@Composable
private fun MonthSwitcher(title: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ArrowButton(Icons.Outlined.ChevronLeft, "Mes anterior", onPrevious)
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        ArrowButton(Icons.Outlined.ChevronRight, "Mes siguiente", onNext)
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = EduviaColors.TextPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp)) {
        listOf("L", "M", "M", "J", "V", "S", "D").forEach { letter ->
            Text(
                text = letter,
                color = EduviaColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    today: CalendarDate,
    selected: CalendarDate,
    eventsByDate: Map<CalendarDate, List<CalendarEvent>>,
    onDayClick: (CalendarDate) -> Unit
) {
    val offset = weekdayIndexMondayFirst(year, month, 1)
    val totalDays = daysInMonth(year, month)
    val rows = (offset + totalDays + 6) / 7

    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp)) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth().height(48.dp)) {
                repeat(7) { col ->
                    val day = row * 7 + col - offset + 1
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (day in 1..totalDays) {
                            val date = CalendarDate(year, month, day)
                            val types = eventsByDate[date]
                                .orEmpty()
                                .map { it.type }
                                .distinct()
                                .sortedBy { it.ordinal }

                            DayCell(
                                day = day,
                                isToday = date == today,
                                isSelected = date == selected,
                                eventTypes = types,
                                onClick = { onDayClick(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    eventTypes: List<CalendarEventType>,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val circleModifier = when {
            isToday -> Modifier.background(EduviaColors.Primary, CircleShape)
            isSelected -> Modifier
                .background(EduviaColors.PrimarySoft, CircleShape)
                .border(1.6.dp, EduviaColors.Primary, CircleShape)
            else -> Modifier
        }
        val textColor = when {
            isToday -> Color.White
            isSelected -> EduviaColors.PrimaryDark
            else -> EduviaColors.TextPrimary
        }

        Box(
            modifier = Modifier.size(32.dp).then(circleModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            eventTypes.forEach { type ->
                Box(Modifier.size(6.dp).background(type.dotColor(), CircleShape))
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(EduviaColors.Task, "Tareas")
        LegendItem(EduviaColors.Meeting, "Juntas")
        LegendItem(EduviaColors.Exam, "Exámenes")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(text = label, color = EduviaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectedDaySection(
    date: CalendarDate,
    events: List<CalendarEvent>,
    meetings: List<Meeting>,
    onRsvp: (CalendarEvent, Boolean) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = date.longLabel(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        if (events.isEmpty()) {
            Text(
                text = "Sin eventos para este día",
                color = EduviaColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        events.forEach { event ->
            val realMeeting = meetings.find { it.id == event.id }

            EventCard(
                event = event,
                realMeetingResponse = realMeeting?.response,
                onRsvp = { attending -> onRsvp(event, attending) },
                onClick = { onEventClick(event) }
            )
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    realMeetingResponse: MeetingResponse?,
    onRsvp: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val isConfirmed = realMeetingResponse == MeetingResponse.ATTENDING
    val isDeclined = realMeetingResponse == MeetingResponse.DECLINED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = EduviaColors.Shadow, spotColor = EduviaColors.Shadow)
            .background(EduviaColors.Surface, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(event.type.softColor()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = event.type.icon(),
                    contentDescription = null,
                    tint = event.type.darkColor(),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(text = event.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = event.subtitle, color = EduviaColors.TextSecondary, fontSize = 13.sp)
            }
        }

        if (event.type == CalendarEventType.MEETING) {
            if (isConfirmed || isDeclined) {
                val color = if (isConfirmed) ConfirmedGreen else DeclinedRed
                val icon = if (isConfirmed) Icons.Outlined.Check else Icons.Outlined.Close
                val text = if (isConfirmed) "Confirmaste asistencia" else "Avisaste que no puedes"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                        Text(text = text, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onRsvp(false) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Cambiar", color = EduviaColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Asistiré", filled = true, onClick = { onRsvp(true) }, modifier = Modifier.weight(1f))
                    PillButton("No puedo", filled = false, onClick = { onRsvp(false) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PillButton(
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

private fun CalendarEventType.dotColor(): Color = when (this) {
    CalendarEventType.TASK -> EduviaColors.Task
    CalendarEventType.MEETING -> EduviaColors.Meeting
    CalendarEventType.EXAM -> EduviaColors.Exam
}

private fun CalendarEventType.softColor(): Color = when (this) {
    CalendarEventType.TASK -> EduviaColors.PrimarySoft
    CalendarEventType.MEETING -> EduviaColors.MeetingSoft
    CalendarEventType.EXAM -> EduviaColors.ExamSoft
}

private fun CalendarEventType.darkColor(): Color = when (this) {
    CalendarEventType.TASK -> EduviaColors.PrimaryDark
    CalendarEventType.MEETING -> EduviaColors.MeetingDark
    CalendarEventType.EXAM -> EduviaColors.ExamDark
}

private fun CalendarEventType.icon(): ImageVector = when (this) {
    CalendarEventType.TASK -> Icons.Outlined.CheckBox
    CalendarEventType.MEETING -> Icons.Outlined.People
    CalendarEventType.EXAM -> Icons.Outlined.School
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CalendarScreenPreview() {
    CalendarScreen(
        events = emptyList(),
        meetings = emptyList(),
        today = CalendarDate(2026, 10, 12)
    )
}
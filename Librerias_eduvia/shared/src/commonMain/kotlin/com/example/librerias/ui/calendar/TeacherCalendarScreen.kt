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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
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
import com.example.librerias.model.AttendanceSummary
import com.example.librerias.model.CalendarDate
import com.example.librerias.model.CalendarEvent
import com.example.librerias.model.CalendarEventType
import com.example.librerias.model.daysInMonth
import com.example.librerias.model.longLabel
import com.example.librerias.model.monthNames
import com.example.librerias.model.weekdayIndexMondayFirst
import com.example.librerias.ui.components.BottomNavBar
import com.example.librerias.ui.components.BottomTab
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

// ===========================================================================
// PANTALLA: D · Mensual (maestro)
// ===========================================================================

/** Calendario del maestro: consulta los eventos del grupo y crea o edita eventos en la fecha elegida. */
@Composable
fun TeacherCalendarScreen(
    modifier: Modifier = Modifier,
    events: List<CalendarEvent> = emptyList(),
    today: CalendarDate,                                      // ✅ Dinámico
    initialSelected: CalendarDate = today,
    groupName: String = "Grupo 3B",
    groupCode: String = "3B-7K2P",
    onTabSelected: (BottomTab) -> Unit = {},
    onCreate: () -> Unit = {},
    onCreateAtDate: (CalendarDate) -> Unit = {},              // 👈 NUEVO
    onEditEvent: (CalendarEvent) -> Unit = {}
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
        Box(
            Modifier
                .fillMaxSize()
                .background(EduviaColors.Background)
                .then(modifier)
        ) {
            Column(Modifier.fillMaxSize()) {
                // 👇 HEADER SIN EL BOTÓN DE DESCARGA
                TeacherHeader(groupName = groupName, groupCode = groupCode)

                TeacherMonthSwitcher(
                    title = "${monthNames[visibleMonth - 1]} $visibleYear",
                    onPrevious = { shiftMonth(-1) },
                    onNext = { shiftMonth(1) }
                )

                TeacherWeekdayHeader()

                TeacherMonthGrid(
                    year = visibleYear,
                    month = visibleMonth,
                    today = today,
                    selected = selected,
                    eventsByDate = eventsByDate,
                    onDayClick = { selected = it }
                )

                TeacherLegend()

                TeacherSelectedDaySection(
                    date = selected,
                    events = eventsByDate[selected].orEmpty(),
                    onEdit = onEditEvent,
                    modifier = Modifier.weight(1f)
                )

                BottomNavBar(selected = BottomTab.CALENDAR, onSelect = onTabSelected)
            }

            // Botón flotante "+ Crear"
            TeacherCreateFab(
                onClick = { onCreateAtDate(selected) }, // 👈 Le pasamos la fecha seleccionada
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 88.dp)
            )
        }
    }
}

// ===========================================================================
// HEADER (título + grupo/código) - SIN BOTÓN DE EXPORTAR
// ===========================================================================

@Composable
private fun TeacherHeader(groupName: String, groupCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 8.dp), // top=48 para no chocar con la cámara
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = "Calendario", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "$groupName · Código $groupCode",
                color = EduviaColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

// ===========================================================================
// MES: flechas + título
// ===========================================================================

@Composable
private fun TeacherMonthSwitcher(title: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TeacherArrowButton(Icons.Outlined.ChevronLeft, "Mes anterior", onPrevious)
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        TeacherArrowButton(Icons.Outlined.ChevronRight, "Mes siguiente", onNext)
    }
}

@Composable
private fun TeacherArrowButton(icon: ImageVector, description: String, onClick: () -> Unit) {
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

// ===========================================================================
// ENCABEZADO DE DÍAS (L M M J V S D)
// ===========================================================================

@Composable
private fun TeacherWeekdayHeader() {
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

// ===========================================================================
// CUADRÍCULA DEL MES
// ===========================================================================

@Composable
private fun TeacherMonthGrid(
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

                            TeacherDayCell(
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
private fun TeacherDayCell(
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
                Box(Modifier.size(6.dp).background(type.teacherDotColor(), CircleShape))
            }
        }
    }
}

// ===========================================================================
// LEYENDA
// ===========================================================================

@Composable
private fun TeacherLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeacherLegendItem(EduviaColors.Task, "Tareas")
        TeacherLegendItem(EduviaColors.Meeting, "Juntas")
        TeacherLegendItem(EduviaColors.Exam, "Exámenes")
    }
}

@Composable
private fun TeacherLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(text = label, color = EduviaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ===========================================================================
// SECCIÓN DEL DÍA SELECCIONADO
// ===========================================================================

@Composable
private fun TeacherSelectedDaySection(
    date: CalendarDate,
    events: List<CalendarEvent>,
    onEdit: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 88.dp),
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
            TeacherEventCard(event = event, onEdit = { onEdit(event) })
        }
    }
}

@Composable
private fun TeacherEventCard(event: CalendarEvent, onEdit: () -> Unit) {
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
                    .background(event.type.teacherSoftColor()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = event.type.teacherIcon(),
                    contentDescription = null,
                    tint = event.type.teacherDarkColor(),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(text = event.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = event.subtitle, color = EduviaColors.TextSecondary, fontSize = 13.sp)
            }
            TeacherEditButton(onClick = onEdit)
        }

        event.attendance?.let { TeacherAttendanceBar(it) }
    }
}

@Composable
private fun TeacherEditButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Editar",
            tint = EduviaColors.IconMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TeacherAttendanceBar(attendance: AttendanceSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Confirmados", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "${attendance.confirmed} de ${attendance.total}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(EduviaColors.Border)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(attendance.confirmedFraction)
                    .fillMaxHeight()
                    .background(EduviaColors.Success)
            )
        }
    }
}

// ===========================================================================
// BOTÓN FLOTANTE "+ Crear"
// ===========================================================================

@Composable
private fun TeacherCreateFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
// COLORES / ICONOS POR TIPO DE EVENTO
// ===========================================================================

private fun CalendarEventType.teacherDotColor(): Color = when (this) {
    CalendarEventType.TASK -> EduviaColors.Task
    CalendarEventType.MEETING -> EduviaColors.Meeting
    CalendarEventType.EXAM -> EduviaColors.Exam
}

private fun CalendarEventType.teacherSoftColor(): Color = when (this) {
    CalendarEventType.TASK -> EduviaColors.PrimarySoft
    CalendarEventType.MEETING -> EduviaColors.MeetingSoft
    CalendarEventType.EXAM -> EduviaColors.ExamSoft
}

private fun CalendarEventType.teacherDarkColor(): Color = when (this) {
    CalendarEventType.TASK -> EduviaColors.PrimaryDark
    CalendarEventType.MEETING -> EduviaColors.MeetingDark
    CalendarEventType.EXAM -> EduviaColors.ExamDark
}

private fun CalendarEventType.teacherIcon(): ImageVector = when (this) {
    CalendarEventType.TASK -> Icons.Outlined.CheckBox
    CalendarEventType.MEETING -> Icons.Outlined.People
    CalendarEventType.EXAM -> Icons.Outlined.School
}

// ===========================================================================
// PREVIEW
// ===========================================================================

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun TeacherCalendarScreenPreview() {
    TeacherCalendarScreen(
        today = CalendarDate(2026, 10, 12)
    )
}
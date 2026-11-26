package com.example.librerias.ui.meetings

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.model.Meeting
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

/** Flujo del maestro para crear una junta o editar una ya existente. */
@Composable
fun CreateMeetingScreen(
    modifier: Modifier = Modifier,
    meetingToEdit: Meeting? = null,
    initialDate: String = "",
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onClose: () -> Unit = {},
    onPublish: (title: String, date: String, time: String, place: String, link: String, reminder: Boolean) -> Unit = { _, _, _, _, _, _ -> }
) {
    var title by remember { mutableStateOf(meetingToEdit?.title ?: "") }
    var date by remember {
        mutableStateOf(
            if (initialDate.isNotBlank()) initialDate
            else meetingToEdit?.date?.let { "${it.day}/${it.month}/${it.year}" } ?: ""
        )
    }
    var time by remember { mutableStateOf(meetingToEdit?.time ?: "") }
    var place by remember { mutableStateOf(meetingToEdit?.location ?: "") }
    var link by remember { mutableStateOf(meetingToEdit?.virtualLink ?: "") }
    var reminder by remember { mutableStateOf(meetingToEdit?.reminderEnabled ?: true) }

    // Qué campo marcar en rojo según el mensaje que devolvió el caso de uso
    val titleError = errorMessage?.startsWith("Escribe el título") == true
    val dateError = errorMessage?.startsWith("La fecha") == true
    val timeError = errorMessage?.startsWith("La hora") == true || errorMessage?.startsWith("Esa hora") == true
    val placeError = errorMessage?.startsWith("Escribe el lugar") == true
    val linkError = errorMessage?.startsWith("El enlace") == true

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(Modifier.fillMaxSize().background(EduviaColors.Background).then(modifier)) {

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onClose() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Close, "Cerrar", tint = EduviaColors.TextPrimary, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = if (meetingToEdit != null) "Editar junta" else "Nueva junta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // CONTENIDO
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormField(label = "Título", value = title, onValueChange = { title = it; onClearError() }, placeholder = "Ej. Junta de padres", isError = titleError)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(label = "Fecha (DD/MM/AAAA)", value = date, onValueChange = { date = it; onClearError() }, placeholder = "Ej. 19/11/2026", modifier = Modifier.weight(1f), isError = dateError)
                    FormField(label = "Hora", value = time, onValueChange = { time = it; onClearError() }, placeholder = "Ej. 8:00 am", modifier = Modifier.weight(1f), isError = timeError)
                }
                FormField(label = "Lugar", value = place, onValueChange = { place = it; onClearError() }, placeholder = "Ej. Salón 3B", isError = placeError)
                FormField(label = "Enlace virtual (opcional)", value = link, onValueChange = { link = it; onClearError() }, placeholder = "Pega el enlace de la videollamada", isError = linkError)
                ReminderSwitch(checked = reminder, onCheckedChange = { reminder = it })
                Spacer(Modifier.height(8.dp))
            }

            // ERROR + BOTÓN PUBLICAR
            Column(
                modifier = Modifier.fillMaxWidth().background(EduviaColors.Background)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Box(Modifier.fillMaxWidth().background(Color(0xFFFFE4E4), RoundedCornerShape(10.dp)).padding(12.dp)) {
                        Text(errorMessage, color = Color(0xFFB91C1C), fontSize = 14.sp)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(EduviaColors.Primary).clickable {
                        onPublish(title, date, time, place, link, reminder)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (meetingToEdit != null) "Guardar cambios" else "Publicar junta", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val borderColor = if (isError) Color(0xFFDC2626) else EduviaColors.BorderStrong
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isError) Color(0xFFDC2626) else EduviaColors.TextSecondary)
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(EduviaColors.Surface)
                .border(if (isError) 1.5.dp else 0.8.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value, onValueChange = onValueChange, singleLine = true,
                textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary),
                cursorBrush = SolidColor(EduviaColors.Primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) Text(placeholder, fontSize = 15.sp, color = EduviaColors.TextSecondary)
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun ReminderSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(EduviaColors.Surface).border(0.8.dp, EduviaColors.Border, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Recordar un día antes", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("Se envía una notificación a los padres", fontSize = 12.sp, color = EduviaColors.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EduviaColors.Primary, uncheckedThumbColor = Color.White, uncheckedTrackColor = EduviaColors.Border))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CreateMeetingScreenPreview() {
    CreateMeetingScreen()
}
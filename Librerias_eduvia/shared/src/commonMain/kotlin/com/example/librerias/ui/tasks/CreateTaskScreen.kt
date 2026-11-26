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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.model.SchoolTask
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

/** Flujo del maestro para publicar una tarea nueva o editar una existente. */
@Composable
fun CreateTaskScreen(
    modifier: Modifier = Modifier,
    taskToEdit: SchoolTask? = null,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onClose: () -> Unit = {},
    onPublish: (title: String, subject: String, description: String, date: String) -> Unit = { _, _, _, _ -> }
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var selectedSubject by remember { mutableStateOf(taskToEdit?.subject?.label ?: "Matemáticas") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var date by remember { mutableStateOf(taskToEdit?.dueDate?.let { "${it.day}/${it.month}/${it.year}" } ?: "") }

    val titleError = errorMessage?.startsWith("Escribe el título") == true
    val dateError = errorMessage?.startsWith("La fecha") == true

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(Modifier.fillMaxSize().background(EduviaColors.Background).then(modifier)) {

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, "Cerrar", tint = EduviaColors.TextPrimary, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = if (taskToEdit != null) "Editar tarea" else "Nueva tarea",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // CONTENIDO
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TaskFormField(label = "Título", value = title, onValueChange = { title = it; onClearError() }, isError = titleError)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Materia", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = EduviaColors.TextSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubjectChip("Matemáticas", selectedSubject == "Matemáticas") { selectedSubject = "Matemáticas" }
                            SubjectChip("Español", selectedSubject == "Español") { selectedSubject = "Español" }
                            SubjectChip("Ciencias", selectedSubject == "Ciencias") { selectedSubject = "Ciencias" }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubjectChip("Historia", selectedSubject == "Historia") { selectedSubject = "Historia" }
                            SubjectChip("Otra", selectedSubject == "Otra") { selectedSubject = "Otra" }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Descripción", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = EduviaColors.TextSecondary)
                    Box(modifier = Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(12.dp)).background(EduviaColors.Surface).border(0.8.dp, EduviaColors.BorderStrong, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        BasicTextField(value = description, onValueChange = { description = it }, textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary), cursorBrush = SolidColor(EduviaColors.Primary), modifier = Modifier.fillMaxSize())
                    }
                }

                TaskFormField(label = "Fecha de entrega (DD/MM/AAAA)", value = date, onValueChange = { date = it; onClearError() }, isError = dateError)
                Spacer(Modifier.height(16.dp))
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
                    modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(EduviaColors.Primary).clickable { onPublish(title, selectedSubject, description, date) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (taskToEdit != null) "Guardar cambios" else "Publicar tarea", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TaskFormField(label: String, value: String, onValueChange: (String) -> Unit, isError: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isError) Color(0xFFDC2626) else EduviaColors.TextSecondary)
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(EduviaColors.Surface)
                .border(if (isError) 1.5.dp else 0.8.dp, if (isError) Color(0xFFDC2626) else EduviaColors.BorderStrong, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(value = value, onValueChange = onValueChange, singleLine = true, textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary), cursorBrush = SolidColor(EduviaColors.Primary))
        }
    }
}
@Composable
private fun SubjectChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (isSelected) EduviaColors.Primary else EduviaColors.Surface).then(if (isSelected) Modifier else Modifier.border(0.8.dp, EduviaColors.BorderStrong, RoundedCornerShape(20.dp))).clickable(onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = if (isSelected) Color.White else EduviaColors.TextPrimary, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CreateTaskPreview() {
    CreateTaskScreen()
}
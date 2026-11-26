package com.example.librerias.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.librerias.model.SchoolTask
import com.example.librerias.model.monthNames
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

private val TagSubjectBg = Color(0xFFDBEAFE)
private val TagSubjectText = Color(0xFF1E3A8A)
private val TagDateBg = Color(0xFFFEE2E2)
private val TagDateText = Color(0xFF7F1D1D)
private val ButtonGreen = Color(0xFF16A34A)

/** Detalle de una tarea para el padre/alumno, con la acción de marcarla como entregada. */
@Composable
fun TaskDetailScreen(
    task: SchoolTask? = null,
    studentId: Long = 1L, // 👈 NUEVO PARÁMETRO
    onBack: () -> Unit = {},
    onMarkAsDelivered: () -> Unit = {}
) {
    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(Modifier.fillMaxSize().background(EduviaColors.Background)) {

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 8.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        "Regresar",
                        tint = EduviaColors.TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(text = "Tarea", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            // CONTENIDO
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tag(
                        text = task?.subject?.label ?: "Materia",
                        bg = TagSubjectBg,
                        textColor = TagSubjectText
                    )
                    Tag(text = "Vence pronto", bg = TagDateBg, textColor = TagDateText)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        task?.title ?: "Sin título",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Publicada por la maestra Laura · ${task?.publishedDate?.day ?: 9} de ${monthNames[(task?.publishedDate?.month ?: 10) - 1].lowercase()}",
                        fontSize = 14.sp,
                        color = EduviaColors.TextSecondary
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().shadow(
                        1.dp,
                        RoundedCornerShape(12.dp),
                        ambientColor = EduviaColors.Shadow,
                        spotColor = EduviaColors.Shadow
                    ).background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CalendarToday,
                                null,
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Fecha de entrega",
                                fontSize = 12.sp,
                                color = EduviaColors.TextSecondary
                            )
                            Text(
                                "${task?.dueDate?.day ?: 12} de ${monthNames[(task?.dueDate?.month ?: 10) - 1].lowercase()}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(0.8.dp).background(EduviaColors.Border))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Descripción", fontSize = 12.sp, color = EduviaColors.TextSecondary)
                        Text(
                            task?.description ?: "Sin descripción",
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Foto de la maestra",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EduviaColors.TextSecondary
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().height(132.dp)
                            .clip(RoundedCornerShape(12.dp)).background(Color(0xFFE2E8F0))
                            .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Image,
                                null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(28.dp)
                            )
                            Text("Foto del pizarrón", fontSize = 13.sp, color = Color(0xFF64748B))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // BOTÓN MARCAR
            Column(
                modifier = Modifier.fillMaxWidth().background(EduviaColors.Background)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 👇 AQUÍ USAMOS EL studentId REAL DEL HIJO
                val alreadyDelivered = task?.isDeliveredBy(studentId) == true

                if (alreadyDelivered) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFFDCFCE7)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Check,
                            null,
                            tint = Color(0xFF166534),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Tarea entregada",
                            color = Color(0xFF166534),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(ButtonGreen)
                            .clickable(onClick = onMarkAsDelivered),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Marcar como entregada",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "Se avisará a la maestra cuando la marques",
                        fontSize = 12.sp,
                        color = EduviaColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, bg: Color, textColor: Color) {
    Box(modifier = Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
fun TaskDetailPreview() {
    TaskDetailScreen()
}
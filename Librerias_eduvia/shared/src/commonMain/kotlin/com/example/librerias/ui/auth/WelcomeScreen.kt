package com.example.librerias.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.ui.theme.poppinsFamily

/**
 * Pantalla de bienvenida que presenta Eduvia y abre el flujo de acceso a la aplicación.
 */
@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit = {}
) {
    val backgroundColor = Color(0xFF0F172A)

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // 👇 AQUÍ ESTÁ EL LOGO DIBUJADO CON CANVAS
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xFF101B2B)), // Fondo oscuro de la imagen
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val azul = Color(0xFF60A5FA)
                    val stroke = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)

                    // 1. Dibujar el calendario
                    val ancho = size.width
                    val alto = size.height

                    // Contorno del calendario
                    drawRoundRect(
                        color = azul,
                        topLeft = Offset(0f, alto * 0.15f),
                        size = Size(ancho, alto * 0.85f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        style = stroke
                    )

                    // Línea horizontal superior del calendario
                    drawLine(
                        color = azul,
                        start = Offset(0f, alto * 0.40f),
                        end = Offset(ancho, alto * 0.40f),
                        strokeWidth = 8.dp.toPx()
                    )

                    // Argollas del calendario (arriba)
                    drawLine(color = azul, start = Offset(ancho * 0.25f, 0f), end = Offset(ancho * 0.25f, alto * 0.25f), strokeWidth = 8.dp.toPx())
                    drawLine(color = azul, start = Offset(ancho * 0.75f, 0f), end = Offset(ancho * 0.75f, alto * 0.25f), strokeWidth = 8.dp.toPx())

                    // 2. Dibujar el Check
                    val pathCheck = Path().apply {
                        moveTo(ancho * 0.5f, alto * 0.85f)
                        lineTo(ancho * 0.65f, alto * 1.0f)
                        lineTo(ancho * 1.1f, alto * 0.6f)
                    }

                    // Fondo para tapar la línea del calendario y que el check resalte
                    drawRect(
                        color = Color(0xFF101B2B),
                        topLeft = Offset(ancho * 0.35f, alto * 0.65f),
                        size = Size(ancho * 0.75f, alto * 0.45f)
                    )

                    // Trazar el check
                    drawPath(
                        path = pathCheck,
                        color = azul,
                        style = stroke
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Bienvenido a EDUVIA",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "La mejor forma de gestionar la escuela.",
                fontSize = 14.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.weight(1f))

            // BOTÓN DE COMENZAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1E36E0), Color(0xFF191680))
                        )
                    )
                    .clickable(onClick = onStartClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Iniciar sesión",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview
@Composable
fun WelcomePreview() {
    WelcomeScreen()
}
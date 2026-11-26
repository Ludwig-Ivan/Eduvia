package com.example.librerias

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import librerias.shared.generated.resources.Res
import librerias.shared.generated.resources.logo
import librerias.shared.generated.resources.carro
import moe.tlaster.precompose.PreComposeApp

/**
 * Composable raíz de la demostración visual: aplica `AppTheme`, envuelve el contenido en
 * `PreComposeApp` y muestra el logo junto con una tarjeta de automóvil de ejemplo.
 *
 * No conecta con `AppNavHost` ni con ViewModels; es la pantalla de muestra del proyecto.
 */
@Composable
@Preview
fun App() {
    // AppTheme ya contiene el MaterialTheme, no hace falta otro dentro
    AppTheme {
        // Colores de tu archivo del tema (Theme.kt)
        val colors = getColorTheme()

        PreComposeApp {
            Column(
                modifier = Modifier
                    .background(colors.backgroundColor)
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(100.dp))

                AutoCard(
                    imagen = Res.drawable.carro,
                    marca = "Mercedes-Benz",
                    modelo = "CLA",
                    anio = "2022",
                    precio = "$650,000"
                )
            }
        }
    }
}

/** Tarjeta de ejemplo de la demo visual: muestra imagen, marca, modelo, año y precio sin ViewModel. */
@Composable
fun AutoCard(
    imagen: DrawableResource,
    marca: String,
    modelo: String,
    anio: String,
    precio: String
) {
    val colors = getColorTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.colorExpenseItem
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(imagen),
                    contentDescription = "$marca $modelo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Modelo: $modelo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textColor
                    )
                    Text(text = "Marca: $marca", fontSize = 18.sp, color = colors.textColor.copy(alpha = 0.7f))
                    Text(text = "Año: $anio", fontSize = 18.sp, color = colors.textColor.copy(alpha = 0.7f))
                    Text(text = "Precio: $precio", fontSize = 18.sp, color = colors.textColor.copy(alpha = 0.7f))
                }
            }

            TextButton(
                onClick = { },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Ver más", color = colors.purple)
            }
        }
    }
}
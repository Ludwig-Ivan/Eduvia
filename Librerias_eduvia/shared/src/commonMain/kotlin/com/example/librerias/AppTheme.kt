package com.example.librerias

import androidx.compose.foundation.shape.AbsoluteCutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tema Material3 de la app: fija el color primario y las formas con esquinas cortadas.
 * Envuelve el contenido raíz; `getColorTheme` complementa la paleta usada por los composables.
 */
@Composable
fun AppTheme(content: @Composable ()->Unit ){
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(primary = Color.Black),
        shapes = MaterialTheme.shapes.copy(
            small = AbsoluteCutCornerShape(size = 0.dp),
            medium = AbsoluteCutCornerShape(size = 0.dp),
            large = AbsoluteCutCornerShape(size = 0.dp)
        )
    ){
        content()
    }
}

/** Paleta de colores usada por los composables raíz; actualmente resuelve el modo claro. */
@Composable
fun getColorTheme(): DarkModeColors{
    val isDarkMode = false

    val Puple = Color(0xff6a66ff)
    val ColorExpenseItem = if(isDarkMode) Color(0xff090808) else Color(0xfff1f1f1)
    val BackgrounColor = if (isDarkMode) Color(0xff1e1c1c) else Color.White
    val TextColor = if (isDarkMode) Color.White else Color.Black
    val IconColor = if (isDarkMode) Puple else Color.Black
    val ColorArrowRound = if (isDarkMode) Puple else Color.Gray.copy(alpha = 0.2f)

    return DarkModeColors(
        purple = Puple,
        colorExpenseItem = ColorExpenseItem,
        backgroundColor = BackgrounColor,
        textColor = TextColor,
        iconColor = IconColor,
        colorArrowRound = ColorArrowRound
    )
}

/** Colores que `getColorTheme` entrega a los composables de la app. */
data class DarkModeColors(
    val purple : Color,
    val colorExpenseItem : Color,
    val backgroundColor : Color,
    val textColor : Color,
    val iconColor : Color,
    val colorArrowRound : Color
)
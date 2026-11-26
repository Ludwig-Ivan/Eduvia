package com.example.librerias.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Colores sacados del HTML/Figma. */
object EduviaColors {
    val Background = Color(0xFFF8FAFC)
    val Surface = Color.White
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF596579)
    val Border = Color(0xFFE2E8F0)
    val BorderStrong = Color(0xFFCBD5E1)
    val Shadow = Color(0x140F172A) // rgba(15,23,42,0.08)

    val Primary = Color(0xFF2563EB)
    val PrimaryDark = Color(0xFF1D4ED8)
    val PrimarySoft = Color(0xFFDBEAFE)

    val Task = Color(0xFF3B82F6)
    val Meeting = Color(0xFFF59E0B)
    val MeetingSoft = Color(0xFFFEF3C7)
    val MeetingDark = Color(0xFF92400E)
    val Exam = Color(0xFF8B5CF6)
    val ExamSoft = Color(0xFFEDE9FE)
    val ExamDark = Color(0xFF5B21B6)

    val IconMuted = Color(0xFF334155)
    val Success = Color(0xFF16A34A)
}

/**
 * Tipografía del diseño: Poppins.
 *
 * Para activarla:
 *  1. Copia poppins_regular.ttf, poppins_medium.ttf, poppins_semibold.ttf y poppins_bold.ttf
 *     a  shared/src/commonMain/composeResources/font/   (nombres en minúsculas y con guion bajo).
 *  2. Descomenta el bloque de abajo y estos imports:
 *       import androidx.compose.ui.text.font.FontWeight
 *       import librerias.shared.generated.resources.Res
 *       import librerias.shared.generated.resources.poppins_regular
 *       import librerias.shared.generated.resources.poppins_medium
 *       import librerias.shared.generated.resources.poppins_semibold
 *       import librerias.shared.generated.resources.poppins_bold
 *       import org.jetbrains.compose.resources.Font
 *  3. Haz Build/Sync para que se genere Res.font.*
 */
@Composable
fun poppinsFamily(): FontFamily {
    /*
    return FontFamily(
        Font(Res.font.poppins_regular, FontWeight.Normal),
        Font(Res.font.poppins_medium, FontWeight.Medium),
        Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        Font(Res.font.poppins_bold, FontWeight.Bold),
    )
    */
    return FontFamily.Default
}
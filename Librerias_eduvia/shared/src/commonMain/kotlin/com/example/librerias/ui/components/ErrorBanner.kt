package com.example.librerias.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.model.AppError
import com.example.librerias.model.AppErrorKind

/**
 * Aviso no intrusivo para fallos de red o de acciones.
 *
 * Muestra siempre el mensaje amigable de [AppError]. Solo ofrece «Reintentar» cuando el error lo
 * permite, y «Entendido» para cerrarlo sin perder el contenido ya cargado.
 */
@Composable
fun ErrorBanner(
    error: AppError?,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    if (error == null) return

    val title = when (error.kind) {
        AppErrorKind.CONNECTION, AppErrorKind.TIMEOUT, AppErrorKind.SERVER -> "Sin conexión"
        AppErrorKind.SESSION -> "Sesión"
        else -> "No se pudo completar"
    }

    Surface(
        color = Color(0xFFFFF4E5),
        contentColor = Color(0xFF92400E),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(error.message, fontSize = 13.sp, lineHeight = 17.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (error.retryable && onRetry != null) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB45309),
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.height(34.dp),
                    ) { Text("Reintentar", fontSize = 12.sp) }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) { Text("Entendido", fontSize = 12.sp) }
            }
        }
    }
}

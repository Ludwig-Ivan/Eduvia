package com.example.librerias.ui.auth

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily
import androidx.compose.material.icons.automirrored.outlined.ArrowForward

/** Flujo de acceso: introduce correo/contraseña, muestra errores y bloqueo temporal y enlaza registro y recuperación. */
@Composable
fun LoginScreen(
    errorMessage: String? = null,
    isLoading: Boolean = false,
    lockSeconds: Int = 0,
    onClearError: () -> Unit = {},
    onLogin: (email: String, password: String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Qué campos marcar en rojo según el mensaje que devolvió el caso de uso
    val bothError = errorMessage?.startsWith("Correo o contraseña") == true
    val emailError = bothError || errorMessage?.startsWith("Escribe un correo") == true
    val passError = bothError || errorMessage?.startsWith("Escribe tu contraseña") == true

    val canLogin = !isLoading && lockSeconds == 0
    val buttonText = when {
        lockSeconds > 0 -> "Espera ${lockSeconds}s"
        isLoading -> "Entrando…"
        else -> "Iniciar sesión"
    }

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            // HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(EduviaColors.Primary, EduviaColors.PrimaryDark)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 20.dp, top = 40.dp, bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(text = "Iniciar sesión", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Qué bueno verte de nuevo.\nContinúa con tus tareas, juntas y avisos.",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 19.5.sp
                        )
                    }
                }
            }

            // FORMULARIO
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    AuthField(
                        label = "Correo electrónico",
                        value = email,
                        onValueChange = { email = it; onClearError() },
                        placeholder = "nombre@correo.com",
                        keyboardType = KeyboardType.Email,
                        isError = emailError
                    )

                    // Contraseña
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Contraseña", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = if (passError) Color(0xFFDC2626) else EduviaColors.TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF6F7FA))
                                .border(
                                    if (passError) 1.5.dp else 0.8.dp,
                                    if (passError) Color(0xFFDC2626) else Color(0xFFD1D5DB),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(start = 14.dp, end = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = password,
                                onValueChange = { password = it; onClearError() },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary),
                                cursorBrush = SolidColor(EduviaColors.Primary),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                decorationBox = { innerTextField ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f)) {
                                            if (password.isEmpty()) {
                                                Text("Tu contraseña", fontSize = 15.sp, color = EduviaColors.TextSecondary)
                                            }
                                            innerTextField()
                                        }
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(CircleShape).clickable { passwordVisible = !passwordVisible },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = "Mostrar contraseña",
                                                tint = Color(0xFF4B5563),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Recordarme y Olvidé contraseña
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(EduviaColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            Text("Recordarme", fontSize = 13.sp, color = Color(0xFF374151))
                        }
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = EduviaColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onForgotPasswordClick)
                        )
                    }

                    // Mensaje de error
                    if (errorMessage != null) {
                        Box(Modifier.fillMaxWidth().background(Color(0xFFFFE4E4), RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text(errorMessage, color = Color(0xFFB91C1C), fontSize = 14.sp)
                        }
                    }

                    // Botón Iniciar sesión
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = if (canLogin) Brush.horizontalGradient(
                                    colors = listOf(EduviaColors.Primary, EduviaColors.PrimaryDark)
                                ) else Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF9CA3AF), Color(0xFF9CA3AF))
                                )
                            )
                            .clickable(enabled = canLogin) { onLogin(email, password) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(buttonText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            if (canLogin) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Texto de registro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("¿No tienes una cuenta? ", fontSize = 14.sp, color = Color(0xFF4B5563))
                        Text(
                            text = "Regístrate",
                            color = EduviaColors.Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onRegisterClick)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = if (isError) Color(0xFFDC2626) else EduviaColors.TextPrimary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF6F7FA))
                .border(
                    if (isError) 1.5.dp else 0.8.dp,
                    if (isError) Color(0xFFDC2626) else Color(0xFFD1D5DB),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary),
                cursorBrush = SolidColor(EduviaColors.Primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, color = EduviaColors.TextSecondary)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun LoginPreview() {
    LoginScreen()
}
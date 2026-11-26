package com.example.librerias.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.librerias.di.AppContainer
import kotlinx.coroutines.delay

/**
 * Flujo de recuperación de contraseña: permite solicitar un código, verificarlo y
 * establecer una contraseña nueva antes de volver al inicio de sesión.
 */
@Composable
fun ForgotPasswordScreen(
    container: AppContainer,
    onBack: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val viewModel: ForgotPasswordViewModel = viewModel { ForgotPasswordViewModel(container) }
    val state by viewModel.state.collectAsState()
    val inbox by viewModel.demoInbox.collectAsState()

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    // Al abrir: limpia el estado de una visita anterior
    LaunchedEffect(Unit) { viewModel.reset() }
    // Al terminar: muestra el mensaje 1.5 s y regresa al login
    LaunchedEffect(state.done) {
        if (state.done) { delay(1500); onDone() }
    }

    val canSend = !state.isSending && state.cooldown == 0
    val sendLabel = when {
        state.isSending -> "Enviando…"
        state.cooldown > 0 -> "Reenviar en ${state.cooldown}s"
        else -> "Enviar código"
    }

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Column(Modifier.fillMaxSize().background(Color.White)) {

            // ---------- HEADER ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(brush = Brush.linearGradient(colors = listOf(EduviaColors.Primary, EduviaColors.PrimaryDark)))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 20.dp, top = 40.dp, bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text("Recuperar contraseña", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Te enviaremos un código a tu correo para que crees una nueva contraseña.",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 19.5.sp
                        )
                    }
                }
            }

            // ---------- FORMULARIO ----------
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
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Correo + botón enviar código
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Correo electrónico", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF6F7FA))
                                    .border(0.8.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary),
                                    cursorBrush = SolidColor(EduviaColors.Primary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    decorationBox = { innerTextField ->
                                        if (email.isEmpty()) {
                                            Text("nombre@correo.com", fontSize = 15.sp, color = EduviaColors.TextSecondary)
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, if (canSend) EduviaColors.Primary else Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                                    .clickable(enabled = canSend) { viewModel.sendCode(email) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    sendLabel,
                                    color = if (canSend) EduviaColors.Primary else Color(0xFF9CA3AF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            "El código vence en 10 minutos. Revisa también spam.",
                            fontSize = 12.sp, color = Color(0xFF4B5563)
                        )
                    }

                    // Aviso de código enviado
                    state.info?.let { msg ->
                        Box(Modifier.fillMaxWidth().background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text(msg, color = Color(0xFF1E40AF), fontSize = 13.sp)
                        }
                    }

                    // SOLO DEMO: aquí aparece el "correo" porque no hay servidor
                    inbox?.let { mail ->
                        Box(Modifier.fillMaxWidth().background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📩 Correo simulado (solo demo)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                Text(mail, fontSize = 13.sp, color = Color(0xFF78350F))
                            }
                        }
                    }

                    // Código de 6 dígitos
                    AuthField(
                        label = "Código de 6 dígitos",
                        value = code,
                        onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                        placeholder = "000000",
                        keyboardType = KeyboardType.Number
                    )

                    PasswordField(
                        label = "Nueva contraseña",
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = "Mínimo 8 caracteres",
                        visible = passVisible,
                        onToggleVisibility = { passVisible = !passVisible }
                    )
                    Text("Mínimo 8 caracteres, una letra y un número.", fontSize = 12.sp, color = Color(0xFF4B5563))

                    PasswordField(
                        label = "Confirmar contraseña",
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Repite la contraseña",
                        visible = confirmVisible,
                        onToggleVisibility = { confirmVisible = !confirmVisible }
                    )

                    // Error
                    state.error?.let { msg ->
                        Box(Modifier.fillMaxWidth().background(Color(0xFFFFE4E4), RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text(msg, color = Color.Red, fontSize = 14.sp)
                        }
                    }

                    // Éxito
                    if (state.done) {
                        Box(Modifier.fillMaxWidth().background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text("¡Contraseña actualizada! Ya puedes iniciar sesión.", color = Color(0xFF166534), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Botón Restablecer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.horizontalGradient(colors = listOf(EduviaColors.Primary, EduviaColors.PrimaryDark))
                            )
                            .clickable(enabled = !state.isResetting && !state.done) {
                                viewModel.resetPassword(email, code, newPassword, confirmPassword)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.isResetting) "Guardando…" else "Restablecer contraseña",
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("¿Ya la recuerdas? ", fontSize = 14.sp, color = Color(0xFF4B5563))
                        Text(
                            "Inicia sesión",
                            color = EduviaColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onLoginClick)
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF6F7FA))
                .border(0.8.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
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

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF6F7FA))
                .border(0.8.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                .padding(start = 14.dp, end = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary),
                cursorBrush = SolidColor(EduviaColors.Primary),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(placeholder, fontSize = 15.sp, color = EduviaColors.TextSecondary)
                            }
                            innerTextField()
                        }
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onToggleVisibility),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
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
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordPreview() {
    ForgotPasswordScreen(container = AppContainer())
}
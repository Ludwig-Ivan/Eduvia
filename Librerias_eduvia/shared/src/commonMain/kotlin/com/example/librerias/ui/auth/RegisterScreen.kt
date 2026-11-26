package com.example.librerias.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.librerias.di.AppContainer
import com.example.librerias.model.ChildInfo
import com.example.librerias.model.RegistrationResult // 👈 NUEVO
import com.example.librerias.model.UserRole
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.HeaderGradient
import com.example.librerias.ui.theme.poppinsFamily

data class TeacherGroup(
    val groupName: String = "",
    val schoolName: String = "",
    val subject: String = ""
)

/** Flujo de alta: valida los datos y registra al usuario como padre (con hijos) o maestro (con grupos). */
@Composable
fun RegisterScreen(
    container: AppContainer,
    onBack: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onSuccessParent: () -> Unit = {},
    onSuccessTeacher: (RegistrationResult) -> Unit = {}
) {
    val viewModel: RegisterViewModel = viewModel { RegisterViewModel(container) }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // (a) Al abrir la pantalla, limpia errores de una visita anterior
    LaunchedEffect(Unit) { viewModel.clearError() }

    var currentStep by remember { mutableStateOf(1) }
    var role by remember { mutableStateOf(UserRole.PARENT) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }

    // Listas para hijos o grupos
    val children = remember { mutableStateListOf(ChildInfo()) }
    val groups = remember { mutableStateListOf(TeacherGroup()) }

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Box(Modifier.fillMaxSize().background(EduviaColors.Background).imePadding()) {

            // HEADER
            Column(
                modifier = Modifier.fillMaxWidth().background(HeaderGradient).padding(start = 8.dp, end = 20.dp, top = 40.dp, bottom = 40.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable {
                        viewModel.clearError()
                        if (currentStep == 1) onBack() else currentStep = 1
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Atrás", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.padding(start = 12.dp, top = 12.dp)) {
                    Text("Regístrate", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (currentStep == 1) "Paso 1: Crea tu cuenta" else if (role == UserRole.PARENT) "Paso 2: Agrega a tu hijo" else "Paso 2: Crea tu grupo",
                        color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                    )
                }
            }

            // TARJETA
            Column(
                modifier = Modifier.fillMaxSize().offset(y = 175.dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp).padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep == 1) {
                    RoleSelector(selected = role, onSelect = { role = it })
                    FormField("Nombre completo", fullName, { fullName = it }, "Nombre y apellidos")
                    FormField("Correo electrónico", email, { email = it }, "nombre@correo.com", keyboardType = KeyboardType.Email)
                    FormField("Número de celular", phone, { phone = it.filter { c -> c.isDigit() }.take(10) }, "10 dígitos", keyboardType = KeyboardType.Phone)
                    PasswordField("Crear contraseña", password, { password = it }, "Mínimo 8 caracteres")
                    PasswordField("Confirmar contraseña", confirmPassword, { confirmPassword = it }, "Repite la contraseña")
                } else {
                    if (role == UserRole.PARENT) {
                        ChildrenSection(
                            children = children,
                            onAdd = { children.add(ChildInfo()) },
                            onUpdate = { index, updated -> children[index] = updated },
                            onRemove = { index -> children.removeAt(index) }
                        )
                    } else {
                        TeacherGroupsSection(
                            groups = groups,
                            onAdd = { groups.add(TeacherGroup()) },
                            onUpdate = { index, updated -> groups[index] = updated },
                            onRemove = { index -> groups.removeAt(index) }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it }, colors = CheckboxDefaults.colors(checkedColor = EduviaColors.Primary, uncheckedColor = EduviaColors.BorderStrong))
                        Text("Acepto los Términos de Servicio y la Política de Privacidad.", fontSize = 13.sp, color = EduviaColors.TextSecondary, modifier = Modifier.padding(top = 12.dp))
                    }
                }

                // (c) El error se muestra en AMBOS pasos
                if (error != null) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFE4E4), RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text(text = error ?: "", color = Color.Red, fontSize = 14.sp)
                    }
                }
            }

            // BOTÓN FIJO
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp)) {
                GradientButton(
                    text = if (isLoading) "Cargando..." else if (currentStep == 1) "Siguiente" else "Crear cuenta",
                    trailingIcon = true,
                    onClick = {
                        if (!isLoading) {
                            if (currentStep == 1) {
                                // (b) Valida formato + correo y celular repetidos
                                viewModel.validateStep1(fullName, email, phone, password, confirmPassword) {
                                    currentStep = 2
                                }
                            } else {
                                if (role == UserRole.PARENT) {
                                    viewModel.registerParent(
                                        fullName, email, phone, password, confirmPassword,
                                        children.toList(), acceptedTerms, onSuccessParent
                                    )
                                } else {
                                    viewModel.registerTeacher(
                                        fullName, email, phone, password, confirmPassword,
                                        groups.toList(), acceptedTerms, onSuccessTeacher
                                    )
                                }
                            }
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("¿Ya tienes cuenta? ", fontSize = 14.sp, color = EduviaColors.TextSecondary)
                    Text("Inicia sesión", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduviaColors.Primary, modifier = Modifier.clickable(onClick = onLoginClick))
                }
            }
        }
    }
}

// ===========================================================================
// COMPONENTES LOCALES (sin cambios)
// ===========================================================================

@Composable
private fun RoleSelector(selected: UserRole, onSelect: (UserRole) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Soy…", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFEEF0F6), RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RoleChip("Padre o tutor", selected == UserRole.PARENT, Modifier.weight(1f)) { onSelect(UserRole.PARENT) }
            RoleChip("Maestro/a", selected == UserRole.TEACHER, Modifier.weight(1f)) { onSelect(UserRole.TEACHER) }
        }
    }
}

@Composable
private fun RoleChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgBrush: Brush = if (selected) HeaderGradient else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    Box(modifier = modifier.height(40.dp).clip(RoundedCornerShape(8.dp)).background(bgBrush).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Color.White else Color(0xFF374151))
    }
}

@Composable
private fun ChildrenSection(children: List<ChildInfo>, onAdd: () -> Unit, onUpdate: (Int, ChildInfo) -> Unit, onRemove: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("Tus hijos", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Escribe el código de la clase que te dio el maestro y el nombre del alumno.", fontSize = 12.sp, color = EduviaColors.TextSecondary)
        }
        for (index in children.indices) {
            val child = children[index]
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFC), RoundedCornerShape(12.dp)).border(0.8.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Hijo/a ${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduviaColors.Primary)
                    if (children.size > 1) Text("Eliminar", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRemove(index) })
                }
                FormField("Código de la clase", child.classCode, { onUpdate(index, child.copy(classCode = it)) }, "Ej. 3B-7K2P")
                FormField("Nombre del alumno", child.studentName, { onUpdate(index, child.copy(studentName = it)) }, "Nombre de tu hijo/a")
            }
        }
        AddButton("Agregar otro hijo", onAdd)
    }
}

@Composable
private fun TeacherGroupsSection(groups: List<TeacherGroup>, onAdd: () -> Unit, onUpdate: (Int, TeacherGroup) -> Unit, onRemove: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("Crea tu grupo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Podrás agregar más grupos después desde tu perfil.", fontSize = 12.sp, color = EduviaColors.TextSecondary)
        }
        for (index in groups.indices) {
            val group = groups[index]
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFC), RoundedCornerShape(12.dp)).border(0.8.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Grupo ${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduviaColors.Primary)
                    if (groups.size > 1) Text("Eliminar", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRemove(index) })
                }
                FormField("Nombre del grupo", group.groupName, { onUpdate(index, group.copy(groupName = it)) }, "Ej. 3° B")
                FormField("Escuela", group.schoolName, { onUpdate(index, group.copy(schoolName = it)) }, "Nombre de la escuela")
            }
        }

        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEF2FF)).border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(8.dp)).padding(14.dp)) {
            Text("Al crear tu cuenta se generará un código de grupo para que lo compartas con los padres.", fontSize = 13.sp, color = Color(0xFF1E36E0), fontWeight = FontWeight.Medium, lineHeight = 18.sp)
        }

        AddButton("Agregar otro grupo", onAdd)
    }
}

@Composable
fun FormField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, keyboardType: KeyboardType = KeyboardType.Text) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Box(modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF6F7FA)).border(0.8.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(value = value, onValueChange = onValueChange, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary), cursorBrush = SolidColor(EduviaColors.Primary), decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, fontSize = 15.sp, color = EduviaColors.TextSecondary)
                inner()
            })
        }
    }
}

@Composable
fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    var visible by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF6F7FA)).border(0.8.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp)).padding(start = 14.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(value = value, onValueChange = onValueChange, singleLine = true, modifier = Modifier.weight(1f), visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), textStyle = TextStyle(fontFamily = poppinsFamily(), fontSize = 15.sp, color = EduviaColors.TextPrimary), cursorBrush = SolidColor(EduviaColors.Primary), decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, fontSize = 15.sp, color = EduviaColors.TextSecondary)
                inner()
            })
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).clickable { visible = !visible }, contentAlignment = Alignment.Center) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Color(0xFF4B5563), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddButton(text: String, onClick: () -> Unit) {
    Row(modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.Add, null, tint = EduviaColors.Primary, modifier = Modifier.size(18.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduviaColors.Primary)
    }
}

@Composable
fun GradientButton(text: String, trailingIcon: Boolean = false, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(10.dp)).background(HeaderGradient).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (trailingIcon) {
            Spacer(Modifier.size(8.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Preview(heightDp = 1000)
@Composable
private fun RegisterScreenPreview() {
    RegisterScreen(container = AppContainer())
}
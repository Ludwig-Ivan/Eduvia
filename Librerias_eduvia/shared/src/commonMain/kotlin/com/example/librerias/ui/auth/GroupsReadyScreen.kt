package com.example.librerias.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.librerias.model.RegistrationResult
import com.example.librerias.ui.theme.EduviaColors
import com.example.librerias.ui.theme.poppinsFamily

private val HeaderGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF1E36E0), Color(0xFF2A4CF0), Color(0xFF1E2BC8), Color(0xFF191680))
)

/**
 * Información que muestra la pantalla de confirmación del alta de grupos:
 * nombre del grupo y código de clase para compartirlo con las familias.
 */
data class GroupCodeInfo(val groupName: String, val code: String)

@Composable
fun GroupsReadyScreen(
    result: RegistrationResult? = null,
    modifier: Modifier = Modifier,
    onGoHome: () -> Unit = {}
) {
    // 👇 Convertimos los grupos del resultado a la lista que usa la UI
    val groupsUi: List<GroupCodeInfo> = result?.createdGroups
        ?.map { GroupCodeInfo(groupName = "Grupo ${it.name}", code = it.code) }
        ?: listOf(GroupCodeInfo("Grupo 3° B", "3B-7K2P")) // fallback de preview

    ProvideTextStyle(TextStyle(fontFamily = poppinsFamily(), color = EduviaColors.TextPrimary)) {
        Box(
            modifier = Modifier.fillMaxSize().background(EduviaColors.Background).then(modifier)
        ) {
            // HEADER
            Column(
                modifier = Modifier.fillMaxWidth().background(HeaderGradient).padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 60.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Check, "Listo", tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (groupsUi.size > 1) "Tus grupos están listos" else "Tu grupo está listo",
                    color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Comparte cada código con los padres del grupo correspondiente.",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.5.sp
                )
            }

            // TARJETA
            Column(
                modifier = Modifier.fillMaxSize().offset(y = 195.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    groupsUi.forEach { group -> GroupCard(group = group) }
                }

                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cómo se unen los padres", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    StepItem("1", "Descargan la app y se registran como Padre o tutor.")
                    StepItem("2", "Escriben el código de su grupo en Código de la clase junto con el nombre de su hijo/a.")
                    StepItem("3", "Ya ven las tareas y juntas de tu grupo.")
                }

                Text(
                    "Puedes ver tus códigos y crear más grupos en Perfil › Mis grupos.",
                    fontSize = 12.sp, color = EduviaColors.TextSecondary, modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp))
                        .background(HeaderGradient).clickable(onClick = onGoHome),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                ) {
                    Text("Ir al inicio", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun GroupCard(group: GroupCodeInfo) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF6F7FA))
            .border(0.8.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(group.groupName, fontSize = 13.sp, color = Color(0xFF4B5563))
            Text(group.code, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = EduviaColors.Primary, letterSpacing = 3.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White)
                    .border(0.8.dp, EduviaColors.Primary, RoundedCornerShape(10.dp)).clickable { /* TODO copiar */ },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.ContentCopy, "Copiar código", tint = EduviaColors.Primary, modifier = Modifier.size(20.dp)) }

            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White)
                    .border(0.8.dp, EduviaColors.Primary, RoundedCornerShape(10.dp)).clickable { /* TODO compartir */ },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Share, "Compartir código", tint = EduviaColors.Primary, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun StepItem(number: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFDBEAFE)), contentAlignment = Alignment.Center) {
            Text(number, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EduviaColors.Primary)
        }
        Text(text, fontSize = 14.sp, color = Color(0xFF374151), lineHeight = 21.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun GroupsReadyScreenPreview() {
    GroupsReadyScreen()
}
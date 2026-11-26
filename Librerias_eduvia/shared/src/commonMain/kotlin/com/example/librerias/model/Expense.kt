package com.example.librerias.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.PartyMode
import androidx.compose.material.icons.filled.ViewCozy
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Gasto mostrado únicamente en la pantalla de gastos (`ui/ExpenceScreen.kt`).
 *
 * No tiene repositorio, caso de uso ni endpoint: no se persiste ni existe en la API
 * (OPEN-QUESTIONS Q1). `icon` se deriva de `category` solo para dibujar la pantalla.
 */
data class Expense(
    val id: Long = -1,
    val amount: Double,
    val category: expenseCategory,
    val description: String
){
    val icon = category.icon
}

/** Categorías de la pantalla de gastos; no existen en la API ni se persisten (OPEN-QUESTIONS Q1). */
enum class expenseCategory(val icon: ImageVector){
    GROCERIES (Icons.Default.FoodBank),
    PARTY (Icons.Default.PartyMode),
    SNACKS (Icons.Default.Fastfood),
    COFFEE (Icons.Default.Coffee),
    CAR (Icons.Default.ElectricCar),
    HOUSE (Icons.Default.House),
    OTHER (Icons.Default.ViewCozy)
}

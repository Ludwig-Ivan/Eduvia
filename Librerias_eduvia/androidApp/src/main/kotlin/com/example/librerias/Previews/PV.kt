package com.example.librerias.Previews

/** Vistas previas solo de Android para diseñar los composables compartidos de `ui`; no forman parte del flujo de la app. */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.librerias.model.Expense
import com.example.librerias.model.expenseCategory
import com.example.librerias.ui.AllExpenseHeader
import com.example.librerias.ui.ExpenceScreen
import com.example.librerias.ui.ExpencesTotalHeader
import com.example.librerias.ui.ExpensesItem

@Preview
@Composable
fun ExpenceScreenPreview(){
    ExpenceScreen()
}

@Preview()
@Composable
fun ExpenseTotalHeaderPreview(){
    Box(
        modifier = Modifier.padding(16.dp)
    ) {
        ExpencesTotalHeader(12520.0)
    }
}

@Preview()
@Composable
fun AllExpenseHeaderPreview(){
    Box(
        modifier = Modifier.padding(16.dp)
    ) {
        AllExpenseHeader()
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseItemPreview(){
    Box(modifier = Modifier.padding(8.dp)){
        ExpensesItem(
            onExpenseClick = {},
            expense = Expense(
                id=1,
                amount = 70.0,
                category = expenseCategory.PARTY,
                description = "Fin de semana"
            )
        )
    }
}
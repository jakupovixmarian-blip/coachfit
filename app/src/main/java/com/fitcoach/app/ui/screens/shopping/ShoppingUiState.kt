package com.fitcoach.app.ui.screens.shopping

/**
 * Stato di presentazione della Lista della spesa.
 *
 * La lista è derivata dal piano alimentare attivo: qui arriva già raggruppata
 * per categoria. Lo stato "preso" è persistito su ShoppingItem.
 */
data class ShoppingUiState(
    val weekLabel: String = "",
    val groups: List<ShoppingGroupUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val totalItems: Int get() = groups.sumOf { it.items.size }

    val checkedItems: Int get() = groups.sumOf { group -> group.items.count { it.checked } }

    val progress: Float
        get() = if (totalItems > 0) checkedItems.toFloat() / totalItems.toFloat() else 0f

    val hasItems: Boolean get() = totalItems > 0
}

/** Categoria di spesa (es. "Proteine", "Carboidrati", "Verdura"). */
data class ShoppingGroupUi(
    val category: String,
    val items: List<ShoppingItemUi> = emptyList(),
)

/** Singolo articolo con quantità calcolata sui grammi del piano. */
data class ShoppingItemUi(
    val id: Long,
    val name: String,
    val quantityLabel: String,
    val checked: Boolean,
)

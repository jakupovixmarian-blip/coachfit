package com.fitcoach.app.data.local

import com.fitcoach.app.domain.model.GearCategory
import com.fitcoach.app.domain.model.GearInventory
import com.fitcoach.app.domain.model.GearItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializzazione dell'inventario attrezzatura.
 *
 * I DTO sono separati dai modelli di dominio perche' il formato su disco deve
 * restare stabile anche se i modelli cambiano: una categoria rinominata non
 * deve rendere illeggibile un profilo gia' salvato.
 */
object GearJson {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    private data class ItemDto(
        val id: String = "",
        val name: String = "",
        val category: String = "CUSTOM",
        val quantity: Int = 1,
        val brand: String = "",
        val model: String = "",
        val minWeightKg: Double? = null,
        val maxWeightKg: Double? = null,
        val incrementsKg: List<Double> = emptyList(),
        val notes: String = ""
    )

    @Serializable
    private data class InventoryDto(
        val version: Int = 1,
        val presets: List<ItemDto> = emptyList(),
        val custom: List<ItemDto> = emptyList()
    )

    fun encode(inventory: GearInventory): String =
        json.encodeToString(
            InventoryDto.serializer(),
            InventoryDto(
                presets = inventory.presets.map { it.toDto() },
                custom = inventory.custom.map { it.toDto() }
            )
        )

    /** Un JSON illeggibile non deve cancellare il profilo: si riparte da vuoto. */
    fun decode(raw: String): GearInventory = runCatching {
        val dto = json.decodeFromString(InventoryDto.serializer(), raw)
        GearInventory(
            presets = dto.presets.mapNotNull { it.toDomain() },
            custom = dto.custom.mapNotNull { it.toDomain() }
        )
    }.getOrDefault(GearInventory())

    private fun GearItem.toDto() = ItemDto(
        id = id, name = name, category = category.name, quantity = quantity,
        brand = brand, model = model, minWeightKg = minWeightKg, maxWeightKg = maxWeightKg,
        incrementsKg = incrementsKg, notes = notes
    )

    private fun ItemDto.toDomain(): GearItem? {
        if (name.isBlank()) return null
        return GearItem(
            id = id.ifBlank { name },
            name = name,
            category = runCatching { GearCategory.valueOf(category) }.getOrDefault(GearCategory.CUSTOM),
            quantity = quantity.coerceAtLeast(0),
            brand = brand,
            model = model,
            minWeightKg = minWeightKg,
            maxWeightKg = maxWeightKg,
            incrementsKg = incrementsKg,
            notes = notes
        )
    }
}

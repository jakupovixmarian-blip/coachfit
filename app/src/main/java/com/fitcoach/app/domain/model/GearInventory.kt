package com.fitcoach.app.domain.model

/**
 * Categoria merceologica dell'attrezzatura. Serve a raggruppare l'inventario
 * nella UI e a dare all'AI un contesto leggibile.
 */
enum class GearCategory(val label: String) {
    BARBELL("Bilancieri e barre"),
    STRUCTURE("Strutture e stazioni"),
    WEIGHTS("Manubri e pesi"),
    BENCH("Panche"),
    MACHINE("Macchine e cavi"),
    ACCESSORY("Accessori"),
    CUSTOM("Attrezzatura personalizzata")
}

/**
 * Singolo attrezzo dell'inventario.
 *
 * Il nome e' libero (non un enum chiuso) perche' la spec richiede di poter
 * inserire attrezzatura non prevista. I campi numerici sono null quando
 * l'utente non li conosce: un limite di carico inventato produrrebbe
 * prescrizioni impossibili.
 */
data class GearItem(
    val id: String,
    val name: String,
    val category: GearCategory,
    /** Quanti esemplari possiede (2 manubri = 2). */
    val quantity: Int = 1,
    val brand: String = "",
    val model: String = "",
    /** Carico minimo utilizzabile in kg (bilanciere vuoto, prima piastra). */
    val minWeightKg: Double? = null,
    /** Carico massimo utilizzabile in kg. */
    val maxWeightKg: Double? = null,
    /** Incrementi disponibili in kg (es. "1.25, 2.5"). */
    val incrementsKg: List<Double> = emptyList(),
    val notes: String = ""
) {
    /** Un attrezzo e' utilizzabile solo se dichiarato presente. */
    val usable: Boolean get() = quantity > 0
}

/**
 * Inventario attrezzatura del profilo.
 *
 * [presets] sono gli attrezzi spuntati dall'elenco standard, [custom] quelli
 * inseriti a mano. Restano separati perche' i preset hanno un nome canonico
 * confrontabile con il catalogo esercizi, i custom no.
 */
data class GearInventory(
    val presets: List<GearItem> = emptyList(),
    val custom: List<GearItem> = emptyList()
) {
    val all: List<GearItem> get() = presets + custom
    val usable: List<GearItem> get() = all.filter { it.usable }
    val isEmpty: Boolean get() = usable.isEmpty()

    fun has(name: String): Boolean =
        usable.any { it.name.equals(name, ignoreCase = true) }

    fun byCategory(): Map<GearCategory, List<GearItem>> =
        usable.groupBy { it.category }

    /** Riepilogo compatto per il prompt AI: una riga per attrezzo. */
    fun summary(): String = usable.joinToString("; ") { item ->
        buildString {
            append(item.name)
            if (item.quantity > 1) append(" x${item.quantity}")
            item.brand.takeIf { it.isNotBlank() }?.let { append(", marca $it") }
            item.model.takeIf { it.isNotBlank() }?.let { append(", modello $it") }
            item.minWeightKg?.let { append(", min ${fmtKg(it)} kg") }
            item.maxWeightKg?.let { append(", max ${fmtKg(it)} kg") }
            if (item.incrementsKg.isNotEmpty()) {
                append(", incrementi ${item.incrementsKg.joinToString("/") { fmtKg(it) }} kg")
            }
            item.notes.takeIf { it.isNotBlank() }?.let { append(", note: $it") }
        }
    }

    private fun fmtKg(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

/**
 * Catalogo degli attrezzi previsti dalla scheda di anamnesi.
 *
 * Ogni voce ha una categoria e, quando ha senso, i valori tipici di
 * fabbrica proposti come default modificabili: l'utente puo' correggerli.
 */
object GearCatalog {

    data class Preset(
        val name: String,
        val category: GearCategory,
        val minWeightKg: Double? = null,
        val maxWeightKg: Double? = null,
        val incrementsKg: List<Double> = emptyList()
    )

    val presets: List<Preset> = listOf(
        // ---- Bilancieri e barre ----
        Preset("Bilanciere olimpico standard (20 kg)", GearCategory.BARBELL, 20.0, null, listOf(1.25, 2.5, 5.0)),
        Preset("Bilanciere olimpico da donna (15 kg)", GearCategory.BARBELL, 15.0, null, listOf(1.25, 2.5, 5.0)),
        Preset("Bilanciere standard da palestra (10 kg)", GearCategory.BARBELL, 10.0, null, listOf(1.25, 2.5)),
        Preset("Bilanciere EZ", GearCategory.BARBELL, 10.0, null, listOf(1.25, 2.5)),
        Preset("Bilanciere W", GearCategory.BARBELL, 10.0, null, listOf(1.25, 2.5)),
        Preset("Barra tricipiti", GearCategory.BARBELL),
        Preset("Trap bar", GearCategory.BARBELL, 20.0, null, listOf(2.5, 5.0)),
        Preset("Safety squat bar", GearCategory.BARBELL, 25.0, null, listOf(2.5, 5.0)),
        Preset("Swiss bar / football bar", GearCategory.BARBELL, 20.0, null, listOf(2.5)),
        Preset("Barra hip thrust", GearCategory.BARBELL, 10.0, null, listOf(2.5)),
        Preset("Barre corte / specialistiche", GearCategory.BARBELL),

        // ---- Strutture ----
        Preset("Smith machine tradizionale", GearCategory.STRUCTURE),
        Preset("Smith machine inclinata", GearCategory.STRUCTURE),
        Preset("Multipower", GearCategory.STRUCTURE),
        Preset("Power rack", GearCategory.STRUCTURE),
        Preset("Half rack", GearCategory.STRUCTURE),
        Preset("Squat stand", GearCategory.STRUCTURE),
        Preset("Sbarra per trazioni", GearCategory.STRUCTURE),
        Preset("Dip station", GearCategory.STRUCTURE),
        Preset("Parallele", GearCategory.STRUCTURE),
        Preset("Stazione multifunzione", GearCategory.STRUCTURE),

        // ---- Manubri e pesi ----
        Preset("Manubri fissi", GearCategory.WEIGHTS, null, null, listOf(1.0, 2.0, 2.5)),
        Preset("Manubri regolabili", GearCategory.WEIGHTS, 2.0, null, listOf(1.0, 2.5)),
        Preset("Manubri esagonali", GearCategory.WEIGHTS),
        Preset("Manubri gommati", GearCategory.WEIGHTS),
        Preset("Kettlebell", GearCategory.WEIGHTS, 4.0, null, listOf(2.0, 4.0)),
        Preset("Dischi in ghisa", GearCategory.WEIGHTS, 1.25, null, listOf(1.25, 2.5, 5.0)),
        Preset("Dischi bumper", GearCategory.WEIGHTS, 5.0, null, listOf(5.0)),
        Preset("Dischi frazionati (0,5-1 kg)", GearCategory.WEIGHTS, 0.5, 1.0, listOf(0.5)),
        Preset("Giubbotto zavorrato", GearCategory.WEIGHTS),
        Preset("Cavigliere zavorrate", GearCategory.WEIGHTS),

        // ---- Panche ----
        Preset("Panca piana", GearCategory.BENCH),
        Preset("Panca inclinabile", GearCategory.BENCH),
        Preset("Panca declinabile", GearCategory.BENCH),
        Preset("Panca regolabile", GearCategory.BENCH),
        Preset("Panca Scott", GearCategory.BENCH),
        Preset("Panca hip thrust", GearCategory.BENCH),
        Preset("Panca addominali", GearCategory.BENCH),

        // ---- Macchine e cavi ----
        Preset("Lat machine", GearCategory.MACHINE),
        Preset("Pulley basso", GearCategory.MACHINE),
        Preset("Crossover", GearCategory.MACHINE),
        Preset("Doppia carrucola regolabile", GearCategory.MACHINE),
        Preset("Functional trainer", GearCategory.MACHINE),
        Preset("Cable tower", GearCategory.MACHINE),
        Preset("Chest press", GearCategory.MACHINE),
        Preset("Shoulder press", GearCategory.MACHINE),
        Preset("Leg press", GearCategory.MACHINE),
        Preset("Hack squat", GearCategory.MACHINE),
        Preset("Leg extension", GearCategory.MACHINE),
        Preset("Leg curl seduto", GearCategory.MACHINE),
        Preset("Leg curl sdraiato", GearCategory.MACHINE),
        Preset("Calf machine", GearCategory.MACHINE),
        Preset("Pec deck", GearCategory.MACHINE),
        Preset("Reverse pec deck", GearCategory.MACHINE),
        Preset("Pullover machine", GearCategory.MACHINE),
        Preset("Row machine", GearCategory.MACHINE),
        Preset("Abdominal machine", GearCategory.MACHINE),
        Preset("Back extension", GearCategory.MACHINE),

        // ---- Accessori ----
        Preset("Corde tricipiti", GearCategory.ACCESSORY),
        Preset("Maniglie singole", GearCategory.ACCESSORY),
        Preset("Barre per cavi", GearCategory.ACCESSORY),
        Preset("Cavigliere per cavi", GearCategory.ACCESSORY),
        Preset("Cinghie", GearCategory.ACCESSORY),
        Preset("Elastici", GearCategory.ACCESSORY),
        Preset("Mini band", GearCategory.ACCESSORY),
        Preset("Foam roller", GearCategory.ACCESSORY),
        Preset("Ruota addominale", GearCategory.ACCESSORY),
        Preset("Tappetino", GearCategory.ACCESSORY),
        Preset("Anelli ginnici", GearCategory.ACCESSORY),
        Preset("TRX", GearCategory.ACCESSORY)
    )

    fun byCategory(): Map<GearCategory, List<Preset>> =
        presets.groupBy { it.category }.filterKeys { it != GearCategory.CUSTOM }

    fun find(name: String): Preset? = presets.firstOrNull { it.name.equals(name, ignoreCase = true) }
}

package com.fitcoach.app.collaudo

import com.fitcoach.app.data.local.GearJson
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.data.local.Mappers
import com.fitcoach.app.domain.model.GearCatalog
import com.fitcoach.app.domain.model.GearCategory
import com.fitcoach.app.domain.model.GearInventory
import com.fitcoach.app.domain.model.GearItem
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Inventario attrezzatura (spec §4): catalogo, serializzazione, persistenza e
 * uso da parte dell'AI (spec §5).
 */
class GearInventoryTest {

    @Test
    fun `il catalogo copre tutte le famiglie richieste dalla spec`() {
        val byCat = GearCatalog.byCategory()
        assertThat(byCat.keys).containsAtLeast(
            GearCategory.BARBELL, GearCategory.STRUCTURE, GearCategory.WEIGHTS,
            GearCategory.BENCH, GearCategory.MACHINE, GearCategory.ACCESSORY
        )
        // Le voci esplicitamente elencate nella spec devono esistere davvero.
        val names = GearCatalog.presets.map { it.name }
        listOf(
            "Trap bar", "Safety squat bar", "Swiss bar / football bar", "Barra hip thrust",
            "Smith machine tradizionale", "Smith machine inclinata", "Multipower",
            "Power rack", "Half rack", "Squat stand", "Sbarra per trazioni", "Dip station",
            "Manubri regolabili", "Manubri fissi", "Kettlebell", "Dischi bumper",
            "Dischi frazionati (0,5-1 kg)", "Giubbotto zavorrato", "Cavigliere zavorrate",
            "Panca Scott", "Panca hip thrust", "Panca addominali", "Lat machine",
            "Pulley basso", "Crossover", "Doppia carrucola regolabile", "Functional trainer",
            "Leg press", "Hack squat", "Leg extension", "Leg curl seduto", "Leg curl sdraiato",
            "Calf machine", "Pec deck", "Reverse pec deck", "Pullover machine", "Row machine",
            "Abdominal machine", "Back extension", "Corde tricipiti", "Ruota addominale",
            "Anelli ginnici", "TRX"
        ).forEach { assertWithMessage("manca %s", it).that(names).contains(it) }
    }

    @Test
    fun `la serializzazione conserva tutti i dettagli`() {
        val inventory = GearInventory(
            presets = listOf(
                GearItem(
                    id = "Bilanciere olimpico standard (20 kg)",
                    name = "Bilanciere olimpico standard (20 kg)",
                    category = GearCategory.BARBELL,
                    quantity = 1,
                    brand = "Eleiko",
                    model = "IPF",
                    minWeightKg = 20.0,
                    maxWeightKg = 300.0,
                    incrementsKg = listOf(1.25, 2.5, 5.0),
                    notes = "barra da gara"
                )
            ),
            custom = listOf(
                GearItem(
                    id = "custom:Panchetta artigianale",
                    name = "Panchetta artigianale",
                    category = GearCategory.CUSTOM,
                    quantity = 2,
                    notes = "costruita in officina"
                )
            )
        )

        val encoded = GearJson.encode(inventory)
        val decoded = GearJson.decode(encoded)

        assertThat(decoded.presets).hasSize(1)
        val bar = decoded.presets.first()
        assertThat(bar.brand).isEqualTo("Eleiko")
        assertThat(bar.model).isEqualTo("IPF")
        assertThat(bar.minWeightKg).isEqualTo(20.0)
        assertThat(bar.maxWeightKg).isEqualTo(300.0)
        assertThat(bar.incrementsKg).containsExactly(1.25, 2.5, 5.0).inOrder()
        assertThat(bar.notes).isEqualTo("barra da gara")
        assertThat(decoded.custom).hasSize(1)
        assertThat(decoded.custom.first().quantity).isEqualTo(2)
        assertThat(decoded.custom.first().category).isEqualTo(GearCategory.CUSTOM)
    }

    @Test
    fun `un JSON corrotto non fa perdere il profilo`() {
        assertThat(GearJson.decode("{non-json")).isEqualTo(GearInventory())
        assertThat(GearJson.decode("")).isEqualTo(GearInventory())
    }

    @Test
    fun `l'inventario sopravvive al round-trip profilo entita`() {
        val profile = UserProfile(
            name = "Test",
            gearInventory = GearInventory(
                presets = listOf(
                    GearItem(
                        id = "Power rack", name = "Power rack",
                        category = GearCategory.STRUCTURE, quantity = 1
                    )
                )
            )
        )
        val entity = with(Mappers) { profile.toEntity() }
        assertThat(entity.gearInventoryJson).isNotEmpty()
        val back = with(Mappers) { entity.toDomain() }
        assertThat(back.gearInventory.usable.map { it.name }).containsExactly("Power rack")
    }

    @Test
    fun `il riepilogo per l'AI contiene i limiti di carico`() {
        val inventory = GearInventory(
            presets = listOf(
                GearItem(
                    id = "Manubri regolabili", name = "Manubri regolabili",
                    category = GearCategory.WEIGHTS, quantity = 2,
                    minWeightKg = 2.0, maxWeightKg = 32.0,
                    incrementsKg = listOf(1.0, 2.5)
                )
            )
        )
        val summary = inventory.summary()
        assertWithMessage(summary).that(summary).contains("Manubri regolabili")
        assertWithMessage(summary).that(summary).contains("x2")
        assertWithMessage(summary).that(summary).contains("min 2 kg")
        assertWithMessage(summary).that(summary).contains("max 32 kg")
        assertWithMessage(summary).that(summary).contains("incrementi 1/2.5 kg")
    }

    @Test
    fun `un attrezzo con quantita zero non risulta disponibile`() {
        val inventory = GearInventory(
            presets = listOf(
                GearItem(id = "Lat machine", name = "Lat machine",
                    category = GearCategory.MACHINE, quantity = 0)
            )
        )
        assertThat(inventory.usable).isEmpty()
        assertThat(inventory.isEmpty).isTrue()
        assertThat(inventory.summary()).isEmpty()
    }

    @Test
    fun `il prompt di generazione contiene l'inventario reale`() {
        val profile = UserProfile(
            name = "Test",
            gearInventory = GearInventory(
                presets = listOf(
                    GearItem(
                        id = "Power rack", name = "Power rack",
                        category = GearCategory.STRUCTURE,
                        minWeightKg = 20.0, maxWeightKg = 250.0,
                        incrementsKg = listOf(2.5, 5.0)
                    )
                )
            )
        )

        val header = AiProgramGenerationService.declaredHeader(profile)

        assertThat(header).contains("INVENTARIO ATTREZZATURA REALE")
        assertThat(header).contains("Power rack")
        assertThat(header).contains("max 250 kg")
        // Il vincolo operativo deve essere esplicito, non implicito nel dato.
        assertThat(header).contains("VINCOLO")
    }

    @Test
    fun `senza inventario il prompt dice che non e' compilato e non impone vincoli`() {
        val header = AiProgramGenerationService.declaredHeader(UserProfile(name = "Test"))
        assertThat(header).contains("NON_COMPILATO")
        assertThat(header).doesNotContain("VINCOLO")
    }

}

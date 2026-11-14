package dev.fitcoach.app

import dev.fitcoach.app.data.local.mapper.toDomain
import dev.fitcoach.app.data.local.mapper.toDomainOrNull
import dev.fitcoach.app.data.local.mapper.toEntity
import dev.fitcoach.app.data.seed.ExerciseSeed
import dev.fitcoach.app.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the catalog the editable lists depend on.
 *
 * The reported bug was an exercise picker with nothing to pick. The seed data itself was
 * fine; what could empty every list at once was the mapper: a single stored row with an
 * enum value the app no longer knows made valueOf() throw, which failed the whole
 * observeAll() flow. These tests pin both the data and the tolerant read.
 */
class ExerciseCatalogIntegrityTest {

    private val catalog = ExerciseSeed.all()

    @Test
    fun `catalog is large enough to build a routine`() {
        assertTrue("solo ${catalog.size} esercizi nel catalogo", catalog.size >= 30)
    }

    @Test
    fun `names are unique and never blank`() {
        val blank = catalog.count { it.name.isBlank() }
        assertEquals("esercizi senza nome", 0, blank)
        val duplicates = catalog.groupBy { it.name.trim().lowercase() }
            .filterValues { it.size > 1 }
            .keys
        assertTrue("nomi duplicati nel catalogo: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `every muscle group has at least one exercise`() {
        val covered = catalog.map { it.primaryMuscle }.toSet()
        val missing = MuscleGroup.entries.filter { it !in covered }
        assertTrue("gruppi muscolari senza esercizi: $missing", missing.isEmpty())
    }

    @Test
    fun `every exercise survives the database round trip`() {
        catalog.forEach { exercise ->
            val back = exercise.toEntity().toDomainOrNull()
            assertNotNull("non rileggibile dal database: ${exercise.name}", back)
            assertEquals(exercise.name, back!!.name)
            assertEquals(exercise.primaryMuscle, back.primaryMuscle)
            assertEquals(exercise.equipment, back.equipment)
            assertEquals(exercise.pattern, back.pattern)
            assertEquals(exercise.secondaryMuscles, back.secondaryMuscles)
            assertEquals(exercise.isCustom, back.isCustom)
        }
    }

    @Test
    fun `unreadable row is skipped without emptying the list`() {
        val broken = catalog.first().toEntity().copy(primaryMuscle = "SPALLE_INESISTENTI")
        assertNull("una riga con muscolo sconosciuto va scartata", broken.toDomainOrNull())

        val rows = catalog.take(3).map { it.toEntity() } + broken
        val mapped = rows.mapNotNull { it.toDomainOrNull() }
        assertEquals("le righe valide devono restare", 3, mapped.size)
    }

    @Test
    fun `unknown optional values fall back instead of throwing`() {
        val broken = catalog.first().toEntity().copy(equipment = "SLITTA", pattern = "VOLO")
        val back = broken.toDomainOrNull()
        assertNotNull(back)
        assertEquals(catalog.first().primaryMuscle, back!!.primaryMuscle)
        assertEquals(catalog.first().name, back.name)
    }

    @Test
    fun `toDomain never throws on unknown stored values`() {
        val broken = catalog.first().toEntity()
            .copy(primaryMuscle = "SCONOSCIUTO", secondaryMuscles = "SCONOSCIUTO,ALTRO")
        val back = broken.toDomain()
        assertEquals(MuscleGroup.CORE, back.primaryMuscle)
        assertTrue("muscoli secondari sconosciuti vanno ignorati", back.secondaryMuscles.isEmpty())
        assertEquals(catalog.first().name, back.name)
    }

    @Test
    fun `no exercise is a near duplicate of another`() {
        // Two rows for the same movement with the same equipment would give the picker two
        // indistinguishable choices and double count the volume for that muscle.
        fun base(raw: String): String {
            var name = raw.lowercase()
            listOf(
                " con bilanciere", " con manubri", " con manubrio", " ai cavi",
                " al cavo basso", " alla macchina", " a corpo libero",
                " con elastici", " con kettlebell", " frontale", " in piedi"
            ).forEach { name = name.replace(it, "") }
            return name.trim()
        }

        val duplicates = catalog
            .groupBy { Triple(it.primaryMuscle, it.equipment, base(it.name)) }
            .filterValues { it.size > 1 }
            .map { (key, items) -> "${items.map { it.name }} (${key.first}/${key.second})" }

        assertTrue("esercizi quasi identici: $duplicates", duplicates.isEmpty())
    }
}

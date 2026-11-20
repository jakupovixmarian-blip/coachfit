package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.knowledge.InvictusCorpus
import com.fitcoach.app.domain.knowledge.KnowledgeCorpus
import com.fitcoach.app.domain.model.Equipment
import com.fitcoach.app.domain.model.Exercise
import com.fitcoach.app.domain.model.ExerciseCategory
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.MuscleGroup
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.SourceKind
import com.fitcoach.app.domain.model.TrainingExperience
import com.fitcoach.app.domain.model.UserProfile
import com.fitcoach.app.domain.workout.WorkoutPlanValidator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Le soglie numeriche non sono cancelli: il volume settimanale dipende da
 * metodologia e contesto, e una scheda che se ne discosta va accettata con un
 * avviso motivato. I blocchi restano solo su cio' che e' verificabile e non
 * negoziabile (esercizi incompatibili, giornate incoerenti, serie fuori range).
 */
class ThresholdsAreWarningsTest {

    private val profile = UserProfile(
        ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
        trainingDaysPerWeek = 2, experience = TrainingExperience.ADVANCED,
        goal = Goal.HYPERTROPHY, availableEquipment = Equipment.entries.toSet()
    )

    private fun ex(id: Long, name: String, muscle: MuscleGroup) = Exercise(
        id = id, name = name, primaryMuscle = muscle, secondaryMuscles = emptySet(),
        category = ExerciseCategory.COMPOUND, equipment = setOf(Equipment.BARBELL),
        technique = "", defaultSets = 4, minReps = 8, maxReps = 12,
        restSeconds = 120, targetRir = 2, difficulty = 1
    )

    @Test
    fun `polpacci sotto soglia per un avanzato non bloccano il piano`() {
        // 8 serie di polpacci: sotto l'intervallo di riferimento, ma e' una scelta
        // di programmazione legittima (es. priorita' su altri distretti).
        val catalog = listOf(
            ex(1, "Calf raise", MuscleGroup.CALVES),
            ex(2, "Panca", MuscleGroup.CHEST)
        )
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.CALVES),
                listOf(WorkoutPlanValidator.ExerciseProposal(1, 4))
            ),
            WorkoutPlanValidator.DayProposal(
                1, setOf(MuscleGroup.CHEST),
                listOf(WorkoutPlanValidator.ExerciseProposal(2, 4))
            )
        )
        val report = WorkoutPlanValidator.validate(profile, catalog, days)

        assertThat(report.ok).isTrue()
        assertThat(report.warnings.joinToString("\n")).contains("CALVES")
    }

    @Test
    fun `un esercizio incompatibile resta un blocco`() {
        val catalog = listOf(ex(1, "Panca", MuscleGroup.CHEST))
        val days = listOf(
            WorkoutPlanValidator.DayProposal(
                0, setOf(MuscleGroup.CHEST),
                listOf(WorkoutPlanValidator.ExerciseProposal(1, 4))
            )
        )
        val limited = profile.copy(movementsToAvoid = listOf("panca"))
        val report = WorkoutPlanValidator.validate(limited, catalog, days)

        assertThat(report.ok).isFalse()
    }

    @Test
    fun `il corpus copre allenamento nutrizione e sicurezza con fonti dichiarate`() {
        val areas = KnowledgeCorpus.rules.map { it.area }.toSet()
        assertThat(areas).containsExactly(
            KnowledgeCorpus.Area.TRAINING,
            KnowledgeCorpus.Area.NUTRITION,
            KnowledgeCorpus.Area.SAFETY
        )
        KnowledgeCorpus.rules.forEach { rule ->
            assertThat(rule.source).isNotEmpty()
            assertThat(rule.statement).isNotEmpty()
            assertThat(rule.limits).isNotEmpty()
        }
    }

    @Test
    fun `il corpus entra nel prompt con le fonti e i limiti`() {
        val block = KnowledgeCorpus.promptBlock()
        assertThat(block).contains("Fonte:")
        assertThat(block).contains("Limiti:")
        assertThat(block).contains("non cancelli")
    }

    @Test
    fun `invictus resta dichiarata divulgativa e non diventa autorita scientifica`() {
        val invictus = KnowledgeCorpus.rules.filter {
            it.source.contains("Project Invictus")
        }
        assertThat(invictus).isNotEmpty()
        invictus.forEach { rule ->
            assertThat(rule.kind).isEqualTo(SourceKind.DIVULGATIVE)
            assertThat(rule.limits).contains("divulgativa")
        }
    }

    @Test
    fun `il corpus invictus cita una fonte verificabile per ogni regola`() {
        assertThat(InvictusCorpus.rules.size).isAtLeast(120)
        InvictusCorpus.rules.forEach { rule ->
            assertThat(rule.statement).isNotEmpty()
            assertThat(rule.articleTitle).isNotEmpty()
            assertThat(rule.url).startsWith("https://www.projectinvictus.it/")
            assertThat(rule.publishedDate).isNotEmpty()
            // La metodologia Invictus e divulgativa: non va spacciata per linee guida.
            assertThat(rule.kind).isEqualTo(SourceKind.DIVULGATIVE)
        }
    }

    @Test
    fun `ogni regola contiene un numero verificabile`() {
        // Il valore del corpus sta nei numeri: senza, e' un'opinione.
        InvictusCorpus.rules.forEach { rule ->
            assertThat(rule.statement).matches(".*\\d.*")
        }
    }

    @Test
    fun `il corpus copre tutti gli argomenti che servono al motore`() {
        val training = InvictusCorpus.forArea(KnowledgeCorpus.Area.TRAINING)
        val nutrition = InvictusCorpus.forArea(KnowledgeCorpus.Area.NUTRITION)
        assertThat(training.size).isAtLeast(60)
        assertThat(nutrition.size).isAtLeast(50)

        // Ogni argomento del motore deve avere almeno una regola.
        listOf(
            InvictusCorpus.Topic.VOLUME,
            InvictusCorpus.Topic.FREQUENCY,
            InvictusCorpus.Topic.RECOVERY,
            InvictusCorpus.Topic.INTENSITY,
            InvictusCorpus.Topic.PROTEIN,
            InvictusCorpus.Topic.CARBS,
            InvictusCorpus.Topic.FAT,
            InvictusCorpus.Topic.CALORIES
        ).forEach { topic ->
            assertThat(InvictusCorpus.forTopic(topic)).isNotEmpty()
        }
    }

    @Test
    fun `le regole contengono i numeri chiave della metodologia`() {
        val all = InvictusCorpus.rules.joinToString("\n") { it.statement }
        assertThat(all).contains("serie")
        assertThat(all).contains("proteine")
        assertThat(all).contains("grassi")
    }

    @Test
    fun `il blocco prompt resta limitato e cita la fonte`() {
        // Il prompt non deve diventare un elenco interminabile: si limita per argomento.
        val block = InvictusCorpus.promptBlock(maxPerTopic = 3)
        assertThat(block).contains("PROJECT INVICTUS")
        assertThat(block).contains("https://www.projectinvictus.it/")

        val maxExpected = InvictusCorpus.Topic.entries.size * 3
        val ruleLines = block.lines().count { it.startsWith("- ") }
        assertThat(ruleLines).isAtMost(maxExpected)
        assertThat(ruleLines).isAtLeast(20)
    }

    @Test
    fun `il testo completo riporta tutte le regole consultabili`() {
        val full = InvictusCorpus.fullText()
        InvictusCorpus.rules.forEach { rule ->
            assertThat(full).contains(rule.articleTitle)
        }
    }
}

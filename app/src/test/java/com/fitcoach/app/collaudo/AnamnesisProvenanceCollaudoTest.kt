package com.fitcoach.app.collaudo

import com.fitcoach.app.domain.anamnesis.AnamnesisEngine
import com.fitcoach.app.domain.coach.AiProgramGenerationService
import com.fitcoach.app.domain.coach.AnamnesisSummary
import com.fitcoach.app.domain.model.Goal
import com.fitcoach.app.domain.model.Sex
import com.fitcoach.app.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * L'anamnesi non deve presentare i default del profilo come dati dichiarati.
 *
 * Il profilo usa default non nullable (obiettivo RECOMP, lavoro SEDENTARY,
 * 5 pasti, esperienza BEGINNER) per non propagare null: se finiscono nel
 * contesto dell'AI come risposte, il coach decide su dati che l'utente non ha
 * mai fornito. Questi test bloccano quella regressione.
 */
class AnamnesisProvenanceCollaudoTest {

    private val unknown = "NON_DISPONIBILE"

    @Test
    fun `un profilo vuoto non espone alcun dato come dichiarato`() {
        val p = UserProfile()
        assertThat(AnamnesisEngine.answeredIds(p)).isEmpty()

        val summary = AnamnesisSummary.forProfile(p)
        assertThat(summary).doesNotContain("RECOMPOSITION")
        assertThat(summary).doesNotContain("SEDENTARY")
        assertThat(summary).doesNotContain("null")
        assertThat(summary).contains(unknown)
    }

    @Test
    fun `i valori risposti compaiono e i mancanti restano non disponibili`() {
        val p = UserProfile(
            ageYears = 30,
            sex = Sex.MALE,
            heightCm = 180.0,
            weightKg = 80.0,
            goal = Goal.HYPERTROPHY,
            answeredQuestionIds = setOf("age", "sex", "heightCm", "weightKg", "goal")
        )

        val summary = AnamnesisSummary.forProfile(p)
        assertThat(summary).contains("eta=30")
        assertThat(summary).contains("sesso=MALE")
        assertThat(summary).contains("obiettivo=HYPERTROPHY")
        // Facoltativi non dichiarati: non devono sembrare misure reali.
        assertThat(summary).contains("massa_grassa=$unknown")
        assertThat(summary).contains("passi=$unknown")
        assertThat(summary).doesNotContain("null")
    }

    @Test
    fun `i campi obbligatori restano coperti da una risposta esplicita`() {
        val p = UserProfile()
        val mandatory = AnamnesisEngine.missingRequired(p).map { it.id }
        // Il gate di generazione si basa sulle obbligatorie: se una di esse
        // risultasse "risposta" senza esserlo, l'utente genererebbe un piano
        // su dati mancanti.
        assertThat(mandatory).isNotEmpty()
        assertThat(AnamnesisEngine.answeredIds(p)).containsNoneIn(mandatory)
    }

    @Test
    fun `una risposta registrata sopravvive alla lettura del profilo`() {
        val p = UserProfile(ageYears = 30, answeredQuestionIds = setOf("age"))
        assertThat(AnamnesisEngine.answeredIds(p)).containsExactly("age")
    }

    @Test
    fun `il budget settimanale inserito nel form non risulta mancante`() {
        // Non e una domanda dell'intervista: senza un ramo dedicato restava
        // sempre NON_DISPONIBILE anche quando l'utente lo aveva compilato.
        val p = UserProfile(weeklyFoodBudgetEur = 70.0)
        val summary = AnamnesisSummary.forProfile(p)
        assertThat(summary).contains("budget_settimanale=70.0")

        val empty = AnamnesisSummary.forProfile(UserProfile())
        assertThat(empty).contains("budget_settimanale=$unknown")
    }

    @Test
    fun `ogni id dichiarato dall'onboarding esiste come domanda`() {
        // Un id inventato verrebbe persistito ma mai letto dal riepilogo.
        val known = AnamnesisEngine.questions.map { it.id }.toSet()
        val declared = setOf(
            "age", "heightCm", "weightKg", "bodyFatPercent", "dailySteps", "sleepHours",
            "trainingYears", "maxSessionMinutes", "allergies", "likedFoods", "dislikedFoods",
            "monthlyFoodBudgetEur", "preferredExercises", "dislikedExercises",
            "physicalLimitations", "movementsToAvoid"
        )
        assertThat(known).containsAtLeastElementsIn(declared)
    }

    @Test
    fun `il contesto di generazione non contraddice il riepilogo`() {
        // Il riepilogo dichiarava "obiettivo=NON_DISPONIBILE" e la riga
        // successiva ripeteva "OBIETTIVO: RECOMP": due istruzioni opposte nello
        // stesso prompt, con l'AI libera di scegliere il default.
        val p = UserProfile(ageYears = 30, heightCm = 180.0, weightKg = 80.0)
        val summary = AnamnesisSummary.forProfile(p)
        assertThat(summary).contains("obiettivo=$unknown")
        assertThat(summary).contains("giorni_allenamento=$unknown")

        val header = AiProgramGenerationService.declaredHeader(p)
        assertThat(header).doesNotContain("RECOMP")
        assertThat(header).contains("OBIETTIVO: NON_DICHIARATO")
        // I giorni restano un vincolo del validatore: non vanno dichiarati assenti.
        assertThat(header).contains("vincolo: esattamente")
    }

    @Test
    fun `un obiettivo dichiarato compare nel contesto`() {
        val p = UserProfile(goal = Goal.HYPERTROPHY, answeredQuestionIds = setOf("goal"))
        val header = AiProgramGenerationService.declaredHeader(p)
        assertThat(header).contains("OBIETTIVO: HYPERTROPHY")
        assertThat(AiProgramGenerationService.declaredHeader(UserProfile()))
            .doesNotContain("HYPERTROPHY")
    }

    // --- L'intervista deve porre le domande che hanno un default ---------------

    private fun answerFor(q: AnamnesisEngine.Question): String = when (q.kind) {
        AnamnesisEngine.Kind.NUMBER, AnamnesisEngine.Kind.INT -> "1"
        AnamnesisEngine.Kind.BOOL -> "true"
        AnamnesisEngine.Kind.CHOICE, AnamnesisEngine.Kind.MULTI -> q.options.first()
        AnamnesisEngine.Kind.TEXT -> "test"
    }

    /** Simula un'intervista completa, registrando ogni risposta come dichiarata. */
    private fun interview(start: UserProfile = UserProfile()): List<String> {
        var p = start
        val asked = mutableListOf<String>()
        while (true) {
            val q = AnamnesisEngine.nextQuestion(p) ?: break
            asked += q.id
            p = AnamnesisEngine.applyAnswer(p, q.id, answerFor(q)).let {
                it.copy(answeredQuestionIds = it.answeredQuestionIds + q.id)
            }
        }
        return asked
    }

    @Test
    fun `l'intervista chiede obiettivo esperienza e giorni di allenamento`() {
        // Regressione: con `isAnswered` da solo queste domande risultavano
        // gia risposte per via del default e non venivano MAI poste.
        val asked = interview()
        assertThat(asked).containsAtLeast("goal", "experience", "trainingDaysPerWeek", "workType")
    }

    @Test
    fun `l'intervista copre tutte le domande tranne i carichi per chi non si allena`() {
        val asked = interview().toSet()
        val never = AnamnesisEngine.questions.map { it.id }.toSet() - asked
        assertThat(never).containsExactly("currentLoads")
    }

    @Test
    fun `una domanda con default gia dichiarata non viene riproposta`() {
        val p = UserProfile(goal = Goal.HYPERTROPHY, answeredQuestionIds = setOf("goal"))
        assertThat(AnamnesisEngine.needsAnswer(p, "goal")).isFalse()
    }

    @Test
    fun `una domanda con default mai dichiarata va ancora posta`() {
        val p = UserProfile(goal = Goal.HYPERTROPHY)
        assertThat(AnamnesisEngine.needsAnswer(p, "goal")).isTrue()
    }

    @Test
    fun `un profilo gia confermato non torna a chiedere le domande dichiarate`() {
        // La migrazione 13->14 marca le obbligatorie: l'intervista non deve
        // ricominciare da capo per chi ha gia completato l'anamnesi.
        val mandatory = AnamnesisEngine.questions.filter { it.required }.map { it.id }.toSet()
        val p = UserProfile(
            ageYears = 30, sex = Sex.MALE, heightCm = 180.0, weightKg = 80.0,
            sleepHours = 7.0, dietaryPreferences = setOf(com.fitcoach.app.domain.model.DietaryPreference.NONE),
            trainingStatus = com.fitcoach.app.domain.model.TrainingStatus.CURRENTLY_TRAINING,
            availableEquipment = setOf(com.fitcoach.app.domain.model.Equipment.BARBELL),
            anamnesisCompleted = true,
            answeredQuestionIds = mandatory
        )
        // Nessuna obbligatoria da riproporre: l'intervista non riparte.
        assertThat(AnamnesisEngine.missingRequired(p)).isEmpty()
        assertThat(AnamnesisEngine.nextQuestion(p, skipOptional = true)).isNull()
    }
}

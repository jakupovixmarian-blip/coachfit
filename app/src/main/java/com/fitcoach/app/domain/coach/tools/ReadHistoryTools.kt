package com.fitcoach.app.domain.coach.tools

import com.fitcoach.app.data.repository.NutritionDiaryRepository
import com.fitcoach.app.data.repository.ProgressPhotoRepository
import com.fitcoach.app.data.repository.ProgressRepository
import com.fitcoach.app.data.repository.WorkoutRepository
import com.fitcoach.app.domain.adaptive.WeightAnalyzer
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** Storico allenamenti: sessioni recenti e prestazioni per esercizio (carichi/rep/volume). */
@Singleton
class GetWorkoutHistoryTool @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : CoachTool {
    override val name = "get_workout_history"
    override val description =
        "Restituisce lo storico allenamenti: sessioni recenti (data, stato, volume, RPE, sonno, " +
            "dolore) e le prestazioni per esercizio (carico top, rep, volume, 1RM stimato, RIR). " +
            "Usalo per valutare progressione, stagnazione e affaticamento prima di proporre modifiche."

    override val parameters: JsonObject = Schema.obj(
        "sessioneLimite" to Schema.integer("Numero di sessioni recenti da includere (default 12)."),
        "esercizioId" to Schema.integer("Se presente, include lo storico prestazioni di quell'esercizio.")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val args = JsonArgs(arguments)
        val limit = (args.int("sessioneLimite") ?: 12).coerceIn(1, 60)
        val exerciseId = args.int("esercizioId")

        val sessions = workoutRepository.getRecentSessions(limit).map { s ->
            JsonOut.obj(
                "id" to JsonOut.num(s.id),
                "dataEpochDay" to JsonOut.num(s.dateEpochDay),
                "titolo" to JsonOut.str(s.dayTitle),
                "stato" to JsonOut.str(s.status.name),
                "durataMin" to JsonOut.num(s.durationMinutes),
                "serieCompletate" to JsonOut.num(s.completedSets),
                "volumeKg" to JsonOut.num(s.totalVolumeKg),
                "rpePercepito" to JsonOut.num(s.perceivedEffort),
                "qualitaSonno" to JsonOut.num(s.sleepQuality),
                "doloreSegnalato" to JsonOut.bool(s.painReported),
                "noteDolore" to JsonOut.str(s.painNotes.ifBlank { null })
            )
        }

        val performances = if (exerciseId != null) {
            workoutRepository.getPerformanceFor(exerciseId.toLong()).takeLast(20).map { p ->
                JsonOut.obj(
                    "dataEpochDay" to JsonOut.num(p.dateEpochDay),
                    "caricoTopKg" to JsonOut.num(p.topWeightKg),
                    "repTop" to JsonOut.num(p.topReps),
                    "serieTotali" to JsonOut.num(p.totalSets),
                    "volumeKg" to JsonOut.num(p.volumeKg),
                    "rirMedio" to JsonOut.num(p.avgRir),
                    "stima1Rm" to JsonOut.num(p.estimated1Rm)
                )
            }
        } else emptyList()

        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "sessioni" to JsonOut.arr(sessions),
                "prestazioni" to JsonOut.arr(performances),
                "sessioniCompletateTotali" to JsonOut.num(workoutRepository.completedSessionCount())
            )
        )
    }
}

/** Peso, circonferenze, record di progresso e check-in: la base per l'analisi dell'evoluzione. */
@Singleton
class GetProgressTool @Inject constructor(
    private val progressRepository: ProgressRepository
) : CoachTool {
    override val name = "get_progress"
    override val description =
        "Restituisce l'evoluzione del soggetto: storico peso, medie settimanali e trend (kg/settimana " +
            "con affidabilita), circonferenze, record di progresso e check-in. Usalo per capire se il " +
            "piano sta funzionando e se servono modifiche. Non reagire a oscillazioni giornaliere."

    override val parameters: JsonObject = Schema.obj(
        "settimanePeso" to Schema.integer("Numero di settimane di medie peso (default 8).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val weeks = (JsonArgs(arguments).int("settimanePeso") ?: 8).coerceIn(1, 52)
        val weights = progressRepository.getWeights()
        val trend = WeightAnalyzer.trend(weights, weeks)
        val weekly = WeightAnalyzer.weeklyAverages(weights, weeks).map { w ->
            JsonOut.obj(
                "settimanaInizio" to JsonOut.num(w.weekStartEpochDay),
                "mediaKg" to JsonOut.num(w.avgWeightKg),
                "campioni" to JsonOut.num(w.samples),
                "minKg" to JsonOut.num(w.minWeightKg),
                "maxKg" to JsonOut.num(w.maxWeightKg)
            )
        }
        val circumferences = progressRepository.getCircumferences().takeLast(60).map { c ->
            JsonOut.obj(
                "dataEpochDay" to JsonOut.num(c.dateEpochDay),
                "sito" to JsonOut.str(c.site),
                "valoreCm" to JsonOut.num(c.valueCm)
            )
        }
        val checkIns = progressRepository.getCheckIns().takeLast(12).map { c ->
            JsonOut.obj(
                "settimanaInizio" to JsonOut.num(c.weekStartEpochDay),
                "pesoMedioKg" to JsonOut.num(c.avgWeightKg),
                "trendKgSettimana" to JsonOut.num(c.weightTrendKgPerWeek),
                "kcalMedie" to JsonOut.num(c.avgCalories),
                "aderenzaPerc" to JsonOut.num(c.dietAdherencePercent),
                "seduteCompletate" to JsonOut.num(c.trainingSessionsCompleted),
                "sedutePianificate" to JsonOut.num(c.trainingSessionsPlanned),
                "trendPrestazioni" to JsonOut.str(c.performanceTrend),
                "sonnoMedio" to JsonOut.num(c.sleepQualityAvg),
                "riepilogo" to JsonOut.str(c.summary.ifBlank { null })
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "pesoAttualeKg" to JsonOut.num(progressRepository.getLatestWeight()?.weightKg),
                "trend" to JsonOut.obj(
                    "kgPerSettimana" to JsonOut.num(trend.trendKgPerWeek),
                    "settimaneDati" to JsonOut.num(trend.weeksOfData),
                    "affidabile" to JsonOut.bool(trend.isReliable)
                ),
                "medieSettimanaliPeso" to JsonOut.arr(weekly),
                "circonferenze" to JsonOut.arr(circumferences),
                "checkIn" to JsonOut.arr(checkIns)
            )
        )
    }
}

/** Diario nutrizionale: consumo reale vs obiettivo e aderenza. */
@Singleton
class GetNutritionDiaryTool @Inject constructor(
    private val diaryRepository: NutritionDiaryRepository
) : CoachTool {
    override val name = "get_nutrition_diary"
    override val description =
        "Restituisce il diario nutrizionale di una giornata: voci registrate, totale kcal/macro, " +
            "obiettivo e rimanente. Usalo per valutare l'aderenza reale e il consumo effettivo."

    override val parameters: JsonObject = Schema.obj(
        "giorniIndietro" to Schema.integer("Quanti giorni indietro (0=oggi, default 0).")
    )

    override suspend fun execute(arguments: JsonObject): String {
        val back = (JsonArgs(arguments).int("giorniIndietro") ?: 0).coerceIn(0, 60)
        val epochDay = java.time.LocalDate.now().toEpochDay() - back
        val balance = diaryRepository.balanceForDay(epochDay)
        val entries = diaryRepository.entriesForDay(epochDay).map { e ->
            JsonOut.obj(
                "alimento" to JsonOut.str(e.foodName),
                "grammi" to JsonOut.num(e.grams),
                "kcal" to JsonOut.num(e.kcal),
                "proteine" to JsonOut.num(e.protein),
                "carboidrati" to JsonOut.num(e.carbs),
                "grassi" to JsonOut.num(e.fat),
                "attivo" to JsonOut.bool(e.active)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "dataEpochDay" to JsonOut.num(epochDay),
                "obiettivoKcal" to JsonOut.num(balance.targetKcal),
                "consumateKcal" to JsonOut.num(balance.consumedKcal),
                "rimanentiKcal" to JsonOut.num(balance.remainingKcal),
                "proteineConsumate" to JsonOut.num(balance.consumedProtein),
                "carboidratiConsumati" to JsonOut.num(balance.consumedCarbs),
                "grassiConsumati" to JsonOut.num(balance.consumedFat),
                "voci" to JsonOut.arr(entries)
            )
        )
    }
}

/** Foto di progresso: solo metadati (mai contenuti) e stato del consenso AI. */
@Singleton
class GetProgressPhotosTool @Inject constructor(
    private val photoRepository: ProgressPhotoRepository
) : CoachTool {
    override val name = "get_progress_photos"
    override val description =
        "Elenca le foto di progresso dell'utente con data, posa, nota e consenso all'analisi AI. " +
            "Restituisce SOLO metadati, non i contenuti. Serve a valutare l'evoluzione visiva nel tempo."

    override val parameters: JsonObject = Schema.obj()

    override suspend fun execute(arguments: JsonObject): String {
        val photos = photoRepository.photos.first().sortedBy { it.capturedAt }
        val items = photos.map { p ->
            JsonOut.obj(
                "id" to JsonOut.num(p.id),
                "catturataIl" to JsonOut.num(p.capturedAt),
                "posa" to JsonOut.str(p.pose.ifBlank { null }),
                "nota" to JsonOut.str(p.note.ifBlank { null }),
                "consensoAi" to JsonOut.bool(p.consentForAi)
            )
        }
        return JsonOut.encode(
            JsonOut.obj(
                "ok" to JsonOut.bool(true),
                "totale" to JsonOut.num(photos.size),
                "conConsensoAi" to JsonOut.num(photos.count { it.consentForAi }),
                "foto" to JsonOut.arr(items)
            )
        )
    }
}

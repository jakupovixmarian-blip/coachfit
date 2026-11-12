package dev.fitcoach.app.data.bootstrap

import dev.fitcoach.app.domain.repository.ExerciseRepository
import dev.fitcoach.app.domain.repository.FoodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Seeds the exercise and food catalogs once, on first launch. */
@Singleton
class CatalogInitializer @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val foodRepository: FoodRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        scope.launch {
            runCatching { exerciseRepository.ensureSeeded() }
            runCatching { foodRepository.ensureSeeded() }
        }
    }
}

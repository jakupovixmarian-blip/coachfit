package dev.fitcoach.app.core.result

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed class AppError(override val message: String, override val cause: Throwable? = null) : Throwable(message, cause) {
    class Network(message: String = "Nessuna connessione disponibile", cause: Throwable? = null) : AppError(message, cause)
    class Auth(message: String = "Autenticazione non valida", cause: Throwable? = null) : AppError(message, cause)
    class RateLimit(message: String = "Limite di richieste raggiunto", cause: Throwable? = null) : AppError(message, cause)
    class Timeout(message: String = "Tempo di risposta scaduto", cause: Throwable? = null) : AppError(message, cause)
    class InsufficientCredits(message: String = "Crediti insufficienti", cause: Throwable? = null) : AppError(message, cause)
    class InvalidResponse(message: String = "Risposta non valida dal provider", cause: Throwable? = null) : AppError(message, cause)
    class ModelMissing(message: String = "Modello non disponibile", cause: Throwable? = null) : AppError(message, cause)
    class Truncated(message: String = "Risposta troncata dal modello", cause: Throwable? = null) : AppError(message, cause)
    class Validation(message: String, cause: Throwable? = null) : AppError(message, cause)
    class NotFound(message: String = "Elemento non trovato", cause: Throwable? = null) : AppError(message, cause)
    class Unknown(message: String = "Errore imprevisto", cause: Throwable? = null) : AppError(message, cause)
}

inline fun <T> runCatchingApp(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (t: AppError) {
    AppResult.Failure(t)
} catch (t: Throwable) {
    AppResult.Failure(AppError.Unknown(t.message ?: "Errore imprevisto", t))
}

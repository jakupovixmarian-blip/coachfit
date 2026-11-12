package dev.fitcoach.app.domain.repository

/** Exports and imports the locally stored data as a JSON snapshot. */
interface BackupRepository {
    suspend fun exportSnapshot(): String
    suspend fun importSnapshot(content: String): Result<Int>
}

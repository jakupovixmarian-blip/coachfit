package com.fitcoach.app.notifications

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avviso di fine recupero con l'app in primo piano.
 *
 * La notifica del canale "Recupero allenamento" copre il caso in background, ma
 * dipende dal permesso notifiche e dal fatto che l'utente non abbia silenziato
 * il canale. Con l'app aperta l'utente sta guardando il cronometro: li' l'avviso
 * deve essere immediato, quindi suono e vibrazione vengono emessi direttamente.
 */
@Singleton
class RestAlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Emette l'avviso rispettando le preferenze dell'utente. Un errore del
     * dispositivo (audio occupato, vibrazione assente) non deve interrompere
     * il cronometro: viene registrato e ignorato.
     */
    fun play(sound: Boolean, vibration: Boolean) {
        if (vibration) vibrate()
        if (sound) tone()
    }

    private fun vibrate() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            val pattern = longArrayOf(0, 400, 200, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }.onFailure { Log.w(TAG, "Vibrazione non disponibile", it) }
    }

    private fun tone() {
        runCatching {
            // Tre bip brevi: udibili anche con la musica a volume basso, senza
            // dipendere dal volume delle notifiche che molti tengono a zero.
            val generator = ToneGenerator(AudioManager.STREAM_ALARM, TONE_VOLUME)
            repeat(3) {
                generator.startTone(ToneGenerator.TONE_PROP_BEEP2, TONE_MS)
                Thread.sleep((TONE_MS + 120).toLong())
            }
            generator.release()
        }.onFailure { Log.w(TAG, "Avviso sonoro non disponibile", it) }
    }

    private companion object {
        const val TAG = "RestAlarmPlayer"
        const val TONE_VOLUME = 90
        const val TONE_MS = 220
    }
}

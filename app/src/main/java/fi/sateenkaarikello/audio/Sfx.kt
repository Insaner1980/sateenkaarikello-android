package fi.sateenkaarikello.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Äänitehosteet. PCM syntetisoidaan kerran taustasäikeessä ja soitetaan AudioTrackilla
 * (MODE_STATIC). Äänet päällä/pois -asetus tarkistetaan AppViewModelissa.
 */
class Sfx {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracks = mutableMapOf<Sound, AudioTrack>()

    init {
        scope.launch {
            for (sound in Sound.entries) {
                runCatching { createTrack(ToneSynth.render(sound)) }
                    .onSuccess { synchronized(tracks) { tracks[sound] = it } }
                    .onFailure { Log.w(TAG, "Ääntä $sound ei voitu luoda", it) }
            }
        }
    }

    private fun createTrack(pcm: ShortArray): AudioTrack {
        val track =
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(ToneSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    fun play(sound: Sound) {
        val track = synchronized(tracks) { tracks[sound] } ?: return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }.onFailure { Log.w(TAG, "Ääntä $sound ei voitu soittaa", it) }
    }

    /** Pysäyttää kaikki äänet (kutsutaan onPause-vaiheessa). */
    fun stopAll() {
        synchronized(tracks) { tracks.values.toList() }.forEach { runCatching { it.stop() } }
    }

    private companion object {
        const val TAG = "Sfx"
    }
}

package fi.sateenkaarikello

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.datastore.preferences.preferencesDataStore
import fi.sateenkaarikello.audio.Sfx
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.audio.Speech
import fi.sateenkaarikello.data.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "sateenkaarikello")

/**
 * Palvelut, joita näkymämallit käyttävät. Testeissä voi antaa oman toteutuksen.
 * Vakaa: toteutukset pitävät vain muuttumattomia viittauksia, ja muuttuva tila kulkee StateFlow-virroilla.
 */
@Stable
interface AppServices {
    val repository: ProgressRepository
    val speechAvailable: StateFlow<Boolean>

    /** Soittaa äänen, jos äänet ovat päällä. [force] soittaa aina (äänten kytkeminen päälle). */
    fun play(
        sound: Sound,
        force: Boolean = false,
    )

    fun speak(text: String)
}

/** Sovelluksen ainoat pitkäikäiset oliot: tallennus, äänet ja puhe. */
class AppContainer(
    context: Context,
) : AppServices {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val repository = ProgressRepository(context.dataStore)
    private val sfx = Sfx()
    private val soundOn = repository.settings.map { it.sound }.stateIn(scope, SharingStarted.Eagerly, true)
    private val speech = Speech(context)
    override val speechAvailable: StateFlow<Boolean> = speech.available

    override fun play(
        sound: Sound,
        force: Boolean,
    ) {
        if (force || soundOn.value) sfx.play(sound)
    }

    override fun speak(text: String) = speech.speak(text)

    /** Äänet ja puhe pysähtyvät, kun sovellus menee taustalle. */
    fun pause() {
        sfx.stopAll()
        speech.stop()
    }
}

class SateenkaariApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

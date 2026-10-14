package fi.sateenkaarikello.ui

import fi.sateenkaarikello.AppServices
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.data.InMemoryPreferencesStore
import fi.sateenkaarikello.data.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Testipalvelut: muistissa oleva tallennus ja äänien kirjaus. */
class FakeServices : AppServices {
    override val repository = ProgressRepository(InMemoryPreferencesStore())
    override val speechAvailable: StateFlow<Boolean> = MutableStateFlow(false)
    val sounds = mutableListOf<Sound>()
    val spoken = mutableListOf<String>()

    override fun play(
        sound: Sound,
        force: Boolean,
    ) {
        sounds += sound
    }

    override fun speak(text: String) {
        spoken += text
    }
}

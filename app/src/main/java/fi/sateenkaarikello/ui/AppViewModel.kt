package fi.sateenkaarikello.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.sateenkaarikello.AppServices
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.Item
import fi.sateenkaarikello.domain.Lessons
import fi.sateenkaarikello.domain.Progress
import fi.sateenkaarikello.domain.Settings
import fi.sateenkaarikello.domain.Wardrobe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Koko sovelluksen tila: edistyminen ja asetukset (DataStoresta), äänet ja puhe.
 * Arvot ovat null, kunnes tallennus on luettu ensimmäisen kerran.
 *
 * One application facade exposes the existing settings, progress, and audio actions.
 *
 * Vakaa: viittaus ei vaihdu, ja muuttuva tila välitetään StateFlow-virroilla.
 */
@Stable
@Suppress("TooManyFunctions")
class AppViewModel(
    val services: AppServices,
) : ViewModel() {
    val progress: StateFlow<Progress?> =
        services.repository.progress.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings: StateFlow<Settings?> =
        services.repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val speechAvailable: StateFlow<Boolean> = services.speechAvailable

    private val currentSettings get() = settings.value ?: Settings()
    private val currentProgress get() = progress.value ?: Progress()

    fun play(sound: Sound) = services.play(sound)

    fun speak(text: String) = services.speak(text)

    /** Web: äänen kytkeminen päälle kuittaa poksahduksella. */
    fun toggleSound() {
        val on = !currentSettings.sound
        updateSettings { it.copy(sound = on) }
        if (on) services.play(Sound.POP, force = true)
    }

    fun toggleHelp() = updateSettings { it.copy(help = !it.help) }

    fun setH24(h24: Boolean) = updateSettings { it.copy(h24 = h24) }

    private fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch { services.repository.updateSettings(transform) }
    }

    fun updateProgress(transform: (Progress) -> Progress) {
        viewModelScope.launch { services.repository.updateProgress(transform) }
    }

    fun resetProgress() {
        viewModelScope.launch { services.repository.resetProgress() }
    }

    fun toggleItem(item: Item) = updateProgress { it.copy(outfit = Wardrobe.toggle(it.outfit, item)) }

    fun setDollName(name: String) = updateProgress { it.copy(dollName = name) }

    /** Pelaa-napin painallus: palauttaa ennen tasoa näytettävät oppitunnit ja merkitsee ne näytetyiksi. */
    fun startLevel(level: Int): List<String> {
        val plan = Lessons.introPlan(level, currentSettings.h24, currentProgress)
        updateProgress { it.copy(introShown = plan.introShown) }
        return plan.queue
    }
}

val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> { error("AppViewModel puuttuu") }

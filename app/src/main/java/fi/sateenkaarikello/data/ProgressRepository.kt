package fi.sateenkaarikello.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.domain.Progress
import fi.sateenkaarikello.domain.Settings
import fi.sateenkaarikello.domain.Slot
import fi.sateenkaarikello.domain.Wardrobe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Edistyminen ja asetukset Preferences DataStoressa. Viallinen tai tuntematon arvo
 * korvataan oletuksella, jotta sovellus ei koskaan kaadu tallennukseen.
 */
class ProgressRepository(
    private val store: DataStore<Preferences>,
) {
    private val data: Flow<Preferences> =
        store.data.catch { e ->
            if (e is IOException) {
                Log.w(TAG, "Tallennusta ei voitu lukea", e)
                emit(emptyPreferences())
            } else {
                throw e
            }
        }

    val progress: Flow<Progress> = data.map { it.toProgress() }.distinctUntilChanged()
    val settings: Flow<Settings> = data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun updateProgress(transform: (Progress) -> Progress) {
        store.edit { prefs -> prefs.write(transform(prefs.toProgress())) }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        store.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[SOUND] = s.sound
            prefs[HELP] = s.help
            prefs[H24] = s.h24
        }
    }

    /** "Aloita alusta": edistyminen nollataan, asetukset säilyvät. */
    suspend fun resetProgress() {
        store.edit { it.write(Progress()) }
    }

    private fun Preferences.toSettings() =
        Settings(
            sound = this[SOUND] ?: true,
            help = this[HELP] ?: true,
            h24 = this[H24] ?: false,
        )

    private fun Preferences.toProgress(): Progress {
        val defaults = Progress()
        return Progress(
            stars = STARS.map { (this[it] ?: 0).coerceIn(0, MAX_LEVEL_STARS) },
            challengeBest = (this[BEST] ?: 0).coerceAtLeast(0),
            outfit = readOutfit(),
            dollName = this[NAME] ?: defaults.dollName,
            lessonsDone = this[DONE] ?: emptySet(),
            introShown = this[INTRO] ?: emptySet(),
        )
    }

    private fun Preferences.readOutfit(): Outfit {
        var outfit = Outfit.Default
        for (slot in Slot.entries) {
            val id = this[outfitKey(slot)] ?: continue
            val item = Wardrobe.items.firstOrNull { it.id == id && it.slot == slot }
            outfit =
                when {
                    item != null -> outfit.with(slot, item.id)
                    slot.optional -> outfit.with(slot, null)
                    else -> outfit
                }
        }
        return outfit
    }

    private fun MutablePreferences.write(p: Progress) {
        STARS.forEachIndexed { i, key -> this[key] = p.stars[i] }
        this[BEST] = p.challengeBest
        for (slot in Slot.entries) this[outfitKey(slot)] = p.outfit[slot].orEmpty()
        this[NAME] = p.dollName
        this[DONE] = p.lessonsDone
        this[INTRO] = p.introShown
    }

    private companion object {
        const val TAG = "ProgressRepository"
        val STARS = List(Progress.LEVEL_COUNT) { intPreferencesKey("stars_$it") }
        val BEST = intPreferencesKey("challenge_best")
        val NAME = stringPreferencesKey("doll_name")
        val DONE = stringSetPreferencesKey("lessons_done")
        val INTRO = stringSetPreferencesKey("intro_shown")
        val SOUND = booleanPreferencesKey("sound")
        val HELP = booleanPreferencesKey("help")
        val H24 = booleanPreferencesKey("h24")
        private val OUTFIT = Slot.entries.associateWith { stringPreferencesKey("outfit_${it.name.lowercase()}") }

        fun outfitKey(slot: Slot) = OUTFIT.getValue(slot)
    }
}

private const val MAX_LEVEL_STARS = 3

package fi.sateenkaarikello.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.domain.Progress
import fi.sateenkaarikello.domain.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepositoryTest {
    private val scope = TestScope(StandardTestDispatcher())
    private val store = InMemoryPreferencesStore()
    private val repository = ProgressRepository(store)

    @Test
    fun `empty store gives defaults`() =
        scope.runTest {
            assertEquals(Progress(), repository.progress.first())
            assertEquals(Settings(), repository.settings.first())
        }

    @Test
    fun `progress round trips`() =
        scope.runTest {
            val p =
                Progress(
                    stars = listOf(3, 2, 1, 0, 0),
                    challengeBest = 12,
                    outfit = Outfit.Default.copy(dress = "dress-tahti", head = "head-kruunu"),
                    dollName = "Ruusa",
                    lessonsDone = setOf("viisarit", "puoli"),
                    introShown = setOf("0", "h24"),
                )
            repository.updateProgress { p }
            assertEquals(p, repository.progress.first())
        }

    @Test
    fun `settings round trip`() =
        scope.runTest {
            repository.updateSettings { it.copy(sound = false, h24 = true) }
            assertEquals(Settings(sound = false, help = true, h24 = true), repository.settings.first())
        }

    @Test
    fun `reset clears progress but keeps settings`() =
        scope.runTest {
            repository.updateSettings { it.copy(help = false, h24 = true) }
            repository.updateProgress { it.withLevelResult(0, 3).copy(dollName = "Ruusa") }
            repository.resetProgress()
            assertEquals(Progress(), repository.progress.first())
            assertEquals(Settings(help = false, h24 = true), repository.settings.first())
        }

    @Test
    fun `unknown outfit ids fall back to defaults`() =
        scope.runTest {
            store.edit {
                it[stringPreferencesKey("outfit_dress")] = "dress-olematon"
                it[stringPreferencesKey("outfit_head")] = "hair-letit"
            }
            val outfit = repository.progress.first().outfit
            assertEquals(Outfit.Default.dress, outfit.dress)
            assertEquals(null, outfit.head)
        }
}

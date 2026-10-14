package fi.sateenkaarikello

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Näkymien avaimet Navigation 3 -paluupinoon. Pino tallentuu myös prosessin kuollessa. */
@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Home : Screen

    @Serializable data object FreeClock : Screen

    @Serializable data object LessonList : Screen

    /** Oppituntijono. Kun [thenLevel] on annettu, jono päättyy tason aloitukseen. */
    @Immutable
    @Serializable data class Lesson(
        val queue: List<String>,
        val thenLevel: Int? = null,
    ) : Screen

    @Serializable data object Levels : Screen

    /** [round] erottaa saman tason peräkkäiset pelikerrat, jotta jokainen saa oman tilansa. */
    @Serializable data class Game(
        val level: Int,
        val round: Long = 0,
    ) : Screen

    /** Pelin tulos. [starsBefore] on kokonaistähtimäärä ennen tätä peliä (uusien vaatteiden laskentaan). */
    @Serializable data class Result(
        val level: Int,
        val stars: Int,
        val previousBest: Int,
        val starsBefore: Int,
    ) : Screen

    @Serializable data object Challenge : Screen

    @Serializable data object Ask : Screen

    @Serializable data object Doll : Screen
}

/** Paluupinon sarjallistus ilman reflektiota (toimii myös R8:n kanssa). */
val ScreenSavedState =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Screen.Home::class, Screen.Home.serializer())
                    subclass(Screen.FreeClock::class, Screen.FreeClock.serializer())
                    subclass(Screen.LessonList::class, Screen.LessonList.serializer())
                    subclass(Screen.Lesson::class, Screen.Lesson.serializer())
                    subclass(Screen.Levels::class, Screen.Levels.serializer())
                    subclass(Screen.Game::class, Screen.Game.serializer())
                    subclass(Screen.Result::class, Screen.Result.serializer())
                    subclass(Screen.Challenge::class, Screen.Challenge.serializer())
                    subclass(Screen.Ask::class, Screen.Ask.serializer())
                    subclass(Screen.Doll::class, Screen.Doll.serializer())
                }
            }
    }

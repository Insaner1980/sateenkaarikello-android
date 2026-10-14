package fi.sateenkaarikello

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.Lessons
import fi.sateenkaarikello.ui.AppViewModel
import fi.sateenkaarikello.ui.LocalAppViewModel
import fi.sateenkaarikello.ui.ask.AskScreen
import fi.sateenkaarikello.ui.challenge.ChallengeRoute
import fi.sateenkaarikello.ui.components.ConfettiHost
import fi.sateenkaarikello.ui.doll.DollScreen
import fi.sateenkaarikello.ui.freeclock.FreeClockScreen
import fi.sateenkaarikello.ui.game.GameRoute
import fi.sateenkaarikello.ui.game.ResultScreen
import fi.sateenkaarikello.ui.home.HomeDestination
import fi.sateenkaarikello.ui.home.HomeScreen
import fi.sateenkaarikello.ui.intro.IntroOverlay
import fi.sateenkaarikello.ui.lesson.LessonListScreen
import fi.sateenkaarikello.ui.lesson.LessonRoute
import fi.sateenkaarikello.ui.levels.LevelsScreen
import fi.sateenkaarikello.ui.theme.SateenkaariTheme
import fi.sateenkaarikello.ui.theme.dotBackground

@Composable
// Keep all Navigation 3 route bindings visible in one declarative graph.
@Suppress("LongMethod")
fun SateenkaariApp(
    modifier: Modifier = Modifier,
    container: AppServices = (LocalContext.current.applicationContext as SateenkaariApplication).container,
    app: AppViewModel = viewModel(factory = viewModelFactory { initializer { AppViewModel(container) } }),
) {
    SateenkaariTheme {
        Box(modifier.fillMaxSize().dotBackground()) {
            val progress by app.progress.collectAsStateWithLifecycle()
            val settings by app.settings.collectAsStateWithLifecycle()
            // Tallennus luetaan muutamassa millisekunnissa; siihen asti näytetään pelkkä tausta.
            val p = progress ?: return@Box
            val s = settings ?: return@Box
            val speech by app.speechAvailable.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppViewModel provides app) {
                ConfettiHost {
                    val backStack = rememberNavBackStack(ScreenSavedState, Screen.Home)
                    val nav = remember(backStack) { Navigator(backStack) }
                    val go = { screen: Screen ->
                        app.play(Sound.POP)
                        nav.push(screen)
                    }
                    val back = {
                        app.play(Sound.POP)
                        nav.pop()
                    }
                    val playLevel = { level: Int, replaceTop: Boolean ->
                        app.play(Sound.POP)
                        nav.startLevel(level, app.startLevel(level), replaceTop)
                    }
                    NavDisplay(
                        backStack = backStack,
                        onBack = { nav.pop() },
                        entryDecorators =
                            listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                        entryProvider =
                            entryProvider {
                                entry<Screen.Home> {
                                    HomeScreen(p, s, onOpen = { destination ->
                                        go(
                                            when (destination) {
                                                HomeDestination.KOULU -> Screen.LessonList
                                                HomeDestination.VAPAA -> Screen.FreeClock
                                                HomeDestination.PELAA -> Screen.Levels
                                                HomeDestination.KYSY -> Screen.Ask
                                                HomeDestination.NUKKE -> Screen.Doll
                                            },
                                        )
                                    })
                                }
                                entry<Screen.FreeClock> { FreeClockScreen(s.h24, s.help, speech, back) }
                                entry<Screen.Ask> { AskScreen(s.h24, s.help, speech, back) }
                                entry<Screen.Doll> { DollScreen(p, back) }
                                entry<Screen.LessonList> {
                                    LessonListScreen(
                                        s.h24,
                                        p.lessonsDone,
                                        onOpen = { go(Screen.Lesson(listOf(it))) },
                                        onBack = back,
                                    )
                                }
                                entry<Screen.Lesson> { key ->
                                    LessonRoute(
                                        key = key,
                                        speechAvailable = speech,
                                        onLeave = { nav.pop() },
                                        onStartGame = { level ->
                                            nav.replace(Screen.Game(level, round = System.nanoTime()))
                                        },
                                    )
                                }
                                entry<Screen.Levels> {
                                    LevelsScreen(
                                        progress = p,
                                        onPlay = { playLevel(it, false) },
                                        onLesson = { go(Screen.Lesson(Lessons.levelLessons[it])) },
                                        onChallenge = { go(Screen.Challenge) },
                                        onBack = back,
                                    )
                                }
                                entry<Screen.Game> { key ->
                                    GameRoute(key, s.h24, s.help, onBack = back, onFinish = { nav.replace(it) })
                                }
                                entry<Screen.Result> { key ->
                                    ResultScreen(
                                        result = key,
                                        outfit = p.outfit,
                                        onPlayAgain = {
                                            app.play(Sound.POP)
                                            nav.replace(Screen.Game(key.level, round = System.nanoTime()))
                                        },
                                        onNextLevel = { playLevel(key.level + 1, true) },
                                        onDress = {
                                            app.play(Sound.POP)
                                            nav.replace(Screen.Doll)
                                        },
                                        onLevels = {
                                            app.play(Sound.POP)
                                            nav.popTo(Screen.Levels)
                                        },
                                    )
                                }
                                entry<Screen.Challenge> { ChallengeRoute(s.h24, s.help, back) }
                            },
                    )
                    // Aloitusnäkymä jokaisella käynnistyksellä; ei palaa näkyviin, kun näkymä luodaan uudelleen.
                    var introDone by rememberSaveable { mutableStateOf(false) }
                    if (!introDone) {
                        IntroOverlay(p.outfit, onTick = { app.play(Sound.TICK) }, onDone = { introDone = true })
                    }
                }
            }
        }
    }
}

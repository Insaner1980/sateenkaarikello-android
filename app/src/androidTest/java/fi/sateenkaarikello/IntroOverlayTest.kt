package fi.sateenkaarikello

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.ui.intro.IntroOverlay
import fi.sateenkaarikello.ui.intro.coveredByIntro
import fi.sateenkaarikello.ui.isReduceMotion
import fi.sateenkaarikello.ui.theme.SateenkaariTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntroOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun tickPlaysOnceWhenTheHandsStopAndTheIntroFinishes() {
        assumeFalse(isReduceMotion(InstrumentationRegistry.getInstrumentation().targetContext))
        var ticks = 0
        var ticksAtDone = -1
        compose.setContent {
            SateenkaariTheme {
                IntroOverlay(Outfit.Default, onTick = { ticks++ }, onDone = { ticksAtDone = ticks })
            }
        }
        compose.waitUntil(10_000) { ticksAtDone >= 0 }
        assertEquals(1, ticksAtDone)
    }

    @Test
    fun tapSkipsTheIntro() {
        var done = 0
        // Kello pysäytetään ennen koostamista, jotta intro ei ehdi päättyä itsestään.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SateenkaariTheme { IntroOverlay(Outfit.Default, onTick = {}, onDone = { done++ }) }
        }
        compose.onNodeWithContentDescription("Sateenkaarikello", substring = true).performClick()
        compose.runOnIdle { assertEquals(1, done) }
    }

    @Test
    fun contentUnderTheIntroIsHiddenFromAccessibilityUntilItEnds() {
        var covered by mutableStateOf(true)
        compose.setContent {
            Box(Modifier.coveredByIntro(covered)) {
                Button(onClick = {}) { Text("Pelaa") }
            }
        }
        compose.onNodeWithText("Pelaa").assertDoesNotExist()
        covered = false
        compose.onNodeWithText("Pelaa").assertExists()
    }
}

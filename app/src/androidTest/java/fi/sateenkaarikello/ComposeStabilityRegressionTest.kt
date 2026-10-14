package fi.sateenkaarikello

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.sateenkaarikello.ui.challenge.ChallengePhase
import fi.sateenkaarikello.ui.challenge.ChallengeScreen
import fi.sateenkaarikello.ui.challenge.ChallengeUiState
import fi.sateenkaarikello.ui.components.ConfettiState
import fi.sateenkaarikello.ui.components.LocalConfetti
import fi.sateenkaarikello.ui.theme.SateenkaariTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeStabilityRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun recordConfettiUsesScoreAnchorAndDoesNotRepeatOnLayout() {
        val state = mutableStateOf(ChallengeUiState(phase = ChallengePhase.END, score = 12, best = 10))
        val confetti = ConfettiState(density = 1f, enabled = true)
        compose.setContent {
            SateenkaariTheme {
                CompositionLocalProvider(LocalConfetti provides confetti) {
                    ChallengeScreen(state.value, h24 = false, help = false, onBack = {}, onStart = {}, onPick = {})
                }
            }
        }
        val score = compose.onNodeWithText("12").performScrollTo().assertIsDisplayed()
        val bounds = score.getUnclippedBoundsInRoot()
        val density =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.displayMetrics.density
        compose.runOnIdle { state.value = state.value.copy(newRecord = true) }
        compose.runOnIdle {
            assertEquals(120, confetti.particles.size)
            confetti.particles.forEach { particle ->
                assertEquals((bounds.left.value + bounds.right.value) / 2f * density, particle.x, 1f)
                assertEquals((bounds.top.value + bounds.bottom.value) / 2f * density, particle.y, 1f)
            }
        }
        compose.onNodeWithText("Tasot").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(120, confetti.particles.size) }
    }
}

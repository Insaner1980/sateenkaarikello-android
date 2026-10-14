package fi.sateenkaarikello

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.sateenkaarikello.ui.challenge.ChallengePhase
import fi.sateenkaarikello.ui.challenge.ChallengeScreen
import fi.sateenkaarikello.ui.challenge.ChallengeUiState
import fi.sateenkaarikello.ui.components.OptionState
import fi.sateenkaarikello.ui.theme.SateenkaariTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChallengeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun challengeShowsOptionsDisablesAnswersAndOffersReplay() {
        val state = mutableStateOf(ChallengeUiState())
        var starts = 0
        var chosen = -1
        var backs = 0
        compose.setContent {
            SateenkaariTheme {
                ChallengeScreen(
                    state = state.value,
                    h24 = true,
                    help = true,
                    onBack = { backs++ },
                    onStart = { starts++ },
                    onPick = { chosen = it },
                )
            }
        }
        compose.onNodeWithText("EI VIELÄ ENNÄTYSTÄ").assertExists()
        compose.onNodeWithText("Aloita!").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, starts)
            state.value = state.value.copy(phase = ChallengePhase.RUN, options = listOf(180, 240, 300))
        }
        compose.onNodeWithText("3.00").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(180, chosen)
            state.value =
                state.value.copy(
                    busy = true,
                    optionStates = mapOf(180 to OptionState.WRONG, 240 to OptionState.CORRECT),
                )
        }
        compose.onNodeWithText("3.00").assertIsNotEnabled()
        compose.onNodeWithText("4.00").assertIsNotEnabled()
        compose.runOnIdle {
            state.value = state.value.copy(phase = ChallengePhase.END, score = 2, best = 5)
        }
        compose.onNodeWithText("Sait 2 oikein. Ennätys on 5.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Uudestaan").performScrollTo().performClick()
        compose.onNodeWithText("Tasot").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(2, starts)
            assertEquals(1, backs)
            state.value = ChallengeUiState(best = 5)
        }
        compose.onNodeWithText("ENNÄTYS: 5 OIKEIN").assertExists()
    }
}

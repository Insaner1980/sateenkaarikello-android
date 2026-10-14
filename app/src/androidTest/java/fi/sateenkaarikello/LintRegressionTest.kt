package fi.sateenkaarikello

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.game.ResultScreen
import fi.sateenkaarikello.ui.resources
import fi.sateenkaarikello.ui.theme.SateenkaariTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LintRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun resourceReaderTracksCompositionResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val initial =
            context
                .createConfigurationContext(
                    Configuration(context.resources.configuration).apply { fontScale = 1f },
                ).resources
        val enlarged =
            context
                .createConfigurationContext(
                    Configuration(context.resources.configuration).apply { fontScale = 2f },
                ).resources
        val current = mutableStateOf(initial)
        compose.setContent {
            CompositionLocalProvider(LocalResources provides current.value) {
                Text("fontScale=${resources().configuration.fontScale}")
            }
        }
        compose.onNodeWithText("fontScale=1.0").assertExists()
        compose.runOnIdle { current.value = enlarged }
        compose.onNodeWithText("fontScale=2.0").assertExists()
    }

    @Test
    fun resultUsesCorrectStarFormsAndKeepsActionsReachable() {
        val stars = mutableIntStateOf(1)
        var replayed = false
        compose.setContent {
            SateenkaariTheme {
                ResultScreen(
                    result = Screen.Result(level = 0, stars = stars.intValue, previousBest = 3, starsBefore = 15),
                    outfit = Outfit.Default,
                    onPlayAgain = { replayed = true },
                    onNextLevel = {},
                    onDress = {},
                    onLevels = {},
                )
            }
        }
        compose.onNodeWithContentDescription("1 tähti kolmesta").assertExists()
        compose.onNodeWithText("Sait 1 tähden.", substring = true).assertExists()
        compose.mainClock.advanceTimeBy(1_000)
        saveResult()
        compose.onNodeWithText("Seuraava taso").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tasot").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText("Pelaa uudestaan")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle {
            assertTrue(replayed)
            stars.intValue = 2
        }
        compose.onNodeWithContentDescription("2 tähteä kolmesta").assertExists()
        compose.onNodeWithText("Sait 2 tähteä.", substring = true).assertExists()
        val res = InstrumentationRegistry.getInstrumentation().targetContext.resources
        assertEquals(
            "Paras tuloksesi tästä tasosta on 1 tähti.",
            res.getQuantityString(R.plurals.result_previous_best, 1, 1),
        )
        assertEquals(
            "Paras tuloksesi tästä tasosta on 2 tähteä.",
            res.getQuantityString(R.plurals.result_previous_best, 2, 2),
        )
    }

    @Test
    fun minuteHandDragStillMovesTheClock() {
        var selectedTime = 180
        compose.setContent {
            SateenkaariTheme {
                ClockFace(
                    command = ClockCommand(180),
                    modifier = Modifier.size(240.dp).testTag("clock"),
                    interactive = true,
                    onChange = { selectedTime = it },
                )
            }
        }
        compose.onNodeWithTag("clock").performTouchInput {
            val radius = width * 0.27f
            down(Offset(center.x, center.y - radius))
            moveTo(Offset(center.x + radius, center.y))
            up()
        }
        compose.runOnIdle { assertEquals(195, selectedTime) }
    }

    private fun saveResult() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = InstrumentationRegistry.getArguments().getString("validationCell", "normal")
        val directory = File(context.filesDir, "lint-regression").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "result-$label.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

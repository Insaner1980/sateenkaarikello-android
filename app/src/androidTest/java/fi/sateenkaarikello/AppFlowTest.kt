package fi.sateenkaarikello

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/**
 * Koko sovelluksen polut emulaattorilla. Oikeat vastaukset luetaan kellon saavutettavuustekstistä,
 * joten testi ei riipu satunnaisuudesta.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    private val freshData =
        object : ExternalResource() {
            override fun before() {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                File(context.filesDir, "datastore").deleteRecursively()
            }
        }
    private val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val LEVEL_CARDS = 6
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(freshData).around(compose)

    private fun waitForText(
        text: String,
        substring: Boolean = false,
        timeout: Long = 10_000,
    ) {
        compose.waitUntil(timeout) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(text: String) {
        waitForText(text)
        compose.onAllNodes(hasText(text) and hasClickAction())[0].performScrollTo().performClick()
    }

    private fun clockWords(): String {
        val node = compose.onNode(hasContentDescription("Kellotaulu:", substring = true)).fetchSemanticsNode()
        return node.config[SemanticsProperties.ContentDescription].first().removePrefix("Kellotaulu: ")
    }

    /** Asetus tallentuu DataStoreen taustasäikeessä, joten valintaa odotetaan eikä sitä tarkisteta heti. */
    private fun waitUntilSelected(text: String) {
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(hasText(text) and hasClickAction())
                .fetchSemanticsNodes()
                .any { it.config.getOrElse(SemanticsProperties.Selected) { false } }
        }
    }

    private fun isShown(
        text: String,
        substring: Boolean = false,
    ) = compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun playTasatunnitThenDressTheDollAndSurviveRecreate() {
        click("Pelaa")
        waitForText("Valitse taso")
        click("Pelaa")
        click("Ohita ja pelaa")
        var wrongDone = false
        repeat(10) { task ->
            compose.waitUntil(5_000) { isShown("Mitä kello on?") || isShown("Laita kello aikaan") }
            compose.waitForIdle()
            if (isShown("Mitä kello on?")) {
                val answer = clockWords().replaceFirstChar { it.uppercase() }
                if (!wrongDone) {
                    val wrong =
                        compose
                            .onAllNodes(hasText("Tasan", substring = true) and hasClickAction())
                            .fetchSemanticsNodes()
                            .map { it.config[SemanticsProperties.Text].first().text }
                            .first { it != answer }
                    click(wrong)
                    waitForText("Katso lyhyttä punaista viisaria", substring = true)
                    wrongDone = true
                }
                click(answer)
            } else {
                val target =
                    compose
                        .onAllNodesWithText("Tasan", substring = true)
                        .fetchSemanticsNodes()
                        .map { it.config[SemanticsProperties.Text].first().text }
                        .first()
                        .lowercase()
                var guard = 0
                while (clockWords() != target && guard++ < 12) {
                    compose
                        .onNode(hasContentDescription("Kellotaulu:", substring = true))
                        .performCustomAccessibilityActionWithLabel("Lisää 60 minuuttia")
                    compose.waitForIdle()
                }
                assertTrue("tehtävä $task: kello ${clockWords()} != $target", clockWords() == target)
                click("Valmis")
            }
            // Onnistumisen jälkeen peli jatkuu 1,5 sekunnin kuluttua.
            Thread.sleep(1_800)
        }
        waitForText("Tasatunnit läpi!", substring = true)
        assertTrue(isShown("Sait 3 tähteä", substring = true))
        assertTrue(isShown("Pilkkumekko"))

        click("Pue nukke")
        waitForText("Nuken vaatekaappi")
        click("Pilkkumekko")
        compose.waitUntil(5_000) { isShown("Päällä") && pilkkumekkoSelected() }

        compose.activityRule.scenario.recreate()
        waitForText("Nuken vaatekaappi")
        compose.waitUntil(5_000) { pilkkumekkoSelected() }
        compose.onAllNodes(hasText("Pilkkumekko") and hasClickAction())[0].assertIsSelected()
    }

    private fun pilkkumekkoSelected(): Boolean =
        compose
            .onAllNodes(hasText("Pilkkumekko") and hasClickAction())
            .fetchSemanticsNodes()
            .any { it.config.getOrElse(SemanticsProperties.Selected) { false } }

    @Test
    fun freeClockAndSisterQuizKeepSettingsAcrossRecreation() {
        click("24 h")
        waitUntilSelected("24 h")
        click("Äänet päällä")
        waitForText("Äänet pois")
        click("Vapaa kello")
        waitForText("KELLO ON")
        assertTrue(clockWords().contains("kolme"))
        compose
            .onNode(hasContentDescription("Kellotaulu:", substring = true))
            .performCustomAccessibilityActionWithLabel("Lisää 5 minuuttia")
        compose.onNodeWithText("kello 15.05").assertExists()
        click("Arvo aika")
        click("Apu päällä")
        waitForText("Apu pois")
        compose.activityRule.scenario.recreate()
        waitForText("Apu pois")
        click("← Takaisin")
        click("Kysy siskolta")
        click("Arvo aika")
        click("Kysy!")
        waitForText("Mitä kello on?")
        click("Näytä vastaus")
        compose.onNodeWithText("Uusi kysymys").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("kello ", substring = true) and !hasText("Mitä kello on?")).assertExists()
        click("Uusi kysymys")
        waitForText("Laita kello johonkin aikaan")
        click("← Takaisin")
        click("12 h")
        waitUntilSelected("12 h")
        click("Aloita alusta")
        click("Varmasti? Tähdet ja vaatteet nollataan. Paina uudestaan")
        waitForText("Aloita alusta")
        compose.onNodeWithText("Äänet pois").assertExists()
    }

    @Test
    fun everyLevelAndTheChallengeArePlayableFromTheStart() {
        click("Pelaa")
        waitForText("Valitse taso")
        // Viisi tasoa ja Minuuttihaaste: kaikki Pelaa-napit toimivat ilman tähtiä.
        val play = compose.onAllNodes(hasText("Pelaa") and hasClickAction())
        compose.waitUntil(5_000) { play.fetchSemanticsNodes().size == LEVEL_CARDS }
        repeat(LEVEL_CARDS) { play[it].performScrollTo().assertIsEnabled() }
        play[LEVEL_CARDS - 1].performScrollTo().performClick()
        waitForText("Aloita!")
    }

    @Test
    fun passTheFirstLessonQuiz() {
        click("Kellokoulu")
        waitForText("Kaksi viisaria")
        click("Kaksi viisaria")
        click("Seuraava")
        click("Seuraava")
        click("Kokeile itse")
        waitForText("Kumpi viisari kertoo tunnit?")
        click("Pitkä sininen viisari")
        waitForText("Tuntiviisari on se lyhyempi ja punainen.")
        click("Lyhyt punainen viisari")
        waitForText("Osaat nyt oppitunnin Kaksi viisaria.", substring = true)
        click("Valmis")
        waitForText("Osattu")
        compose.onNodeWithText("Osattu").assertExists()
    }
}

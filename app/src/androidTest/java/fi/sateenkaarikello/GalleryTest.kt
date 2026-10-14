package fi.sateenkaarikello

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.sateenkaarikello.domain.Hand
import fi.sateenkaarikello.domain.Mark
import fi.sateenkaarikello.domain.MarkColor
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.domain.Wardrobe
import fi.sateenkaarikello.ui.clock.ClockCommand
import fi.sateenkaarikello.ui.clock.ClockFace
import fi.sateenkaarikello.ui.doll.Doll
import fi.sateenkaarikello.ui.theme.SateenkaariTheme
import fi.sateenkaarikello.ui.theme.SkColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Kuvagalleria silmämääräiseen vertailuun web-version kanssa: nukke jokaisessa vaatteessa ja
 * kello eri tiloissa. Kuvat tallentuvat laitteelle sovelluksen files/gallery-kansioon.
 */
@RunWith(AndroidJUnit4::class)
class GalleryTest {
    @get:Rule val compose = createComposeRule()

    private fun save(name: String) {
        val bitmap = compose.onNodeWithTag("galleria").captureToImage().asAndroidBitmap()
        val dir =
            File(
                InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
                "gallery",
            ).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun dollInEveryItem() {
        val outfits = Wardrobe.items.filter { it.star > 0 }.map { Outfit.Default.with(it.slot, it.id) } + Outfit.Default
        compose.setContent {
            SateenkaariTheme {
                Column(
                    Modifier.testTag("galleria").background(SkColors.Bg).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    outfits.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { Doll(it, Modifier.size(width = 90.dp, height = 153.dp)) }
                        }
                    }
                }
            }
        }
        save("nukke")
    }

    @Test
    fun clockVariants() {
        compose.setContent {
            SateenkaariTheme {
                Column(
                    Modifier.testTag("galleria").background(SkColors.Bg).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClockFace(ClockCommand(205), Modifier.size(190.dp), help = false)
                        ClockFace(ClockCommand(205), Modifier.size(190.dp), help = true, interactive = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClockFace(ClockCommand(930), Modifier.size(220.dp), period = 1440, help = true)
                        ClockFace(ClockCommand(1290), Modifier.size(220.dp), period = 1440, help = false)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClockFace(
                            ClockCommand(195),
                            Modifier.size(220.dp),
                            highlight = Hand.HOUR,
                            under = listOf(Mark.Sector(0, 15, MarkColor.HALF), Mark.NumberGlow(3), Mark.TickGlow(7)),
                            over = listOf(Mark.Bubble(5, "5"), Mark.Bubble(10, "10")),
                        )
                        ClockFace(ClockCommand(555), Modifier.size(120.dp), mini = true)
                    }
                }
            }
        }
        save("kello")
    }
}

// Literal SVG geometry and crop rectangles are source artwork data.
@file:Suppress("MagicNumber")

package fi.sateenkaarikello.ui.doll

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser
import fi.sateenkaarikello.domain.Slot

/** Nuken polut ja rajaukset web-version dollSVG:stä (koordinaatisto 200×340). */
internal object DollParts {
    const val WIDTH = 200f
    const val HEIGHT = 340f

    private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

    val Dress = p("M74 128Q100 120 126 128L136 162L158 236Q100 254 42 236L64 162Z")
    val LongHair = p("M52 76Q50 26 100 26Q150 26 148 76L156 196Q100 212 44 196Z")
    val Bangs = p("M55 84Q50 30 100 30Q150 30 145 84Q138 62 120 60Q112 50 100 58Q88 50 80 60Q62 62 55 84Z")
    val Collar = p("M86 126Q100 142 114 126")
    val Hem = p("M47 230Q100 246 153 230")
    val StarBelt = p("M65 162H135")
    val PrincessSkirt =
        p("M64 166L46 232Q57 246 70 236Q81 249 92 238Q100 251 108 238Q119 249 130 236Q143 246 154 232L136 166Z")
    val Mouth = p("M90 102Q100 111 110 102")
    val BowLeft = p("M128 40L110 28V52Z")
    val BowRight = p("M128 40L146 28V52Z")
    val Crown = p("M76 42L78 14L90 28L100 8L110 28L122 14L124 42Z")
    val BagStrap = p("M126 132Q150 160 152 200")
    val BagFlap = p("M138 206H168")

    /** Vaatekaapin pikkukuvien rajaukset (web: CROP). */
    fun crop(slot: Slot): Rect =
        when (slot) {
            Slot.HAIR -> Rect(26f, 4f, 26f + 148f, 4f + 148f)
            Slot.HEAD -> Rect(26f, 0f, 26f + 148f, 148f)
            Slot.DRESS -> Rect(28f, 108f, 28f + 144f, 108f + 150f)
            Slot.SHOES -> Rect(56f, 268f, 56f + 88f, 268f + 52f)
            Slot.EXTRA -> Rect(0f, 92f, 200f, 92f + 168f)
        }

    val Full = Rect(0f, 0f, WIDTH, HEIGHT)
}

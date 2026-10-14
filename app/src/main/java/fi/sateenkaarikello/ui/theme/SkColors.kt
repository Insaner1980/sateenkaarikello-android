package fi.sateenkaarikello.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Kaikki sovelluksen värit. Arvot tulevat web-version CSS:n :root-muuttujista ja tyyleistä.
 * Näkymät eivät koskaan kirjoita värejä suoraan, vaan käyttävät näitä.
 */
object SkColors {
    // Pinnat ja teksti
    val Bg = Color(0xFFFFF5FA)
    val Dots = Color(0xFFFFE1EF)
    val Surface = Color(0xFFFFFFFF)
    val Line = Color(0xFFF1DDEA)
    val Ink = Color(0xFF3A2453)
    val InkSoft = Color(0xFF76638C)
    val Shadow = Color(0xFF3A2453)

    // Viisarit
    val Hour = Color(0xFFE64980)
    val Minute = Color(0xFF228BE6)

    // Kulta
    val Gold = Color(0xFFFCC419)
    val GoldDeep = Color(0xFFF59F00)

    // Palaute
    val Good = Color(0xFF2B8A3E)
    val GoodBg = Color(0xFFE3F8E7)
    val Try = Color(0xFFB35000)
    val TryBg = Color(0xFFFFF0E0)

    // Kellon puoliskot
    val Yli = Color(0xFFFFF3BF)
    val Vaille = Color(0xFFDCEEFF)

    /** Kellon reunan 12 väriä. */
    val Rainbow =
        listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFF922B),
            Color(0xFFFCC419),
            Color(0xFF94D82D),
            Color(0xFF51CF66),
            Color(0xFF20C997),
            Color(0xFF22B8CF),
            Color(0xFF339AF0),
            Color(0xFF5C7CFA),
            Color(0xFF845EF7),
            Color(0xFFCC5DE8),
            Color(0xFFF06595),
        )

    /** Sateenkaaren 7 väriä: konfetti, edistymispallot, ajastin, hiukset ja mekko. */
    val Rainbow7 =
        listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFA94D),
            Color(0xFFFCC419),
            Color(0xFF51CF66),
            Color(0xFF339AF0),
            Color(0xFF845EF7),
            Color(0xFFDA77F2),
        )

    /** Otsikon kirjainten värit järjestyksessä. */
    val Logo =
        listOf(
            Color(0xFFF03E3E),
            Color(0xFFF76707),
            Color(0xFFE8A200),
            Color(0xFF2F9E44),
            Color(0xFF1C7ED6),
            Color(0xFF7048E8),
            Color(0xFFC2255C),
        )

    /** Otsikon kirjaimen [index] väri: värit kiertävät järjestyksessä. */
    fun logoColor(index: Int): Color = Logo[index % Logo.size]

    // Valikkonapit: täyttö ja tummempi varjo
    val KouluFill = Color(0xFFFFE8A3)
    val KouluShadow = Color(0xFFFCC419)
    val VapaaFill = Color(0xFFBFE3FF)
    val VapaaShadow = Color(0xFF74C0FC)
    val PelaaFill = Color(0xFFFFD1E3)
    val PelaaShadow = Color(0xFFF783AC)
    val KysyFill = Color(0xFFC3F2CB)
    val KysyShadow = Color(0xFF69DB7C)
    val NukkeFill = Color(0xFFE5DBFF)
    val NukkeShadow = Color(0xFFB197FC)
    val MenuDressBelt = Color(0xFFF06595)

    /** Etusivun sateenkaarikaaret ulkoa sisään. */
    val HeroArcs =
        listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFA94D),
            Color(0xFFFCC419),
            Color(0xFF51CF66),
            Color(0xFF339AF0),
            Color(0xFF845EF7),
        )

    /** Tasokorttien värit (Tasatunnit … Minuutit). */
    val LevelCards =
        listOf(
            Color(0xFFFFE3E3),
            Color(0xFFFFE8CC),
            Color(0xFFFFF3BF),
            Color(0xFFD3F9D8),
            Color(0xFFD0EBFF),
        )
    val ChallengeCard = Color(0xFFE5DBFF)

    // Napit
    val BtnFill = Color(0xFFF3EAF8)
    val BtnShadow = Color(0xFFDCCBE8)
    val BtnPrimaryFill = Color(0xFFFF8FB8)
    val BtnPrimaryShadow = Color(0xFFE64980)
    val BtnGreenFill = Color(0xFF8CE99A)
    val BtnGreenShadow = Color(0xFF40C057)

    // Sirut, pillerit ja pienet pinnat
    val Pressed = Color(0xFFFFF3BF)
    val PillMuted = Color(0xFFF1EAF5)
    val DigitalBg = Color(0xFFF1EAF5)
    val OptDigitalBg = Color(0xFFF6F0FA)
    val Legend = Color(0xFFFFF9E6)
    val DotEmpty = Color(0xFFEADDF0)
    val Caption = Color(0xFFFFF3BF)

    // Vastausvaihtoehdot
    val OptCorrectBorder = Color(0xFF51CF66)
    val OptCorrectShadow = Color(0xFF8CE99A)
    val OptWrongBorder = Color(0xFFFFA94D)
    val OptWrongShadow = Color(0xFFFFD8A8)

    // Tähdet ja lukko
    val StarOn = Color(0xFFFCC419)
    val StarOnStroke = Color(0xFFE67700)
    val StarOff = Color(0xFFE9DFF0)
    val StarOffStroke = Color(0xFFD6C8E0)
    val Lock = Color(0xFFB9A8C9)

    // Nukke ja vaatekaappi
    val DollStageTop = Color(0xFFFFF0F6)
    val DollStageBottom = Color(0xFFF3F0FF)
    val ItemOnBorder = Color(0xFFB197FC)
    val ItemOnBg = Color(0xFFF3F0FF)
    val ItemLockedBg = Color(0xFFF7F2FA)

    // Kellotaulu
    val Face = Color(0xFFFFFFFF)
    val Tick = Color(0xFFD3C6DE)
    val Hour24 = Color(0xFF9775FA)
    val Sun = Color(0xFFFCC419)
    val SunRay = Color(0xFFF59F00)
    val Moon = Color(0xFF5C7CFA)

    // Opetuksen merkinnät kellon päällä
    val MarkNumber = Color(0xFFFFE066)
    val MarkTick = Color(0xFFFF8FB8)
    val MarkHalf = Color(0xFFFFE8A3)
    val MarkRed = Color(0xFFFFC9C9)
    val MarkGreen = Color(0xFFB2F2BB)
    val MarkBlue = Color(0xFFA5D8FF)
}

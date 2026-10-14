package fi.sateenkaarikello.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fi.sateenkaarikello.R

/** Fredoka (otsikot ja isot tekstit) ja Nunito (leipäteksti). Staattiset TTF:t, yksi per paino. */
val Fredoka =
    FontFamily(
        Font(R.font.fredoka_medium, FontWeight.Medium),
        Font(R.font.fredoka_semibold, FontWeight.SemiBold),
        Font(R.font.fredoka_bold, FontWeight.Bold),
    )

val Nunito =
    FontFamily(
        Font(R.font.nunito_semibold, FontWeight.SemiBold),
        Font(R.font.nunito_bold, FontWeight.Bold),
        Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    )

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

private fun display(
    size: Int,
    weight: FontWeight = FontWeight.SemiBold,
    lineHeight: Float = 1.15f,
) = TextStyle(
    fontFamily = Fredoka,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.em,
    lineHeightStyle = Trim,
    color = SkColors.Ink,
)

private fun body(
    size: Int,
    weight: FontWeight = FontWeight.Bold,
    lineHeight: Float = 1.45f,
) = TextStyle(
    fontFamily = Nunito,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.em,
    lineHeightStyle = Trim,
    color = SkColors.Ink,
)

/**
 * Tekstityylit. Koot ovat web-version clamp()-arvot puhelimen leveydellä (noin 400 dp).
 */
object SkType {
    val Body = body(17)
    val BodySoft = body(17).copy(color = SkColors.InkSoft)
    val Small = body(15)
    val Eyebrow = body(13, lineHeight = 1f).copy(letterSpacing = 0.08.em, color = SkColors.InkSoft)

    val Logo = display(40, FontWeight.Bold, 1.1f)
    val Tagline = display(20).copy(color = SkColors.InkSoft)
    val Title = display(22, lineHeight = 1.1f)
    val Words = display(28, lineHeight = 1.1f)
    val Question = display(26, lineHeight = 1.1f)
    val Digital = display(26, lineHeight = 1.2f)
    val OptWords = display(19)
    val OptDigital = display(18, lineHeight = 1.2f).copy(color = SkColors.InkSoft)

    val Button = display(21, lineHeight = 1.1f)
    val ButtonSmall = display(18, lineHeight = 1.1f)
    val Chip = body(15, lineHeight = 1.2f)
    val Segment = body(15, FontWeight.ExtraBold, 1.2f)
    val Pill = body(13, FontWeight.ExtraBold, 1.2f)

    val BigButton = display(22, lineHeight = 1.05f)
    val BigButtonSmall = body(14, lineHeight = 1.3f).copy(color = SkColors.InkSoft)
    val NowWords = display(22)
    val StarCount = display(30, lineHeight = 1.1f)

    val FeedbackOk = display(26).copy(color = SkColors.Good)
    val FeedbackHint = body(18).copy(color = SkColors.Try)

    val LevelTitle = display(24, lineHeight = 1.1f)
    val LessonTitle = display(22)
    val LessonNumber = display(22, FontWeight.Bold, 1f)
    val LessonText = body(19, lineHeight = 1.4f)
    val Caption = display(22)
    val Intro = body(19)

    val ResultTitle = display(34, lineHeight = 1.1f)
    val ResultText = body(19)
    val BigNumber = display(64, FontWeight.Bold, 1f).copy(color = SkColors.Hour)

    val SectionTitle = display(24, lineHeight = 1.1f)
    val ItemName = body(14, lineHeight = 1.2f)
    val NameField = display(22, lineHeight = 1.2f)
}

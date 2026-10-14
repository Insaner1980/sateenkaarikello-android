// Resource adapters stay together at the domain/UI boundary.
@file:Suppress("TooManyFunctions")

package fi.sateenkaarikello.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import fi.sateenkaarikello.R
import fi.sateenkaarikello.domain.FinnishTime
import fi.sateenkaarikello.domain.Greeting
import fi.sateenkaarikello.domain.Hint
import fi.sateenkaarikello.domain.HourPosition
import fi.sateenkaarikello.domain.MinutePosition
import fi.sateenkaarikello.domain.Slot
import fi.sateenkaarikello.domain.Text
import fi.sateenkaarikello.domain.capitalizeFirst

/**
 * Resolves domain text references using strings.xml.
 *
 * Android's formatting API requires varargs; Text.Res stores the arguments as a list.
 */
@Suppress("SpreadOperator")
fun Text.resolve(res: Resources): String =
    when (this) {
        is Text.Res -> if (args.isEmpty()) res.getString(id) else res.getString(id, *args.toTypedArray())
        is Text.Plural -> res.getQuantityString(id, count, count)
        is Text.Time -> FinnishTime.timeWords(minutes).capitalizeFirst()
        is Text.Plain -> value
    }

@Composable
@ReadOnlyComposable
fun resources(): Resources = LocalResources.current

@Composable
@ReadOnlyComposable
fun Text.resolve(): String = resolve(resources())

/** Kellonaika sanoina isolla alkukirjaimella, 24 h -tilassa vuorokaudenajan kanssa. */
fun timeLabel(
    t: Int,
    h24: Boolean,
): String = FinnishTime.fullWords(t, h24).capitalizeFirst()

/** Numeroaika: "3.30" tai 24 h -tilassa "kello 15.30" (web: digitalLabel). */
fun digitalLabel(
    res: Resources,
    t: Int,
    h24: Boolean,
): String = if (h24) res.getString(R.string.digital_24, FinnishTime.digital(t, true)) else FinnishTime.digital(t)

/** Kuuntele: "Kello on puoli neljä iltapäivällä, eli kello viisitoista kolmekymmentä". */
fun spokenTime(
    res: Resources,
    t: Int,
    h24: Boolean,
): String =
    if (h24) {
        res.getString(R.string.speak_time_24, FinnishTime.fullWords(t, true), FinnishTime.spoken24(t))
    } else {
        res.getString(R.string.speak_time, FinnishTime.fullWords(t, false))
    }

fun praise(
    res: Resources,
    index: Int,
): String {
    val all = res.getStringArray(R.array.praise)
    return all[Math.floorMod(index, all.size)]
}

private fun hourText(
    res: Resources,
    p: HourPosition,
): String =
    when (p) {
        is HourPosition.Exact -> res.getString(R.string.hour_pos_exact, p.number)
        is HourPosition.JustPassed -> res.getString(R.string.hour_pos_passed, p.number)
        is HourPosition.Between -> res.getString(R.string.hour_pos_between, p.number, p.next)
        is HourPosition.Almost -> res.getString(R.string.hour_pos_almost, p.next)
    }

private fun minuteText(
    res: Resources,
    p: MinutePosition,
): String =
    when (p) {
        MinutePosition.Top -> res.getString(R.string.minute_pos_top)
        MinutePosition.Bottom -> res.getString(R.string.minute_pos_bottom)
        MinutePosition.QuarterPast -> res.getString(R.string.minute_pos_quarter_past)
        MinutePosition.QuarterTo -> res.getString(R.string.minute_pos_quarter_to)
        is MinutePosition.Number -> res.getString(R.string.minute_pos_number, p.number, p.minutes)
        is MinutePosition.Ticks -> res.getString(R.string.minute_pos_ticks, p.minutes)
    }

private fun withTip(
    main: String,
    tip: String?,
): String = if (tip == null) main else "$main $tip"

/** Vinkin teksti täsmälleen kuten webin hintRead ja hintSet. */
fun Hint.text(res: Resources): String =
    when (this) {
        is Hint.HandsRight -> {
            res.getString(
                R.string.hint_hands_right,
                res.getString(if (dayPart.isSun) R.string.hint_sun else R.string.hint_moon),
                dayPart.name,
            )
        }

        is Hint.ReadHour -> {
            withTip(
                res.getString(R.string.hint_read_hour, hourText(res, position)),
                if (nearHalf) res.getString(R.string.hint_read_hour_half) else null,
            )
        }

        is Hint.ReadMinute -> {
            withTip(
                res.getString(R.string.hint_read_minute, minuteText(res, position)),
                if (alsoHour) res.getString(R.string.hint_read_minute_also_hour) else null,
            )
        }

        is Hint.SetMinute -> {
            res.getString(R.string.hint_set_minute, minuteText(res, position))
        }

        is Hint.SetHour -> {
            withTip(
                res.getString(R.string.hint_set_hour, hourText(res, position)),
                if (turnTip) res.getString(R.string.hint_set_hour_turn) else null,
            )
        }
    }

@StringRes
fun levelName(index: Int): Int =
    when (index) {
        0 -> R.string.level_tasatunnit
        1 -> R.string.level_puolet
        2 -> R.string.level_vartit
        FIVE_MINUTE_LEVEL -> R.string.level_viisi
        else -> R.string.level_minuutit
    }

/** Aloitusnäkymän tervehdyksen teksti. */
@StringRes
fun greetingText(greeting: Greeting): Int =
    when (greeting) {
        Greeting.MORNING -> R.string.intro_greeting_morning
        Greeting.DAY -> R.string.intro_greeting_day
        Greeting.EVENING -> R.string.intro_greeting_evening
        Greeting.NIGHT -> R.string.intro_greeting_night
    }

@StringRes
fun slotName(slot: Slot): Int =
    when (slot) {
        Slot.HAIR -> R.string.slot_hair
        Slot.DRESS -> R.string.slot_dress
        Slot.SHOES -> R.string.slot_shoes
        Slot.HEAD -> R.string.slot_head
        Slot.EXTRA -> R.string.slot_extra
    }

private val ItemNames =
    mapOf(
        "hair-letit" to R.string.item_hair_letit,
        "dress-perus" to R.string.item_dress_perus,
        "shoes-tossut" to R.string.item_shoes_tossut,
        "dress-pilkku" to R.string.item_dress_pilkku,
        "head-rusetti" to R.string.item_head_rusetti,
        "shoes-punaiset" to R.string.item_shoes_punaiset,
        "hair-pitkat" to R.string.item_hair_pitkat,
        "dress-tahti" to R.string.item_dress_tahti,
        "head-kukat" to R.string.item_head_kukat,
        "extra-laukku" to R.string.item_extra_laukku,
        "shoes-saappaat" to R.string.item_shoes_saappaat,
        "hair-kiharat" to R.string.item_hair_kiharat,
        "dress-prinsessa" to R.string.item_dress_prinsessa,
        "head-kruunu" to R.string.item_head_kruunu,
        "extra-siivet" to R.string.item_extra_siivet,
        "shoes-kimalle" to R.string.item_shoes_kimalle,
        "hair-sateenkaari" to R.string.item_hair_sateenkaari,
        "dress-sateenkaari" to R.string.item_dress_sateenkaari,
    )

@StringRes
fun itemName(id: String): Int = ItemNames.getValue(id)

private const val FIVE_MINUTE_LEVEL = 3

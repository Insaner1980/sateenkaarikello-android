package fi.sateenkaarikello.ui.clock

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fi.sateenkaarikello.domain.ClockTime
import fi.sateenkaarikello.domain.Hand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * Käsky siirtää kello aikaan [time]. [ms] = 0 siirtää heti. [exact] = true kestää tasan [ms]
 * (opetuksen hitaat kierrokset). Jokaisella käskyllä on oma [id], joten saman ajan voi lähettää uudestaan.
 */
@Immutable
data class ClockCommand(
    val time: Int,
    val ms: Int = 0,
    val exact: Boolean = false,
    val id: Long = nextId.incrementAndGet(),
) {
    private companion object {
        val nextId = AtomicLong()
    }
}

private val WebEase = Easing { ClockTime.easeInOut(it) }

/** Viisarien tila: [value] on pyöristetty aika, [raw] piirrettävä (animoitu tai raahattu) aika. */
@Stable
class ClockState internal constructor(
    initial: Int,
    period: Int,
    private val scope: CoroutineScope,
) {
    var period by mutableIntStateOf(period)
        private set
    var value by mutableIntStateOf(ClockTime.mod(initial, period))
        private set
    var raw by mutableFloatStateOf(value.toFloat())
        private set
    var dragging by mutableStateOf<Hand?>(null)
        private set

    var step = 5
    var reduceMotion = false
    var onChange: ((Int) -> Unit)? = null

    private var animation: Job? = null

    fun updatePeriod(p: Int) {
        if (p == period) return
        period = p
        animation?.cancel()
        value = ClockTime.mod(value, p)
        raw = value.toFloat()
    }

    fun apply(command: ClockCommand) {
        value = ClockTime.mod(command.time, period)
        if (command.ms > 0) animateTo(value, command.ms, command.exact) else jumpTo(value)
    }

    private fun jumpTo(t: Int) {
        animation?.cancel()
        raw = t.toFloat()
    }

    private fun animateTo(
        t: Int,
        ms: Int,
        exact: Boolean,
    ) {
        animation?.cancel()
        val from = raw
        val delta = ClockTime.circularDelta(from, t.toFloat(), period)
        if (reduceMotion || abs(delta) < ANIMATION_EPSILON) {
            raw = t.toFloat()
            return
        }
        val duration = ClockTime.animationMillis(delta, ms, exact)
        animation =
            scope.launch {
                animate(0f, 1f, animationSpec = tween(duration, easing = WebEase)) { k, _ -> raw = from + delta * k }
                raw = t.toFloat()
            }
    }

    internal fun startDrag(
        hand: Hand,
        angle: Float,
    ) {
        animation?.cancel()
        dragging = hand
        dragTo(angle)
    }

    internal fun dragTo(angle: Float) {
        raw =
            when (dragging) {
                Hand.MINUTE -> ClockTime.minuteDrag(raw, angle, period)
                Hand.HOUR -> ClockTime.hourDrag(raw, angle, period)
                null -> return
            }
        val v = ClockTime.snap(raw, step, period)
        if (v != value) {
            value = v
            onChange?.invoke(v)
        }
    }

    internal fun endDrag() {
        if (dragging == null) return
        dragging = null
        value = ClockTime.snap(raw, step, period)
        animateTo(value, SNAP_MS, exact = false)
        onChange?.invoke(value)
    }

    /** Saavutettavuustoiminto ja näppäimistö: siirtää ajan yhden askeleen. */
    internal fun nudge(direction: Int) {
        value = ClockTime.mod(value + direction * step, period)
        animateTo(value, NUDGE_MS, exact = false)
        onChange?.invoke(value)
    }

    private companion object {
        const val SNAP_MS = 160
        const val NUDGE_MS = 200
    }
}

private const val ANIMATION_EPSILON = 0.01f

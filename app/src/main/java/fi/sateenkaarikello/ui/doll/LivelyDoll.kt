package fi.sateenkaarikello.ui.doll

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import fi.sateenkaarikello.domain.Outfit
import fi.sateenkaarikello.ui.components.LocalConfetti
import fi.sateenkaarikello.ui.components.confettiAnchor
import fi.sateenkaarikello.ui.rememberReduceMotion

/**
 * Elävä nukke: räpyttää silmiään ja keinuu kevyesti. [celebration] ajetaan kerran.
 * Kun järjestelmän animaatiot on poistettu, nukke pysyy paikallaan ja uusi asu näkyy heti.
 */
@Composable
fun LivelyDoll(
    outfit: Outfit,
    modifier: Modifier = Modifier,
    celebration: DollCelebration? = null,
) {
    val reduceMotion = rememberReduceMotion()
    val confetti = LocalConfetti.current
    val nowMs = remember { mutableLongStateOf(0L) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) withInfiniteAnimationFrameNanos { nowMs.longValue = (it - start) / NANOS_PER_MS }
    }
    val active = celebration?.takeIf { it.spin || it.jump }
    val frameAt = { now: Long ->
        active?.takeIf { now >= it.delayMs }?.let { DollMotion.celebration(now - it.delayMs, it.spin, it.jump) }
    }
    val showNew by remember(active, reduceMotion) {
        derivedStateOf { active?.spin == true && (reduceMotion || frameAt(nowMs.longValue)?.showNewOutfit == true) }
    }
    val center = remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(showNew) {
        if (showNew && !reduceMotion) confetti?.sparkle(center.value)
    }
    Doll(
        outfit = active?.newOutfit?.takeIf { showNew } ?: outfit,
        modifier = modifier.confettiAnchor { center.value = it },
        pose = {
            if (reduceMotion) {
                DollPose()
            } else {
                val now = nowMs.longValue
                val idle = DollMotion.idle(now)
                val frame = frameAt(now)
                if (frame == null || frame.done) idle else frame.pose.copy(blink = idle.blink)
            }
        },
    )
}

private const val NANOS_PER_MS = 1_000_000L

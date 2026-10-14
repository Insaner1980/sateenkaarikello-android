package fi.sateenkaarikello

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey

/** Paluupinon muokkaus yhdessä paikassa. Koti on aina pohjalla. */
@Stable
class Navigator(
    private val stack: MutableList<NavKey>,
) {
    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** Korvaa päällimmäisen näkymän (esim. peli → tulos), jolloin takaisin vie edelliseen näkymään. */
    fun replace(screen: Screen) {
        if (stack.size > 1) stack[stack.lastIndex] = screen else stack.add(screen)
    }

    /** Palaa pinossa näkymään [screen] tai lisää sen kodin päälle. */
    fun popTo(screen: Screen) {
        val index = stack.indexOfLast { it == screen }
        if (index >= 0) {
            while (stack.size > index + 1) stack.removeAt(stack.lastIndex)
        } else {
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            stack.add(screen)
        }
    }

    /** Tason aloitus: ensin oppitunnit, jos niitä on, muuten suoraan peliin. */
    fun startLevel(
        level: Int,
        lessonQueue: List<String>,
        replaceTop: Boolean,
    ) {
        val target =
            if (lessonQueue.isEmpty()) {
                Screen.Game(level, round = System.nanoTime())
            } else {
                Screen.Lesson(lessonQueue, thenLevel = level)
            }
        if (replaceTop) replace(target) else push(target)
    }
}

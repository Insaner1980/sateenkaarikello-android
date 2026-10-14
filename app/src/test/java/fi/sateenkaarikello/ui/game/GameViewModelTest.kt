package fi.sateenkaarikello.ui.game

import fi.sateenkaarikello.Screen
import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.domain.Hint
import fi.sateenkaarikello.domain.Levels
import fi.sateenkaarikello.domain.TaskType
import fi.sateenkaarikello.ui.FakeServices
import fi.sateenkaarikello.ui.MainDispatcherRule
import fi.sateenkaarikello.ui.components.OptionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val scope = TestScope(mainDispatcher.dispatcher)
    private val services = FakeServices()

    private fun game(
        level: Int = 0,
        h24: Boolean = false,
    ) = GameViewModel(level, h24, services, Random(11))

    @Test
    fun `starts with a read task showing three options and the clock at the answer`() =
        scope.runTest {
            val vm = game()
            val s = vm.state.value
            assertEquals(0, s.taskIndex)
            assertEquals(TaskType.READ, s.task.type)
            assertEquals(3, s.options.size)
            assertTrue(s.task.time in s.options)
            assertEquals(s.task.time, s.clock.time)
            assertFalse(s.interactive)
        }

    @Test
    fun `correct answer praises and moves on after a pause`() =
        scope.runTest {
            val vm = game()
            val answer = vm.state.value.task.time
            vm.pick(answer)
            val s = vm.state.value
            assertEquals(OptionState.CORRECT, s.optionStates[answer])
            assertTrue(s.feedback is GameFeedback.Praise)
            assertEquals(1, s.celebrate)
            assertEquals(listOf(Sound.OK), services.sounds)
            advanceTimeBy(1600)
            assertEquals(1, vm.state.value.taskIndex)
            assertEquals(TaskType.SET, vm.state.value.task.type)
            assertTrue(vm.state.value.interactive)
        }

    @Test
    fun `first mistake gives a hint and second reveals the answer`() =
        scope.runTest {
            val vm = game()
            val s0 = vm.state.value
            val wrong = s0.options.filter { it != s0.task.time }
            vm.pick(wrong[0])
            assertTrue(vm.state.value.feedback is GameFeedback.HintText)
            assertEquals(OptionState.WRONG, vm.state.value.optionStates[wrong[0]])
            vm.pick(wrong[0])
            assertEquals(1, vm.state.value.mistakes)
            vm.pick(wrong[1])
            val s = vm.state.value
            assertEquals(2, s.mistakes)
            assertEquals(GameFeedback.RevealRead(s0.task.time), s.feedback)
            assertEquals(OptionState.CORRECT, s.optionStates[s0.task.time])
            assertTrue(s.showNext)
            vm.pick(s0.task.time)
            assertEquals(2, vm.state.value.mistakes)
            vm.next()
            assertEquals(1, vm.state.value.taskIndex)
        }

    @Test
    fun `set task accepts the right hand position and hints otherwise`() =
        scope.runTest {
            val vm = game(level = 2)
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1600)
            val target = vm.state.value.task.time
            assertEquals(Levels.setTaskStart(target), vm.state.value.clock.time)
            assertTrue(vm.state.value.showDone)
            vm.onClockChange((target + 30) % 720)
            vm.done()
            val hint = (vm.state.value.feedback as GameFeedback.HintText).hint
            assertTrue(hint is Hint.SetMinute)
            vm.onClockChange(target)
            vm.done()
            assertTrue(vm.state.value.feedback is GameFeedback.Praise)
            assertEquals(target, vm.state.value.clock.time)
            assertFalse(vm.state.value.interactive)
        }

    @Test
    fun `24 h set task accepts the same hands in the other day half`() =
        scope.runTest {
            val vm = game(level = 1, h24 = true)
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1600)
            val target = vm.state.value.task.time
            vm.onClockChange((target + 720) % 1440)
            vm.done()
            assertTrue(vm.state.value.feedback is GameFeedback.Praise)
        }

    @Test
    fun `second set mistake shows the answer on the clock`() =
        scope.runTest {
            val vm = game(level = 0)
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1600)
            val target = vm.state.value.task.time
            vm.onClockChange((target + 60) % 720)
            vm.done()
            vm.done()
            val s = vm.state.value
            assertEquals(GameFeedback.RevealSet(target), s.feedback)
            assertEquals(target, s.clock.time)
            assertEquals(1400, s.clock.ms)
            assertFalse(s.showDone)
            assertTrue(s.showNext)
        }

    @Test
    fun `finishing ten tasks records stars and reports the result`() =
        scope.runTest {
            val vm = game(level = 0)
            repeat(10) {
                val s = vm.state.value
                if (s.task.type == TaskType.READ) {
                    vm.pick(s.task.time)
                } else {
                    vm.onClockChange(s.task.time)
                    vm.done()
                }
                advanceTimeBy(1600)
            }
            advanceUntilIdle()
            val result = vm.state.value.finished
            assertNotNull(result)
            assertEquals(Screen.Result(level = 0, stars = 3, previousBest = 0, starsBefore = 0), result)
            assertEquals(
                3,
                services.repository.progress
                    .first()
                    .stars[0],
            )
            assertEquals(Sound.WIN, services.sounds.last())
        }

    @Test
    fun `no result before the last task`() =
        scope.runTest {
            val vm = game()
            assertNull(vm.state.value.finished)
        }
}

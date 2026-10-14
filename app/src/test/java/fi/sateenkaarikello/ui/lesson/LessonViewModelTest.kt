package fi.sateenkaarikello.ui.lesson

import fi.sateenkaarikello.domain.Hand
import fi.sateenkaarikello.domain.Mark
import fi.sateenkaarikello.ui.FakeServices
import fi.sateenkaarikello.ui.MainDispatcherRule
import fi.sateenkaarikello.ui.components.OptionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val scope = TestScope(mainDispatcher.dispatcher)
    private val services = FakeServices()

    private fun vm(
        queue: List<String>,
        thenLevel: Int? = null,
    ) = LessonViewModel(queue, thenLevel, services, Random(5))

    @Test
    fun `opens the first page and runs its demo`() =
        scope.runTest {
            val vm = vm(listOf("viisarit"))
            val s = vm.state.value
            assertEquals("viisarit", s.lesson.id)
            assertEquals(0, s.page)
            assertEquals(240, s.clock.time)
            assertNull(s.highlight)
            advanceTimeBy(701)
            assertEquals(Hand.HOUR, vm.state.value.highlight)
        }

    @Test
    fun `next page resets marks and restarts the demo`() =
        scope.runTest {
            val vm = vm(listOf("viisarit"))
            advanceTimeBy(800)
            vm.next()
            runCurrent()
            assertEquals(1, vm.state.value.page)
            assertNull(vm.state.value.highlight)
            advanceTimeBy(701)
            assertEquals(Hand.MINUTE, vm.state.value.highlight)
            vm.previous()
            runCurrent()
            assertEquals(0, vm.state.value.page)
        }

    @Test
    fun `page with initial marks starts with them`() =
        scope.runTest {
            val vm = vm(listOf("puoli"))
            vm.next()
            runCurrent()
            assertTrue(
                vm.state.value.under
                    .first() is Mark.Sector,
            )
            assertEquals(210, vm.state.value.clock.time)
        }

    @Test
    fun `demo moves the clock with an exact animation`() =
        scope.runTest {
            val vm = vm(listOf("kierros"))
            advanceTimeBy(1501)
            assertEquals(240, vm.state.value.clock.time)
            assertEquals(6000, vm.state.value.clock.ms)
            assertTrue(vm.state.value.clock.exact)
        }

    @Test
    fun `quiz hint after a wrong choice and completion is saved`() =
        scope.runTest {
            val vm = vm(listOf("viisarit"))
            repeat(3) { vm.next() }
            runCurrent()
            val s = vm.state.value
            assertTrue(s.isQuiz)
            val wrong = s.choices.indexOfFirst { !it.correct }
            vm.pick(wrong)
            assertEquals(OptionState.WRONG, vm.state.value.choiceStates[wrong])
            assertTrue(vm.state.value.showHint)
            val right = s.choices.indexOfFirst { it.correct }
            vm.pick(right)
            advanceUntilIdle()
            assertTrue(vm.state.value.quizDone)
            assertEquals(NextAction.FINISH, vm.state.value.nextAction)
            assertEquals(
                setOf("viisarit"),
                services.repository.progress
                    .first()
                    .lessonsDone,
            )
        }

    @Test
    fun `queue moves to the next lesson and ends with the level`() =
        scope.runTest {
            val vm = vm(listOf("puoli", "vrk"), thenLevel = 1)
            repeat(4) { vm.next() }
            vm.pick(
                vm.state.value.choices
                    .indexOfFirst { it.correct },
            )
            assertEquals(NextAction.NEXT_LESSON, vm.state.value.nextAction)
            vm.next()
            runCurrent()
            assertEquals("vrk", vm.state.value.lesson.id)
            assertEquals(1440, vm.state.value.period)
            repeat(3) { vm.next() }
            vm.pick(
                vm.state.value.choices
                    .indexOfFirst { it.correct },
            )
            assertEquals(NextAction.START_GAME, vm.state.value.nextAction)
            assertFalse(vm.state.value.finished)
            vm.next()
            assertTrue(vm.state.value.finished)
        }

    @Test
    fun `next is ignored on the quiz until it is answered`() =
        scope.runTest {
            val vm = vm(listOf("minuutit"))
            repeat(2) { vm.next() }
            vm.next()
            assertTrue(vm.state.value.isQuiz)
            assertFalse(vm.state.value.finished)
        }
}

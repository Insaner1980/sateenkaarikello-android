package fi.sateenkaarikello.ui.challenge

import fi.sateenkaarikello.audio.Sound
import fi.sateenkaarikello.ui.FakeServices
import fi.sateenkaarikello.ui.components.OptionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val services = FakeServices()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts on the intro with the clock at a quarter to one`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(1))
            assertEquals(ChallengePhase.START, vm.state.value.phase)
            assertEquals(45, vm.state.value.clock.time)
        }

    @Test
    fun `correct answers score and the next question follows`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(1))
            vm.start()
            val first = vm.state.value.question
            vm.pick(first)
            assertEquals(1, vm.state.value.score)
            assertEquals(OptionState.CORRECT, vm.state.value.optionStates[first])
            advanceTimeBy(400)
            assertTrue(vm.state.value.question != first)
            assertTrue(
                vm.state.value.optionStates
                    .isEmpty(),
            )
        }

    @Test
    fun `wrong answer shows the right one and continues`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(2))
            vm.start()
            val s = vm.state.value
            val wrong = s.options.first { it != s.question }
            vm.pick(wrong)
            assertEquals(0, vm.state.value.score)
            assertEquals(OptionState.WRONG, vm.state.value.optionStates[wrong])
            assertEquals(OptionState.CORRECT, vm.state.value.optionStates[s.question])
            assertEquals(Sound.BAD, services.sounds.last())
            advanceTimeBy(1400)
            assertTrue(
                vm.state.value.optionStates
                    .isEmpty(),
            )
        }

    @Test
    fun `time runs out after a minute and the record is saved`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(3))
            vm.start()
            vm.pick(vm.state.value.question)
            runCurrent()
            advanceTimeBy(60_001)
            advanceUntilIdle()
            val s = vm.state.value
            assertEquals(ChallengePhase.END, s.phase)
            assertEquals(1, s.score)
            assertTrue(s.newRecord)
            assertEquals(
                1,
                services.repository.progress
                    .first()
                    .challengeBest,
            )
        }
}

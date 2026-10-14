package fi.sateenkaarikello.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.ExternalResource

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : ExternalResource() {
    val dispatcher = StandardTestDispatcher()

    override fun before() {
        Dispatchers.setMain(dispatcher)
    }

    override fun after() {
        Dispatchers.resetMain()
    }
}

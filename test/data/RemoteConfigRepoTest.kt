package data

import dita.dev.appModules
import dita.dev.data.RemoteConfigRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject

@ExperimentalCoroutinesApi
class RemoteConfigRepoTest : AutoCloseKoinTest() {
    private val remoteConfigRepo: RemoteConfigRepo by inject()


    @Test
    fun remoteConfigsAreFetchedSuccessfully(): Unit = runBlocking {
        startKoin {
            modules(appModules)
        }
        val config = remoteConfigRepo.getRemoteConfig()
        assertThat(config.parameters.feedbackEmail.defaultValue.value, `is`("dita@daystar.ac.ke"))
    }
}
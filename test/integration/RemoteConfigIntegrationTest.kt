package integration

import dita.dev.appModules
import dita.dev.data.RemoteConfigRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject

@ExperimentalCoroutinesApi
class RemoteConfigIntegrationTest : AutoCloseKoinTest() {
    private val remoteConfigRepo: RemoteConfigRepo by inject()

    @Test(expected = Test.None::class)
    fun remoteConfigsAreFetchedSuccessfully(): Unit = runBlocking {
        startKoin {
            modules(appModules)
        }
        val config = remoteConfigRepo.getRemoteConfig()
    }
}
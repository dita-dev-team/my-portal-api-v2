package data

import com.google.gson.Gson
import dita.dev.data.RemoteConfig
import dita.dev.data.RemoteConfigRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject

fun dummyConfig(payload: String): RemoteConfig {
    val gson = Gson()
    return gson.fromJson(payload, RemoteConfig::class.java)
}

@ExperimentalCoroutinesApi
class RemoteConfigRepoTest : AutoCloseKoinTest() {
    private val remoteConfigRepo: RemoteConfigRepo by inject()

//    @Test
//    fun areExamsAvailable_returnsFalse(): Unit = runBlocking {
//
//    }
}
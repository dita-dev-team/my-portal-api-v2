package dita.dev.data

import com.github.kittinunf.fuse.core.CacheBuilder
import com.github.kittinunf.fuse.core.StringDataConvertible
import com.github.kittinunf.fuse.core.build
import com.github.kittinunf.fuse.core.scenario.ExpirableCache
import com.github.kittinunf.fuse.core.scenario.get
import com.github.kittinunf.fuse.core.scenario.put
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.ErrorCode
import com.google.firebase.FirebaseException
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileInputStream
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.hours

interface RemoteConfigRepo {
    suspend fun getRemoteConfig(): RemoteConfig

    suspend fun updateRemoteConfig(remoteConfig: RemoteConfig)

    suspend fun areExamsOngoing(): Boolean

    suspend fun updateCurrentCalendar()

    suspend fun checkExamPeriod()

}


class RemoteConfigRepoImpl(private val client: OkHttpClient, private val url: String) : RemoteConfigRepo {

    private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
    private val tempDir = createTempDir().absolutePath
    private val cache =
        CacheBuilder.config(tempDir, convertible = StringDataConvertible()).build().let(::ExpirableCache)


    private var etag: String? = null

    private suspend fun loadFromRemote(): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Content-Type", "application.json")
            .header("Accept-Encoding", "gzip")
            .header("Authorization", "Bearer ${getAccessToken()}")
            .build()
        val response = client.newCall(request).await()
        val etag = response.header("etag")
        val body = response.body?.text()
        etag?.let {
            saveToCache("etag", it)
        }
        body?.let {
            saveToCache("body", it)
        }
        this.etag = etag
        return body
    }

    @OptIn(ExperimentalTime::class)
    private fun loadFromCache(key: String): String? {
        val (value, error) = cache.get(key, null, timeLimit = 24.hours)
        return value
    }

    private fun saveToCache(key: String, value: String) {
        cache.put(key, value)
    }

    override suspend fun getRemoteConfig(): RemoteConfig {
        var body = loadFromCache("body")
        if (body == null) {
            body = loadFromRemote()
        } else {
            etag = loadFromCache("etag")
        }
        return gson.fromJson(body, RemoteConfig::class.java)
    }

    override suspend fun updateRemoteConfig(remoteConfig: RemoteConfig) {
        val jsonString = gson.toJson(remoteConfig)
        val requestBody = jsonString.toRequestBody()
        var request = Request.Builder()
            .url("$url?validate_only=true")
            .put(requestBody)
            .header("Content-Type", "application.json")
            .header("Accept-Encoding", "gzip")
            .header("Authorization", "Bearer ${getAccessToken()}")
            .header("If-Match", etag!!)
            .build()
        val response = client.newCall(request).await()
        if (response.isSuccessful) {
            if (response.header("etag") == null) {
                throw FirebaseException(
                    ErrorCode.FAILED_PRECONDITION,
                    "ETag header is not present in the server response.",
                    null
                )
            }
            request = request.newBuilder().url(url).build()
            client.newCall(request).await()
            loadFromRemote() // We need to refresh the cached data
            println("Remote config update successfully")
        }
    }

    override suspend fun areExamsOngoing(): Boolean {
        val config = getRemoteConfig()
        val examPeriod = config.getExamPeriod()
        val now = Date()
        return examPeriod.startDate.before(now) && examPeriod.endDate.after(now)
    }

    override suspend fun updateCurrentCalendar() {
        TODO("Not yet implemented")
    }

    override suspend fun checkExamPeriod() {
        val config = getRemoteConfig()
        if (areExamsOngoing()) {
            config.enableExamTimetableAvailability()
        } else {
            config.disableExamTimetableAvailability()
        }
        updateRemoteConfig(config)
    }

    private fun getAccessToken(): String {
        val credentials = GoogleCredentials.fromStream(FileInputStream("google-credentials.json"))
        val scopedCredentials = credentials.createScoped("https://www.googleapis.com/auth/firebase.remoteconfig")
        val accessToken = scopedCredentials.refreshAccessToken()
        return accessToken.tokenValue
    }

}
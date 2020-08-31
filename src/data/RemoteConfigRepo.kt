package dita.dev.data

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.ErrorCode
import com.google.firebase.FirebaseException
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileInputStream

interface RemoteConfigRepo {
    suspend fun getRemoteConfig(): RemoteConfig

    suspend fun updateRemoteConfig(remoteConfig: RemoteConfig)

    suspend fun areExamsOngoing(): Boolean

    suspend fun updateCurrentCalendar()

    suspend fun checkExamPeriod()

}

class RemoteConfigRepoImpl(private val client: OkHttpClient) : RemoteConfigRepo {

    private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
    private val projectId = System.getProperty("project_id")

    private var etag: String? = null

    override suspend fun getRemoteConfig(): RemoteConfig {
        val url = "https://firebaseremoteconfig.googleapis.com/v1/projects/${projectId}/remoteConfig"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Content-Type", "application.json")
            .header("Accept-Encoding", "gzip")
            .header("Authorization", "Bearer ${getAccessToken()}")
            .build()
        val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
        val response = client.newCall(request).await()
        etag = response.header("etag")
        val body = response.body?.text()
        return gson.fromJson(body, RemoteConfig::class.java)
    }

    override suspend fun updateRemoteConfig(remoteConfig: RemoteConfig) {
        val url = "https://firebaseremoteconfig.googleapis.com/v1/projects/${projectId}/remoteConfig"
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
        var response = client.newCall(request).await()
        if (response.isSuccessful) {
            if (response.header("etag") == null) {
                throw FirebaseException(
                    ErrorCode.FAILED_PRECONDITION,
                    "ETag header is not present in the server response.",
                    null
                )
            }
            request = request.newBuilder().url(url).build()
            response = client.newCall(request).await()
            println("Remote config update successfully")
        }
    }

    override suspend fun areExamsOngoing(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun updateCurrentCalendar() {
        TODO("Not yet implemented")
    }

    override suspend fun checkExamPeriod() {
        TODO("Not yet implemented")
    }

    private fun getAccessToken(): String {
        val credentials = GoogleCredentials.fromStream(FileInputStream("google-credentials.json"))
        val scopedCredentials = credentials.createScoped("https://www.googleapis.com/auth/firebase.remoteconfig")
        val accessToken = scopedCredentials.refreshAccessToken()
        return accessToken.tokenValue
    }

}
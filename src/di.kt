package dita.dev

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import dita.dev.data.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.FileInputStream

val appModules = module {
    single {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(FileInputStream("google-credentials.json")))
            .setProjectId(FirebaseConfig.projectId)
            .build()

        FirebaseApp.initializeApp(options)
    }

    single {
        FirebaseAuth.getInstance(get())
    }

    single {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC)
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .build()
    }

    single<RemoteConfigRepo> {
        RemoteConfigRepoImpl(get(), get(named("firebaseUrl")), get())
    }

    single(named("firebaseUrl")) {
        val projectId = System.getProperty("project_id")
        "https://firebaseremoteconfig.googleapis.com/v1/projects/${projectId}/remoteConfig"
    }

    single<CalendarProvider> {
        PortalCalendarProvider()
    }

    single<AuthRepo> {
        AuthRepoImpl(get())
    }
}
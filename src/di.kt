package dita.dev

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.koin.dsl.module
import java.io.FileInputStream

val appModules = module {
    single {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(FileInputStream("google-credentials.json")))
            .setProjectId(System.getProperty("project_id"))
            .build()

        FirebaseApp.initializeApp(options)
    }
}
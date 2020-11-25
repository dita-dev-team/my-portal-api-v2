package dita.dev.data

object FirebaseConfig {
    val apiKey = System.getenv("FIREBASE_API_KEY")
    val authDomain = System.getenv("FIREBASE_AUTH_DOMAIN")
    val databaseURL = System.getenv("FIREBASE_DATABASE_URL")
    val projectId = System.getenv("FIREBASE_PROJECT_ID")
    val storageBucket = System.getenv("FIREBASE_STORAGE_BUCKET")
    val messagingSenderId = System.getenv("FIREBASE_MESSAGING_SENDER_ID")
    val appId = System.getenv("FIREBASE_APP_ID")

    fun generateModel(src: Map<String, Any>): Map<String, Any> {
        val firebaseConfigMap = mapOf<String, String>(
            "apiKey" to apiKey,
            "authDomain" to authDomain,
            "databaseURL" to databaseURL,
            "projectId" to projectId,
            "storageBuckert" to storageBucket,
            "messagingSenderId" to messagingSenderId,
            "appId" to appId
        )
        val finalMap = mutableMapOf<String, Any>()
        finalMap.putAll(src)
        finalMap.putAll(firebaseConfigMap)
        return finalMap
    }
}
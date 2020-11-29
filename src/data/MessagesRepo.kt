package dita.dev.data

import com.google.cloud.firestore.Firestore
import com.google.firebase.messaging.*

interface MessagesRepo {

    suspend fun saveNotification(notification: Notification)

    suspend fun fetchAllNotifications(): List<Notification>

    suspend fun fetchNotificationsByEmail(email: String): List<Notification>

    suspend fun clearNotifications()

    suspend fun sendNotification(title: String, body: String, topic: String, sender: String): Boolean
}

class MessagesRepoImpl(private val firestore: Firestore, private val firebaseMessaging: FirebaseMessaging) :
    MessagesRepo, FirestoreRepo() {
    private val collection = "messages"

    override suspend fun saveNotification(notification: Notification) {
        firestore.collection(collection).add(notification).await()
    }

    override suspend fun fetchAllNotifications(): List<Notification> {
        val snapshot = firestore.collection(collection).get().await()
        return snapshot.toObjects(Notification::class.java)
    }

    override suspend fun fetchNotificationsByEmail(email: String): List<Notification> {
        val snapshot = firestore.collection(collection).whereEqualTo("email", email).get().await()
        return snapshot.toObjects(Notification::class.java)
    }

    override suspend fun clearNotifications() {
        val batchSize = 100
        val query = firestore.collection(collection).orderBy("email").limit(batchSize)
        return deleteQueryBatch(firestore, query, batchSize)
    }

    override suspend fun sendNotification(title: String, body: String, topic: String, sender: String): Boolean {
        return try {
            val androidNotification = AndroidNotification.builder()
                .setTitle(title)
                .setBody(body)
                .setIcon("stock_ticker_update")
                .setColor("#1D1124")
                .build()

            val androidConfig = AndroidConfig.builder()
                .setTtl(3600 * 1000)
                .setPriority(AndroidConfig.Priority.NORMAL)
                .setNotification(androidNotification)
                .putData("title", title)
                .putData("body", body)
                .build()

            val message = Message.builder()
                .setAndroidConfig(androidConfig)
                .setTopic(topic)
                .build()
            firebaseMessaging.sendAsync(message).await()
            saveNotification(
                Notification(
                    sender,
                    title,
                    body,
                    topic,
                    "success"
                )
            )
            true
        } catch (e: FirebaseMessagingException) {
            println(e)
            saveNotification(
                Notification(
                    sender,
                    title,
                    body,
                    topic,
                    "failed"
                )
            )
            false
        }
    }

}
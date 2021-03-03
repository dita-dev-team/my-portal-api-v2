package dita.dev.data

import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

sealed class TokenValid {
    class Yes(val data: User) : TokenValid()
    object No : TokenValid()
}

data class UserSession(val uid: String, val email: String, val expiration: Long)

interface AuthRepo {

    fun isTokenValid(idToken: String): TokenValid

    suspend fun isAdmin(uid: String): Boolean
}

class AuthRepoImpl(private val firebaseAuth: FirebaseAuth, private val firestore: Firestore) : AuthRepo {
    private val collection = "admins"

    override fun isTokenValid(idToken: String): TokenValid {
        return try {
            val token = firebaseAuth.verifyIdToken(idToken)
            TokenValid.Yes(User(token.uid, token.email))
        } catch (e: FirebaseAuthException) {
            e.printStackTrace()
            TokenValid.No
        }
    }

    override suspend fun isAdmin(uid: String): Boolean {
        val snapshot = firestore.collection(collection).whereEqualTo("userId", uid).get().await()
        return !snapshot.isEmpty
    }
}
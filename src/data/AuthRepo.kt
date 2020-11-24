package dita.dev.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

sealed class TokenValid {
    class Yes(val data: User) : TokenValid()
    object No : TokenValid()
}

data class UserSession(val uid: String, val email: String, val expiration: Long)

interface AuthRepo {

    fun isTokenValid(idToken: String): TokenValid
}

class AuthRepoImpl(private val firebaseAuth: FirebaseAuth) : AuthRepo {

    override fun isTokenValid(idToken: String): TokenValid {
        return try {
            val token = firebaseAuth.verifyIdToken(idToken)
            TokenValid.Yes(User(token.uid, token.email))
        } catch (e: FirebaseAuthException) {
            e.printStackTrace()
            TokenValid.No
        }
    }
}
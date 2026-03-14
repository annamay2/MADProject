package com.example.madprojectactivity.data.repository

import android.content.Context
import com.example.madprojectactivity.data.local.AppDatabase
import com.example.madprojectactivity.data.local.UserDao
import com.example.madprojectactivity.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AuthListenerHandle internal constructor(internal val inner: FirebaseAuth.AuthStateListener)

class UserRepository(context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userDao: UserDao = AppDatabase.getDatabase(context).userDao()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    fun addAuthStateListener(onAuthChanged: (uid: String?) -> Unit): AuthListenerHandle {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            onAuthChanged(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        return AuthListenerHandle(listener)
    }

    fun removeAuthStateListener(handle: AuthListenerHandle) {
        auth.removeAuthStateListener(handle.inner)
    }

    fun observeUser(uid: String): Flow<UserProfile?> {
        return userDao.observeUser(uid)
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        persistCurrentUser()
    }

    suspend fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
        persistCurrentUser()
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun persistCurrentUser() {
        val firebaseUser: FirebaseUser = auth.currentUser ?: return
        val profile = UserProfile(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: ""
        )
        userDao.insertUser(profile)
    }
}

package com.example.madprojectactivity.data.repository

import android.util.Log
import com.example.madprojectactivity.data.model.UserProfile
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

class UserRepository {
    private val db = Firebase.firestore

    fun addUser(profile: UserProfile) {
        db.collection("users")
            .add(profile) // auto-generates a document ID
            .addOnSuccessListener { docRef ->
                Log.d("Firestore", "User added with id=${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to add user", e)
            }
    }

    fun getUser(docId: String, onResult: (UserProfile?) -> Unit) {
        db.collection("users")
            .document(docId)
            .get()
            .addOnSuccessListener { snap ->
                onResult(snap.toObject(UserProfile::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}

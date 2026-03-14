// AI-generated (Claude): Added image sync — downloads images from Firebase Storage
// on remote sync, passes imageUri to Receipt UI model, preserves local paths.
package com.example.madprojectactivity.screens.home

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.madprojectactivity.data.local.AppDatabase
import com.example.madprojectactivity.data.model.ReceiptEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date

data class Receipt(
    val id: String = "",
    val amount: Double = 0.0,
    val storeName: String = "",
    val date: Timestamp? = null,
    val uploadedToRevenue: Boolean = false,
    val imageUri: Uri? = null
)

data class HomeUiState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val receipts: List<Receipt> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val receiptDao = AppDatabase.getDatabase(application).receiptDao()

    private var remoteListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoggedIn = auth.currentUser != null,
            userEmail = auth.currentUser?.email
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

    private val authListener = FirebaseAuth.AuthStateListener { a ->
        val user = a.currentUser
        _uiState.update { it.copy(
            isLoggedIn = user != null,
            userEmail = user?.email
        ) }
        if (user != null) {
            observeLocalReceipts(user.uid)
            startRemoteSync(user.uid)
        } else {
            stopRemoteSync()
            _uiState.update { it.copy(receipts = emptyList()) }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        auth.currentUser?.let {
            observeLocalReceipts(it.uid)
            startRemoteSync(it.uid)
        }
    }

    private fun observeLocalReceipts(uid: String) {
        viewModelScope.launch {
            receiptDao.getAllReceipts(uid).collect { entities ->
                val list = entities.map { entity ->
                    Receipt(
                        id = entity.id,
                        amount = entity.amount,
                        storeName = entity.storeName,
                        date = Timestamp(Date(entity.date)),
                        uploadedToRevenue = entity.uploadedToRevenue,
                        imageUri = entity.imageUri?.let { Uri.parse(it) }
                    )
                }
                _uiState.update { it.copy(receipts = list) }
            }
        }
    }

    private fun startRemoteSync(uid: String) {
        remoteListener?.remove()

        remoteListener = db.collection("users")
            .document(uid)
            .collection("receipts")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val remoteUrl = doc.getString("imageUrl")
                                val existing = receiptDao.getReceiptById(doc.id)

                                val entity = ReceiptEntity(
                                    id = doc.id,
                                    userId = uid,
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    storeName = doc.getString("storeName") ?: "",
                                    glutenFreeItems = doc.getString("glutenFreeItems") ?: "",
                                    uploadedToRevenue = doc.getBoolean("uploadedToRevenue") ?: false,
                                    date = doc.getTimestamp("date")?.toDate()?.time ?: 0L,
                                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                                    isSynced = true,
                                    imageUri = existing?.imageUri,
                                    remoteImageUrl = remoteUrl
                                )
                                receiptDao.insertReceipt(entity)

                                // Download image if we have a remote URL but no local file
                                if (remoteUrl != null && existing?.imageUri == null) {
                                    try {
                                        val localUri = downloadImageToLocal(remoteUrl, doc.id)
                                        receiptDao.updateLocalImageUri(doc.id, localUri.toString())
                                    } catch (ex: Exception) {
                                        Log.w("HomeViewModel", "Image download failed for ${doc.id}", ex)
                                    }
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                receiptDao.deleteById(doc.id)
                            }
                        }
                    }
                }
            }
    }

    private suspend fun downloadImageToLocal(remoteUrl: String, receiptId: String): Uri {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(remoteUrl)
        val photosDir = File(getApplication<Application>().filesDir, "camera_photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val localFile = File(photosDir, "$receiptId.jpg")
        storageRef.getFile(localFile).await()
        return Uri.fromFile(localFile)
    }

    private fun stopRemoteSync() {
        remoteListener?.remove()
        remoteListener = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        stopRemoteSync()
        super.onCleared()
    }

    fun logout() = auth.signOut()
}

package com.planner.tracker.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object CloudService {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    // User profile data
    data class UserProfile(
        val uid: String = "",
        val email: String = "",
        val displayName: String = "",
        val groupId: String? = null
    )

    // Group info data
    data class GroupInfo(
        val groupId: String = "",
        val name: String = "",
        val createdBy: String = "",
        val createdAt: Long = 0,
        val members: List<String> = emptyList()
    )

    // Shared Entry (represented on Firestore)
    data class SharedEntry(
        val id: String = "",
        val category: String = "",
        val minutes: Int = 0,
        val note: String = "",
        val photoUrl: String? = null,
        val userId: String = "",
        val userName: String = "",
        val date: Long = 0,
        val createdAt: Long = 0,
        val reactions: Map<String, String> = emptyMap()
    )

    // Auth Flows
    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // Get User Profile
    fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val docRef = firestore.collection("users").document(uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    // Register
    suspend fun registerUser(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val authResult = suspendCancellableCoroutine<Result<FirebaseUser>> { continuation ->
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(task.result?.user!!))
                        } else {
                            continuation.resume(Result.failure(task.exception ?: Exception("Registration failed")))
                        }
                    }
            }
            if (authResult.isSuccess) {
                val user = authResult.getOrThrow()
                // Save user profile to Firestore
                suspendCancellableCoroutine<Result<FirebaseUser>> { continuation ->
                    val profile = UserProfile(uid = user.uid, email = email, displayName = displayName)
                    firestore.collection("users").document(user.uid).set(profile)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                continuation.resume(Result.success(user))
                            } else {
                                continuation.resume(Result.failure(task.exception ?: Exception("Profile saving failed")))
                            }
                        }
                }
            } else {
                authResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return suspendCancellableCoroutine<Result<FirebaseUser>> { continuation ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.success(task.result?.user!!))
                    } else {
                        continuation.resume(Result.failure(task.exception ?: Exception("Login failed")))
                    }
                }
        }
    }

    // Logout
    fun logout() {
        auth.signOut()
    }

    // Create Group
    suspend fun createGroup(groupName: String, userId: String): Result<String> {
        return try {
            val code = generateGroupCode()
            val groupInfo = GroupInfo(
                groupId = code,
                name = groupName,
                createdBy = userId,
                createdAt = System.currentTimeMillis(),
                members = listOf(userId)
            )
            // Create group document
            val createResult = suspendCancellableCoroutine<Result<String>> { continuation ->
                firestore.collection("groups").document(code).set(groupInfo)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(code))
                        } else {
                            continuation.resume(Result.failure(task.exception ?: Exception("Group creation failed")))
                        }
                    }
            }
            if (createResult.isSuccess) {
                // Update user document
                suspendCancellableCoroutine<Result<String>> { continuation ->
                    firestore.collection("users").document(userId).update("groupId", code)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                continuation.resume(Result.success(code))
                            } else {
                                continuation.resume(Result.failure(task.exception ?: Exception("Failed to update user's group ID")))
                            }
                        }
                }
            } else {
                createResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Join Group
    suspend fun joinGroup(groupCode: String, userId: String): Result<String> {
        return try {
            val cleanCode = groupCode.trim().uppercase()
            // Verify group exists
            val groupSnapshot = suspendCancellableCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { continuation ->
                firestore.collection("groups").document(cleanCode).get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(task.result)
                        } else {
                            continuation.resume(null)
                        }
                    }
            }
            if (groupSnapshot == null || !groupSnapshot.exists()) {
                return Result.failure(Exception("유효하지 않은 초대 코드입니다."))
            }

            val group = groupSnapshot.toObject(GroupInfo::class.java)!!
            val newMembers = group.members.toMutableList()
            if (!newMembers.contains(userId)) {
                newMembers.add(userId)
            }

            // Update group members
            val updateGroupResult = suspendCancellableCoroutine<Result<String>> { continuation ->
                firestore.collection("groups").document(cleanCode).update("members", newMembers)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(cleanCode))
                        } else {
                            continuation.resume(Result.failure(task.exception ?: Exception("Failed to update group members")))
                        }
                    }
            }
            if (updateGroupResult.isSuccess) {
                // Update user document
                suspendCancellableCoroutine<Result<String>> { continuation ->
                    firestore.collection("users").document(userId).update("groupId", cleanCode)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                continuation.resume(Result.success(cleanCode))
                            } else {
                                continuation.resume(Result.failure(task.exception ?: Exception("Failed to update user's group ID")))
                            }
                        }
                }
            } else {
                updateGroupResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Leave Group
    suspend fun leaveGroup(userId: String, groupCode: String): Result<Unit> {
        return try {
            val groupSnapshot = suspendCancellableCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { continuation ->
                firestore.collection("groups").document(groupCode).get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(task.result)
                        } else {
                            continuation.resume(null)
                        }
                    }
            }
            if (groupSnapshot != null && groupSnapshot.exists()) {
                val group = groupSnapshot.toObject(GroupInfo::class.java)!!
                val newMembers = group.members.toMutableList()
                newMembers.remove(userId)
                
                // Update group members
                suspendCancellableCoroutine<Unit> { continuation ->
                    firestore.collection("groups").document(groupCode).update("members", newMembers)
                        .addOnCompleteListener { continuation.resume(Unit) }
                }
            }
            // Update user document
            suspendCancellableCoroutine<Result<Unit>> { continuation ->
                firestore.collection("users").document(userId).update("groupId", null)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(Unit))
                        } else {
                            continuation.resume(Result.failure(task.exception ?: Exception("Failed to clear group ID")))
                        }
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upload photo to Firebase Storage
    suspend fun uploadPhoto(groupId: String, localUri: Uri): Result<String> {
        val filename = "photo_${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("groups/$groupId/photos/$filename")
        return suspendCancellableCoroutine { continuation ->
            ref.putFile(localUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { uri ->
                    continuation.resume(Result.success(uri.toString()))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }

    // Upload shared entry to Firestore under group collection
    suspend fun shareEntry(entry: SharedEntry, groupId: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            firestore.collection("groups")
                .document(groupId)
                .collection("entries")
                .add(entry)
                .addOnSuccessListener { continuation.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> continuation.resume(Result.failure(e)) }
        }
    }

    // Sync entry to Firestore
    suspend fun syncEntry(groupId: String, entry: Entry, userId: String, userName: String): Result<Unit> {
        return suspendCancellableCoroutine<Result<Unit>> { continuation ->
            val documentId = entry.id.toString()
            val sharedEntry = SharedEntry(
                id = documentId,
                category = entry.category,
                minutes = entry.minutes,
                note = entry.note,
                photoUrl = entry.photoUrl,
                userId = userId,
                userName = userName,
                date = entry.date,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("groups").document(groupId)
                .collection("entries").document(documentId).set(sharedEntry)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(Result.failure(task.exception ?: Exception("Failed to sync entry")))
                    }
                }
        }
    }

    // Delete synced entry
    suspend fun deleteSyncedEntry(groupId: String, entryId: Long): Result<Unit> {
        return suspendCancellableCoroutine<Result<Unit>> { continuation ->
            firestore.collection("groups").document(groupId)
                .collection("entries").document(entryId.toString()).delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(Result.failure(task.exception ?: Exception("Failed to delete remote entry")))
                    }
                }
        }
    }

    // Subscribe to Shared Feed (Realtime)
    fun subscribeToFeed(groupId: String): Flow<List<SharedEntry>> = callbackFlow {
        val query = firestore.collection("groups").document(groupId)
            .collection("entries")
            .orderBy("date", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(SharedEntry::class.java)
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    // Add reaction to a shared entry
    suspend fun addReaction(groupId: String, entryId: String, userId: String, emoji: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val docRef = firestore.collection("groups").document(groupId)
                .collection("entries").document(entryId)
            docRef.update("reactions.$userId", emoji)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }

    // Helper: Generate a unique 6-digit uppercase code (e.g. GP-XXXXXX)
    private fun generateGroupCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        val randomString = (1..6)
            .map { allowedChars.random() }
            .joinToString("")
        return "GP-$randomString"
    }
}

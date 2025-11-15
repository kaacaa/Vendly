package com.katarina.vendly.data.user

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.katarina.vendly.domain.gamification.Points
import com.katarina.vendly.domain.model.user.User
import com.katarina.vendly.domain.model.user.UserStats
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun updateUserLocation(uid: String, latitude: Double, longitude: Double) {
        val userRef = db.collection("users").document(uid)
        val payload = mapOf(
            "uid" to uid,
            "latitude" to latitude,
            "longitude" to longitude,
            "lastSeenAt" to FieldValue.serverTimestamp()
        )
        try {
            userRef.set(payload, SetOptions.merge()).await()
        } catch (_: Exception) { }
    }

    suspend fun clearUserLocation(uid: String) {
        val userRef = db.collection("users").document(uid)
        val payload = mapOf(
            "latitude" to null,
            "longitude" to null,
            "lastSeenAt" to FieldValue.serverTimestamp()
        )
        try {
            userRef.set(payload, SetOptions.merge()).await()
        } catch (_: Exception) { }
    }

    fun observeUsers(onUsersUpdate: (List<Triple<String, String, LatLng>>) -> Unit) {
        db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val users = snapshot.documents.mapNotNull { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")
                    val name = doc.getString("fullName")
                        ?: doc.getString("displayName")
                        ?: doc.getString("email")?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                        ?: "Unknown"
                    if (lat != null && lng != null) Triple(uid, name, LatLng(lat, lng)) else null
                }
                onUsersUpdate(users)
            }
    }

    private fun DocumentSnapshot.num(field: String): Long =
        when (val v = get(field)) { is Number -> v.toLong() else -> 0L }

    private fun DocumentSnapshot.statLong(vararg keys: String): Long {
        val stats = get("stats") as? Map<*, *> ?: emptyMap<String, Any?>()
        for (k in keys) {
            when (val v = stats[k]) {
                is Number -> return v.toLong()
            }
        }
        return 0L
    }

    private fun DocumentSnapshot.toSafeUser(): User {
        val uid = getString("uid") ?: id
        val email = getString("email") ?: ""
        val fullName = getString("fullName") ?: (getString("displayName") ?: "")
        val phone = getString("phoneNumber") ?: ""
        val photo = getString("profileImageUrl")

        val points = num("points")

        val stats = UserStats(
            vendingMachinesAdded = statLong("vendingMachinesAdded", "vendingCreated").toInt(),
            statusUpdatesCount  = statLong("statusUpdatesCount", "statusUpdates").toInt()
        )

        return User(
            uid = uid,
            email = email,
            fullName = fullName,
            phoneNumber = phone,
            profileImageUrl = photo,
            points = points,
            stats = stats
        )
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val reg = db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                val u = if (snap != null && snap.exists()) snap.toSafeUser() else null
                trySend(u).isSuccess
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateProfile(
        uid: String,
        fullName: String,
        phoneNumber: String?,
        profileImageUrl: String?
    ) {
        val data = hashMapOf<String, Any?>(
            "uid" to uid,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "profileImageUrl" to profileImageUrl
        )
        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun getUserName(uid: String): String? {
        return try {
            val snap = db.collection("users").document(uid).get().await()
            snap.getString("fullName")
                ?: snap.getString("displayName")
                ?: snap.getString("email")?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun awardPointsForNewVendingMachine(
        uid: String,
        points: Long = Points.PER_VENDING_MACHINE
    ) = incrementUserCounters(
        uid = uid,
        pointsDelta = points,
        counters = mapOf("stats.vendingMachinesAdded" to 1L)
    )

    suspend fun awardPointsForStatusUpdate(
        uid: String,
        points: Long = Points.PER_STATUS_UPDATE
    ) = incrementUserCounters(
        uid = uid,
        pointsDelta = points,
        counters = mapOf("stats.statusUpdatesCount" to 1L)
    )

    private suspend fun incrementUserCounters(
        uid: String,
        pointsDelta: Long,
        counters: Map<String, Long>
    ) {
        val userRef = db.collection("users").document(uid)

        db.runTransaction { tx ->
            val snap = tx.get(userRef)

            val currentPoints = when (val p = snap.get("points")) {
                is Number -> p.toLong()
                else -> 0L
            }

            val stats = (snap.get("stats") as? Map<*, *>) ?: emptyMap<String, Any?>()

            fun stat(vararg keys: String): Long {
                for (k in keys) {
                    when (val v = stats[k]) {
                        is Number -> return v.toLong()
                    }
                }
                return 0L
            }

            var vending = stat("vendingMachinesAdded", "vendingCreated")
            var updates = stat("statusUpdatesCount", "statusUpdates")

            counters.forEach { (k, v) ->
                when (k) {
                    "stats.vendingMachinesAdded" -> vending += v
                    "stats.statusUpdatesCount" -> updates += v
                }
            }

            val nextPoints = currentPoints + pointsDelta

            val data = hashMapOf<String, Any?>(
                "uid" to uid,
                "points" to nextPoints,
                "lastActivityAt" to FieldValue.serverTimestamp(),
                "stats" to mapOf(
                    "vendingMachinesAdded" to vending,
                    "statusUpdatesCount" to updates
                )
            )

            tx.set(userRef, data, SetOptions.merge())

            tx.set(userRef, data, SetOptions.merge())
            null
        }.await()
    }

    private fun mapDocToLeaderboardUser(d: DocumentSnapshot): LeaderboardUser {
        val uid = d.getString("uid") ?: d.id

        fun firstNonBlank(vararg s: String?): String? =
            s.firstOrNull { !it.isNullOrBlank() }

        val name = firstNonBlank(
            d.getString("fullName"),
            d.getString("displayName"),
            d.getString("name"),
            d.getString("email")?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        ) ?: "Unknown"

        val photoUrl = firstNonBlank(
            d.getString("profileImageUrl"),
            d.getString("photoUrl"),
            d.getString("avatarUrl"),
            d.getString("imageUrl")
        )

        val points = when (val p = d.get("points")) {
            is Number -> p.toLong()
            else -> 0L
        }

        return LeaderboardUser(
            uid = uid,
            displayName = name,
            photoUrl = photoUrl,
            points = points
        )
    }

    fun observeLeaderboard(limit: Long = 10): Flow<List<LeaderboardUser>> = callbackFlow {
        val reg = db.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { mapDocToLeaderboardUser(it) }.orEmpty()
                trySend(list).isSuccess
            }
        awaitClose { reg.remove() }
    }

    fun observeMyRank(uid: String): Flow<Pair<Int, LeaderboardUser?>> = callbackFlow {
        val meRef = db.collection("users").document(uid)
        var aboveReg: ListenerRegistration? = null

        val meReg = meRef.addSnapshotListener { meSnap, _ ->
            val myPoints = when (val p = meSnap?.get("points")) {
                is Number -> p.toLong()
                else -> 0L
            }
            val meUser = meSnap?.let { mapDocToLeaderboardUser(it) }

            aboveReg?.remove()
            aboveReg = db.collection("users")
                .whereGreaterThan("points", myPoints)
                .addSnapshotListener { aboveSnap, _ ->
                    val rank = (aboveSnap?.size() ?: 0) + 1
                    trySend(rank to meUser).isSuccess
                }
        }

        awaitClose {
            meReg.remove()
            aboveReg?.remove()
        }
    }

    suspend fun getUsersLite(limit: Long = 1000): List<UserLite> {
        return try {
            val snap = db.collection("users")
                .limit(limit)
                .get()
                .await()

            snap.documents.map { d ->
                val uid = d.getString("uid") ?: d.id
                val name = d.getString("fullName")
                    ?: d.getString("displayName")
                    ?: d.getString("name")
                    ?: d.getString("email")?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                    ?: "Unknown"
                val photo = d.getString("profileImageUrl")
                    ?: d.getString("photoUrl")
                    ?: d.getString("avatarUrl")
                UserLite(uid = uid, fullName = name, profileImageUrl = photo)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
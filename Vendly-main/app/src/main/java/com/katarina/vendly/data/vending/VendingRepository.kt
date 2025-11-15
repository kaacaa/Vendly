package com.katarina.vendly.data.vending

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.katarina.vendly.domain.model.vm.VendingMachine
import com.katarina.vendly.domain.model.vm.VendingStatus
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "vending"

class VendingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun readMillis(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            is Timestamp -> value.toDate().time
            is Map<*, *> -> {
                val s = (value["seconds"] as? Number)?.toLong()
                val ns = (value["nanoseconds"] as? Number)?.toLong()
                if (s != null && ns != null) s * 1000 + ns / 1_000_000 else 0L
            }
            else -> 0L
        }
    }

    suspend fun getMachines(): List<VendingMachine> = try {
        val snap = db.collection(COLLECTION).get().await()
        snap.documents.map { d ->
            VendingMachine(
                id = d.id,
                name = d.getString("name") ?: "",
                productType = d.getString("productType") ?: "",
                status = VendingStatus.fromCode(d.getString("status")).code, // normalized
                latitude = d.getDouble("latitude") ?: 0.0,
                longitude = d.getDouble("longitude") ?: 0.0,
                imageUrl = d.getString("imageUrl") ?: "",
                addedByUserId = d.getString("addedByUserId") ?: "",
                createdAt = readMillis(d.get("createdAt")),
                updatedAt = readMillis(d.get("updatedAt"))
            )
        }
    } catch (_: Exception) { emptyList() }

    suspend fun getMachineById(id: String): VendingMachine? = try {
        val d = db.collection(COLLECTION).document(id).get().await()
        if (!d.exists()) null else VendingMachine(
            id = d.id,
            name = d.getString("name") ?: "",
            productType = d.getString("productType") ?: "",
            status = VendingStatus.fromCode(d.getString("status")).code, // normalized
            latitude = d.getDouble("latitude") ?: 0.0,
            longitude = d.getDouble("longitude") ?: 0.0,
            imageUrl = d.getString("imageUrl") ?: "",
            addedByUserId = d.getString("addedByUserId") ?: "",
            createdAt = readMillis(d.get("createdAt")),
            updatedAt = readMillis(d.get("updatedAt"))
        )
    } catch (_: Exception) { null }

    suspend fun addMachine(m: VendingMachine): String {
        val now = System.currentTimeMillis()
        val doc = db.collection(COLLECTION).document()
        val id = doc.id

        val payload = mapOf(
            "name" to m.name,
            "productType" to m.productType,
            "status" to VendingStatus.fromCode(m.status).code, // canonical
            "latitude" to m.latitude,
            "longitude" to m.longitude,
            "imageUrl" to m.imageUrl,
            "addedByUserId" to m.addedByUserId,
            "createdAt" to (if (m.createdAt == 0L) now else m.createdAt),
            "updatedAt" to (if (m.updatedAt == 0L) now else m.updatedAt)
        )

        doc.set(payload, SetOptions.merge()).await()
        return id
    }

    private val allowed = setOf("full", "low", "empty", "out_of_order")

    suspend fun updateStatus(id: String, newStatus: String, actorUid: String) {
        val canonical = VendingStatus.normalize(newStatus)
        require(canonical in allowed) { "Invalid status: $canonical" }

        db.collection(COLLECTION).document(id)
            .set(
                mapOf(
                    "status" to canonical,
                    "updatedAt" to System.currentTimeMillis(),
                    "lastUpdatedBy" to actorUid
                ),
                SetOptions.merge()
            )
            .await()
    }
}
package dita.dev.data

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query

abstract class FirestoreRepo {

    protected suspend fun deleteQueryBatch(firestore: Firestore, query: Query, batchSize: Int) {
        val snapshot = query.get().await()
        if (snapshot.isEmpty) {
            return
        }

        val batch = firestore.batch()
        snapshot.documents.forEach {
            batch.delete(it.reference)
        }

        val result = batch.commit().await()
        if (result.isEmpty()) {
            return
        }

        return deleteQueryBatch(firestore, query, batchSize)
    }
}
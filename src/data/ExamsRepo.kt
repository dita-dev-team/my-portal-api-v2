package dita.dev.data

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query

interface ExamsRepo {

    suspend fun getExamScheduleCount(): Int

    suspend fun clearExamSchedule()

    suspend fun uploadExamSchedule(schedule: List<Exam>)
}

class ExamsRepoImpl(private val firestore: Firestore) : ExamsRepo {
    private val collection = "exam_schedule"

    override suspend fun getExamScheduleCount(): Int {
        val querySnapshot = firestore.collection(collection).get().await()
        return if (querySnapshot.isEmpty) {
            0
        } else {
            querySnapshot.size()
        }
    }

    override suspend fun uploadExamSchedule(schedule: List<Exam>) {
        schedule.chunked(100).forEach { chunk ->
            writeToDb(chunk)
        }
    }

    override suspend fun clearExamSchedule() {
        val batchSize = 100
        val query = firestore.collection(collection).orderBy("__name__").limit(batchSize)
        return deleteQueryBatch(query, batchSize)
    }

    private suspend fun writeToDb(chunk: List<Exam>) {
        val batch = firestore.batch()
        chunk.forEach { item ->
            val ref = firestore.collection(collection).document()
            batch.set(ref, item)
        }
        batch.commit().await()
    }

    private suspend fun deleteQueryBatch(query: Query, batchSize: Int) {
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

        return deleteQueryBatch(query, batchSize)
    }


}
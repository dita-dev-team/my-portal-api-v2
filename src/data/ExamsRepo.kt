package dita.dev.data

import com.google.cloud.firestore.Firestore

interface ExamsRepo {

    suspend fun getExamScheduleCount(): Int

    suspend fun clearExamSchedule()

    suspend fun uploadExamSchedule(schedule: List<Exam>)
}

class ExamsRepoImpl(private val firestore: Firestore) : ExamsRepo, FirestoreRepo() {
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
        return deleteQueryBatch(firestore, query, batchSize)
    }

    private suspend fun writeToDb(chunk: List<Exam>) {
        val batch = firestore.batch()
        chunk.forEach { item ->
            val ref = firestore.collection(collection).document()
            batch.set(ref, item)
        }
        batch.commit().await()
    }

}
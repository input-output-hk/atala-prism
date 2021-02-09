package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.iohk.atala.prism.app.data.local.db.model.*

@Dao
abstract class ProofRequestDao : ActivityHistoryDao() {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertSync(proofRequest: ProofRequest): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertProofRequestCredentialsSync(list: List<ProofRequestCredential>): List<Long>

    @Transaction
    open fun insertSync(proofRequest: ProofRequest, credentials: List<Credential>): Long {
        val proofRequestId = insertSync(proofRequest)
        val credentialsReferences: List<ProofRequestCredential> = credentials.map {
            ProofRequestCredential(it.credentialId, proofRequestId)
        }
        insertProofRequestCredentialsSync(credentialsReferences)
        return proofRequestId
    }

    @Transaction
    open fun insertAllSync(proofRequests: List<Pair<ProofRequest, List<Credential>>>): List<Long> {
        return proofRequests.map {
            insertSync(it.first, it.second)
        }
    }

    @Query("SELECT * FROM proofRequests ORDER BY id asc")
    abstract fun all(): LiveData<List<ProofRequest>>

    @Query("SELECT * FROM proofRequests ORDER BY id asc")
    abstract fun allWithCredentials(): LiveData<List<ProofRequestWithContactAndCredentials>>

    @Query("SELECT * FROM proofRequests ORDER BY id asc")
    abstract suspend fun getAllWithCredentials(): List<ProofRequestWithContactAndCredentials>

    @Delete
    abstract suspend fun delete(proofRequest: ProofRequest)

    @Query("SELECT * from proofRequests where id = :id LIMIT 1")
    abstract suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials?
}
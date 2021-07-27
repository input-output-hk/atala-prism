package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.iohk.atala.prism.app.core.enums.CredentialType
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.KycRequest

@Dao
abstract class KycRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSync(kycRequest: KycRequest): Long

    @Query("SELECT * FROM kycRequests ORDER BY id asc LIMIT 1")
    abstract fun first(): LiveData<KycRequest?>

    @Query("SELECT * FROM kycRequests ORDER BY id asc LIMIT 1")
    abstract suspend fun firstSync(): KycRequest?

    @Update
    abstract suspend fun update(kycRequest: KycRequest)

    @Query("SELECT * FROM credentials WHERE credential_type = :type AND deleted = 0 order by id asc LIMIT 1")
    abstract fun kycCredential(
        type: String = CredentialType.KYC_CREDENTIAL.value
    ): LiveData<Credential?>
}

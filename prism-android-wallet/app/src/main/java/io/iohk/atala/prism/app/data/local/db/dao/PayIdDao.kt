package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.data.local.db.model.PayIdPublicKey

@Dao
abstract class PayIdDao {
    // for now the application will only be able to handle a single pay id
    @Query("SELECT * from payIds WHERE status = :status ORDER BY id ASC LIMIT 1")
    abstract suspend fun getPayIdByStatus(status: Int): PayId?

    @Query("SELECT * from payIds WHERE message_id = :messageID AND status = :status ORDER BY id ASC LIMIT 1")
    abstract suspend fun getPayIdByMessageIdAndStatus(messageID: String, status: Int): PayId?

    @Query("SELECT * from payIds WHERE status = :status ORDER BY id ASC LIMIT 1")
    abstract fun getPayIdByStatusLiveData(status: Int): LiveData<PayId?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun storePayId(payId: PayId): Long

    @Update
    abstract suspend fun updatePayId(payId: PayId)

    @Delete
    abstract suspend fun deletePayId(payId: PayId)

    @Query("SELECT COUNT(*) FROM payIdAddresses ORDER BY id")
    abstract fun totalOfPayIdAddresses(): LiveData<Int>

    @Query("SELECT * FROM payIdAddresses ORDER BY id ASC")
    abstract fun registeredPayIdAddresses(): LiveData<List<PayIdAddress>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun createPayIdAddress(payIdAddress: PayIdAddress): Long

    /* PayIdPublicKey */

    @Query("SELECT COUNT(*) FROM payIdPublicKeys ORDER BY id")
    abstract fun totalOfPayIdPublicKeys(): LiveData<Int>

    @Query("SELECT * FROM payIdPublicKeys ORDER BY id ASC")
    abstract fun registeredPayIdPublicKeys(): LiveData<List<PayIdPublicKey>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun createPayIdPublicKey(payIdPublicKey: PayIdPublicKey): Long
}

package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.iohk.atala.prism.app.data.local.db.model.KycRequest

@Dao
abstract class KycRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSync(kycRequest: KycRequest): Long

    @Query("SELECT * FROM kycRequests WHERE skipped = 0 ORDER BY id asc LIMIT 1")
    abstract fun firstNotSkipped(): LiveData<KycRequest?>

    @Query("SELECT * FROM kycRequests WHERE skipped = 0 ORDER BY id asc LIMIT 1")
    abstract suspend fun firstNotSkippedSync(): KycRequest?

    @Update
    abstract suspend fun update(kycRequest: KycRequest)
}

package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential

@Dao
abstract class ActivityHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivityHistory(activityHistory: ActivityHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivityHistories(activityHistories: List<ActivityHistory>): List<Long>

    @Query("SELECT * FROM activityHistories ORDER BY id ASC")
    abstract suspend fun getAllActivityHistories(): List<ActivityHistory>

    @Query("SELECT * FROM activityHistories ORDER BY date DESC, id")
    abstract fun activityHistories(): LiveData<List<ActivityHistoryWithContactAndCredential>>
}
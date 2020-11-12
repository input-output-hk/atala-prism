package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential

@Dao
abstract class ActivityHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivityHistory(activityHistory: ActivityHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivityHistories(activityHistories: List<ActivityHistory>): List<Long>

    @Query("SELECT * FROM activityHistories order by id asc")
    abstract suspend fun getAllActivityHistories(): List<ActivityHistory>
}
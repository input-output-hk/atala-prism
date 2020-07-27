package io.iohk.cvp.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import io.iohk.cvp.data.local.db.model.CredentialHistory;
import io.reactivex.Flowable;

@Dao
public interface CredentialHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CredentialHistory credentialHistory);

    @Query("SELECT * FROM credential_history  ORDER BY id ASC")
    Flowable<CredentialHistory> getAll();
}

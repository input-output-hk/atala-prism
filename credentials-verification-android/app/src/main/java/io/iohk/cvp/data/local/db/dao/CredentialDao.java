package io.iohk.cvp.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import io.iohk.cvp.data.local.db.model.Credential;
import io.reactivex.Flowable;

@Dao
public interface CredentialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Credential credential);

    @Query("SELECT * FROM credential  ORDER BY id ASC")
    Flowable<Credential> getAll();
}

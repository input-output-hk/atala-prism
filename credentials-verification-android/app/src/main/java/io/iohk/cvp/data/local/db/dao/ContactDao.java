package io.iohk.cvp.data.local.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import io.iohk.cvp.data.local.db.model.Contact;
import io.reactivex.Flowable;

@Dao
public interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Contact contact);

    @Query("SELECT * FROM contact ORDER BY id ASC")
    Flowable<Contact> getAll();
}

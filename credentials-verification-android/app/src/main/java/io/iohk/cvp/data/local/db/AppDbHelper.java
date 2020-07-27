package io.iohk.cvp.data.local.db;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.iohk.cvp.data.local.db.model.Contact;
import io.reactivex.Flowable;

@Singleton
public class AppDbHelper implements DbHelper {

    private final AppDatabase mAppDatabase;

    @Inject
    public AppDbHelper(AppDatabase appDatabase) {
        this.mAppDatabase = appDatabase;
    }

    @Override
    public long saveContact(Contact contact) {
        return mAppDatabase.contactDao().insert(contact);
    }

    @Override
    public Flowable<Contact> getAllContacts() {
        return mAppDatabase.contactDao().getAll();
    }
}

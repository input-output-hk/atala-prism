package io.iohk.cvp.data;

import javax.inject.Inject;

import io.iohk.cvp.data.local.db.DbHelper;
import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.cvp.data.local.remote.ApiHelper;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;
import io.iohk.prism.protos.ConnectorPublicKey;
import io.iohk.prism.protos.GetMessagesPaginatedResponse;
import io.reactivex.Flowable;
import io.reactivex.Observable;

public class AppDataManager implements DataManager {

    private final DbHelper mDbHelper;
    private final ApiHelper apiHelper;

    @Inject
    public AppDataManager(DbHelper dbHelper, ApiHelper apiHelper) {
        this.mDbHelper = dbHelper;
        this.apiHelper = apiHelper;
    }

    @Override
    public long saveContact(Contact contact) {
        return mDbHelper.saveContact(contact);
    }

    @Override
    public Flowable<Contact> getAllContacts() {
        return mDbHelper.getAllContacts();
    }

}

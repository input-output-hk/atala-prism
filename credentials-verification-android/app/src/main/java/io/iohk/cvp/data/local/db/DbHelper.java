package io.iohk.cvp.data.local.db;

import io.iohk.cvp.data.local.db.model.Contact;
import io.reactivex.Flowable;

public interface DbHelper {
    long saveContact(Contact contact);

    Flowable<Contact> getAllContacts();
}

package io.iohk.cvp.data.local.db.mappers;

import java.util.Date;

import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;
import io.iohk.prism.protos.ConnectionInfo;

public class ConnectionTokenToContact {

     static public Contact mapToContact(AddConnectionFromTokenResponse addConnectionFromTokenResponse) {

        Contact contact = new Contact();
        ConnectionInfo connectionInfo = addConnectionFromTokenResponse.getConnection();
        contact.connectionId = connectionInfo.getConnectionId();
        contact.dateCreated = System.currentTimeMillis();
        contact.did = addConnectionFromTokenResponse.getUserId();
        contact.name = connectionInfo.getParticipantInfo().getIssuer().getName();
        contact.token = connectionInfo.getToken();
        contact.userId = addConnectionFromTokenResponse.getUserId();
        return contact;
    }
}

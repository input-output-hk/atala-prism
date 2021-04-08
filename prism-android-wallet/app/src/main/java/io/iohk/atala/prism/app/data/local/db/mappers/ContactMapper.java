package io.iohk.atala.prism.app.data.local.db.mappers;

import io.iohk.atala.prism.app.data.local.db.model.Contact;
import io.iohk.atala.prism.protos.ConnectionInfo;

public class ContactMapper {

    static public Contact mapToContact(ConnectionInfo connectionInfo, String keyDerivationPath) {
        Contact contact = new Contact();
        contact.connectionId = connectionInfo.getConnectionId();
        contact.dateCreated = connectionInfo.getCreated();
        contact.did = connectionInfo.getParticipantDID();
        contact.name = connectionInfo.getParticipantName();
        contact.logo = connectionInfo.getParticipantLogo().toByteArray();
        contact.token = connectionInfo.getToken();
        contact.keyDerivationPath = keyDerivationPath;
        return contact;
    }
}
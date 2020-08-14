package io.iohk.cvp.data.local.db.mappers;

import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.HolderInfo;
import io.iohk.prism.protos.IssuerInfo;

public class ContactMapper {

     static public Contact mapToContact(AddConnectionFromTokenResponse addConnectionFromTokenResponse, String keyDerivationPath) {

        Contact contact = new Contact();
        ConnectionInfo connectionInfo = addConnectionFromTokenResponse.getConnection();
        IssuerInfo issuerInfo = connectionInfo.getParticipantInfo().getIssuer();
        HolderInfo holderInfo = connectionInfo.getParticipantInfo().getHolder();

        contact.connectionId = connectionInfo.getConnectionId();
        contact.dateCreated = System.currentTimeMillis();
        contact.did = issuerInfo.getDID() != null ? issuerInfo.getDID() : holderInfo.getDID();
        contact.name = issuerInfo.getName() != null ? issuerInfo.getName() : holderInfo.getName();
        contact.token = connectionInfo.getToken();
        contact.keyDerivationPath = keyDerivationPath;
        contact.logo = issuerInfo.getLogo().toByteArray();
        return contact;
    }
}

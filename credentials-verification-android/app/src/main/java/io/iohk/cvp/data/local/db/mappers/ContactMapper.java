package io.iohk.cvp.data.local.db.mappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.HolderInfo;
import io.iohk.prism.protos.IssuerInfo;
import io.iohk.prism.protos.VerifierInfo;

public class ContactMapper {

    static public Contact mapToContact(ConnectionInfo connectionInfo, String keyDerivationPath) {

        Contact contact = new Contact();
        IssuerInfo issuerInfo = connectionInfo.getParticipantInfo().getIssuer();
        HolderInfo holderInfo = connectionInfo.getParticipantInfo().getHolder();
        VerifierInfo verifierInfo = connectionInfo.getParticipantInfo().getVerifier();

        contact.connectionId = connectionInfo.getConnectionId();
        contact.dateCreated = System.currentTimeMillis();
        if (!issuerInfo.getDID().isEmpty()) {
            contact.did = issuerInfo.getDID();
            contact.name = issuerInfo.getName();
            contact.logo = issuerInfo.getLogo().toByteArray();
        } else if (!verifierInfo.getDID().isEmpty()) {
            contact.did = verifierInfo.getDID();
            contact.name = verifierInfo.getName();
            contact.logo = verifierInfo.getLogo().toByteArray();
        } else if (!holderInfo.getDID().isEmpty()) {
            contact.did = holderInfo.getDID();
            contact.name = holderInfo.getName();
        }
        contact.token = connectionInfo.getToken();
        contact.keyDerivationPath = keyDerivationPath;
        return contact;
    }

}

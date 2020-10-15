package io.iohk.cvp.data.local.db.mappers;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.protobuf.InvalidProtocolBufferException;

import io.iohk.cvp.utils.CredentialParse;
import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.atala.prism.protos.AtalaMessage;
import io.iohk.atala.prism.protos.Credential;
import io.iohk.atala.prism.protos.ReceivedMessage;

public class CredentialMapper {

    static public io.iohk.cvp.data.local.db.model.Credential mapToCredential(ReceivedMessage receivedMessage) {
        io.iohk.cvp.data.local.db.model.Credential credentialModel = new io.iohk.cvp.data.local.db.model.Credential();
        try {

            Credential credential = AtalaMessage.parseFrom(receivedMessage.getMessage())
                    .getIssuerSentCredential().getCredential();

            CredentialDto credentialDto = CredentialParse.parse(credential);

            credentialModel.credentialId = receivedMessage.getId();
            credentialModel.dateReceived = System.currentTimeMillis();
            credentialModel.credentialEncoded = credential.toByteString();
            credentialModel.issuerId = credentialDto.getIssuer().getId();
            credentialModel.issuerName = credentialDto.getIssuer().getName();
            credentialModel.credentialType = credential.getTypeId();
            credentialModel.connectionId = receivedMessage.getConnectionId();
            credentialModel.credentialDocument = credential.getCredentialDocument();
        } catch (InvalidProtocolBufferException ex) {
            FirebaseCrashlytics.getInstance().recordException(ex);
        }
        return credentialModel;
    }
}
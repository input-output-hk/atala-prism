package io.iohk.atala.prism.app.data.local.db.mappers;

import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import io.iohk.atala.prism.app.core.exception.ErrorCode;
import io.iohk.atala.prism.app.core.exception.InvalidAtalaMessageCaseException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import io.iohk.atala.prism.app.data.local.db.model.Contact;
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential;
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential;
import io.iohk.atala.prism.protos.AtalaMessage;
import io.iohk.atala.prism.protos.PlainTextCredential;
import io.iohk.atala.prism.protos.ReceivedMessage;

public class CredentialMapper {

    static public io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential mapToCredential(ReceivedMessage receivedMessage, String credentialId, String connectionId, Long received, Contact issuer) throws InvalidProtocolBufferException, InvalidAtalaMessageCaseException {
        AtalaMessage atalaMessage = AtalaMessage.parseFrom(receivedMessage.getMessage());

        if (atalaMessage.getMessageCase() == AtalaMessage.MessageCase.PLAIN_CREDENTIAL) {
            PlainTextCredential plainTextCredential = atalaMessage.getPlainCredential();
            String encodedCredential = plainTextCredential.getEncodedCredential();
            PlainTextCredentialJson plainTextCredentialJson = parsePlainTextCredential(encodedCredential);
            io.iohk.atala.prism.app.data.local.db.model.Credential credentialModel = new io.iohk.atala.prism.app.data.local.db.model.Credential();
            credentialModel.credentialId = credentialId;
            credentialModel.dateReceived = received;
            credentialModel.issuerName = issuer.name;
            credentialModel.issuerId = issuer.did;
            credentialModel.credentialType = plainTextCredentialJson.getCredentialType();
            credentialModel.connectionId = connectionId;
            // All the bytes of the complete AtalaMessage are stored separately
            EncodedCredential encodedCredentialObj = new EncodedCredential();
            encodedCredentialObj.credentialEncoded = receivedMessage.getMessage();
            encodedCredentialObj.credentialId = credentialId;
            return new CredentialWithEncodedCredential(credentialModel, encodedCredentialObj);
        } else {
            Log.e("Got Atala message", atalaMessage.getMessageCase().toString());
            throw new InvalidAtalaMessageCaseException(ErrorCode.INVALID_ATALA_MESSAGE_CASE);
        }

    }


    public static PlainTextCredentialJson parsePlainTextCredential(String encodedCredential) {
        try {
            String base64Json = encodedCredential.split("\\.")[0];
            byte[] bytes = Base64.decode(base64Json, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String messageString = new String(bytes, StandardCharsets.UTF_8);
            JSONObject jsonMessage = new JSONObject(messageString);
            String jsonString = jsonMessage.getString("credentialSubject");
            Gson gson = new GsonBuilder().create();

            return gson.fromJson(jsonString, PlainTextCredentialJson.class);
        } catch (Exception ex) {
            Log.e("Error in parsePlainTextCredential:", ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

}
package io.iohk.atala.prism.app.data.local.db.mappers;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import io.iohk.atala.prism.app.core.enums.CredentialType;
import io.iohk.atala.prism.app.data.local.db.model.Contact;
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential;
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential;
import io.iohk.atala.prism.protos.AtalaMessage;
import io.iohk.atala.prism.protos.Credential;
import io.iohk.atala.prism.protos.IssuerSentCredential;
import io.iohk.atala.prism.protos.PlainTextCredential;
import io.iohk.atala.prism.protos.ReceivedMessage;

public class CredentialMapper {

    static public Boolean isACredentialMessage(AtalaMessage atalaMessage) {
        switch (atalaMessage.getMessageCase()) {
            case ISSUER_SENT_CREDENTIAL:
            case PLAIN_CREDENTIAL:
                return true;
            default:
                return false;
        }
    }

    static public io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential mapToCredential(ReceivedMessage receivedMessage, String credentialId, String connectionId, Long received, Contact issuer) throws InvalidProtocolBufferException {
        AtalaMessage atalaMessage = AtalaMessage.parseFrom(receivedMessage.getMessage());
        switch (atalaMessage.getMessageCase()) {
            case PLAIN_CREDENTIAL:
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
            case ISSUER_SENT_CREDENTIAL:
                // support for int demo credentials
                return mapToCredential(atalaMessage.getIssuerSentCredential(), credentialId, connectionId, received);
            default:
                return new CredentialWithEncodedCredential(new io.iohk.atala.prism.app.data.local.db.model.Credential(), new EncodedCredential());
        }
    }

    /**
     * Support for Demo Credential Format
     */
    static private io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential mapToCredential(IssuerSentCredential issuerSentCredential, String credentialId, String connectionId, Long received) {
        io.iohk.atala.prism.app.data.local.db.model.Credential credentialModel = new io.iohk.atala.prism.app.data.local.db.model.Credential();
        Credential credential = issuerSentCredential.getCredential();
        IssuerInfo issuerInfo = obtainIssuerInfo(issuerSentCredential);
        credentialModel.credentialId = credentialId;
        credentialModel.dateReceived = received;
        credentialModel.issuerId = issuerInfo.issuerId;
        credentialModel.issuerName = issuerInfo.issuerName;
        credentialModel.credentialType = credential.getTypeId();
        credentialModel.connectionId = connectionId;
        // All the bytes of the complete Credential are stored separately
        EncodedCredential encodedCredentialObj = new EncodedCredential();
        encodedCredentialObj.credentialEncoded = credential.toByteString();
        encodedCredentialObj.credentialId = credentialId;
        return new CredentialWithEncodedCredential(credentialModel, encodedCredentialObj);
    }

    public static PlainTextCredentialJson parsePlainTextCredential(String encodedCredential) {
        String base64Json = encodedCredential.split("\\.")[0];
        byte[] bytes = Base64.decode(base64Json, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        String messageString = new String(bytes, StandardCharsets.UTF_8);
        try {
            JSONObject jsonMessage = new JSONObject(messageString);
            String jsonString = jsonMessage.getString("credentialSubject");
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(jsonString, PlainTextCredentialJson.class);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static IssuerInfo obtainIssuerInfo(IssuerSentCredential issuerSentCredential) {
        try {
            JSONObject jsonObject = new JSONObject(issuerSentCredential.getCredential().getCredentialDocument());
            JSONObject issuerObj = jsonObject.getJSONObject("issuer");
            String issuerId = issuerObj.getString("id");
            String issuerName = issuerObj.getString("name");
            return new IssuerInfo(issuerId, issuerName);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static class IssuerInfo {
        public String issuerName;
        public String issuerId;

        IssuerInfo(String issuerId, String issuerName) {
            this.issuerId = issuerId;
            this.issuerName = issuerName;
        }
    }

    public static Boolean isADemoCredential(io.iohk.atala.prism.app.data.local.db.model.Credential credential) {
        CredentialType credentialType = CredentialType.getByValue(credential.credentialType).get();
        return credentialType.equals(CredentialType.DEMO_EMPLOYMENT_CREDENTIAL)
                || credentialType.equals(CredentialType.DEMO_DEGREE_CREDENTIAL)
                || credentialType.equals(CredentialType.DEMO_ID_CREDENTIAL)
                || credentialType.equals(CredentialType.DEMO_INSURANCE_CREDENTIAL);
    }
}
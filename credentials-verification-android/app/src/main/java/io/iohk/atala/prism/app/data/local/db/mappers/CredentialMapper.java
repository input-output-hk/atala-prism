package io.iohk.atala.prism.app.data.local.db.mappers;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import io.iohk.atala.prism.app.data.local.db.model.Contact;
import io.iohk.atala.prism.protos.AtalaMessage;
import io.iohk.atala.prism.protos.Credential;
import io.iohk.atala.prism.protos.IssuerSentCredential;
import io.iohk.atala.prism.protos.PlainTextCredential;

public class CredentialMapper {

    static public Boolean isACredentialMessage(AtalaMessage atalaMessage) {
        switch (atalaMessage.getMessageCase()) {
            case ISSUERSENTCREDENTIAL:
            case PLAINCREDENTIAL:
                return true;
            default:
                return false;
        }
    }

    static public io.iohk.atala.prism.app.data.local.db.model.Credential mapToCredential(AtalaMessage atalaMessage, String credentialId, String connectionId, Long received, Contact issuer) {
        switch (atalaMessage.getMessageCase()) {
            case PLAINCREDENTIAL:
                PlainTextCredential plainTextCredential = atalaMessage.getPlainCredential();
                String encodedCredential = plainTextCredential.getEncodedCredential();
                PlainTextCredentialJson plainTextCredentialJson = parsePlainTextCredential(encodedCredential);
                io.iohk.atala.prism.app.data.local.db.model.Credential credentialModel = new io.iohk.atala.prism.app.data.local.db.model.Credential();
                credentialModel.credentialId = credentialId;
                credentialModel.dateReceived = received;
                credentialModel.credentialEncoded = atalaMessage.toByteString();
                credentialModel.issuerName = issuer.name;
                credentialModel.issuerId = issuer.did;
                credentialModel.credentialType = plainTextCredentialJson.getCredentialType();
                credentialModel.connectionId = connectionId;
                credentialModel.credentialDocument = encodedCredential;
                credentialModel.htmlView = getHtmlFromCredential(plainTextCredential);
                return credentialModel;
            case ISSUERSENTCREDENTIAL:
                // support for int demo credentials
                return mapToCredential(atalaMessage.getIssuerSentCredential(), credentialId, connectionId, received);
            default:
                return new io.iohk.atala.prism.app.data.local.db.model.Credential();
        }
    }

    static private io.iohk.atala.prism.app.data.local.db.model.Credential mapToCredential(IssuerSentCredential issuerSentCredential, String credentialId, String connectionId, Long received) {
        io.iohk.atala.prism.app.data.local.db.model.Credential credentialModel = new io.iohk.atala.prism.app.data.local.db.model.Credential();
        Credential credential = issuerSentCredential.getCredential();
        IssuerInfo issuerInfo = obtainIssuerInfo(issuerSentCredential);
        credentialModel.credentialId = credentialId;
        credentialModel.dateReceived = received;
        credentialModel.credentialEncoded = credential.toByteString();
        credentialModel.issuerId = issuerInfo.issuerId;
        credentialModel.issuerName = issuerInfo.issuerName;
        credentialModel.credentialType = credential.getTypeId();
        credentialModel.connectionId = connectionId;
        credentialModel.credentialDocument = credential.getCredentialDocument();
        return credentialModel;
    }

    public static PlainTextCredentialJson parsePlainTextCredential(String encodedCredential) {
        String base64Json = encodedCredential.split("\\.")[0];
        byte[] bytes = Base64.decode(base64Json, Base64.DEFAULT);
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

    private static String getHtmlFromCredential(PlainTextCredential plainTextCredential) {
        PlainTextCredentialJson plainTextCredentialJson = parsePlainTextCredential(plainTextCredential.getEncodedCredentialBytes().toStringUtf8());
        return plainTextCredentialJson.getHtml();
    }

    private static class IssuerInfo {
        public String issuerName;
        public String issuerId;

        IssuerInfo(String issuerId, String issuerName) {
            this.issuerId = issuerId;
            this.issuerName = issuerName;
        }
    }
}
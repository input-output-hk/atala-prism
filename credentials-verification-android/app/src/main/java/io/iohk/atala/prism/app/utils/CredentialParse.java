package io.iohk.atala.prism.app.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.iohk.atala.prism.app.viewmodel.dtos.CredentialDto;
import io.iohk.atala.prism.protos.Credential;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CredentialParse {

    public static CredentialDto parse(Credential credential) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").disableHtmlEscaping().create();
        CredentialDto dto = gson.fromJson(credential.getCredentialDocument(), CredentialDto.class);
        dto.setCredentialType(credential.getTypeId());
        return dto;
    }

    public static CredentialDto parse(String credentialType, String credentialDocument) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").disableHtmlEscaping().create();
        CredentialDto dto = gson.fromJson(credentialDocument, CredentialDto.class);
        dto.setCredentialType(credentialType);
        return dto;
    }

}
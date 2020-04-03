package io.iohk.cvp.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.iohk.cvp.viewmodel.dtos.CredentialDto;
import io.iohk.prism.protos.Credential;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CredentialParse {

    public static CredentialDto parse(Credential credential) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").disableHtmlEscaping().create();
        CredentialDto dto = gson.fromJson(credential.getCredentialDocument(), CredentialDto.class);
        dto.setCredentialType(credential.getTypeId());
        return dto;
    }

}

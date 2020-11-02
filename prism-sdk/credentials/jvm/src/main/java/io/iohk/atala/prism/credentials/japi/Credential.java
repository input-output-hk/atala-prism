package io.iohk.atala.prism.credentials.japi;

import io.iohk.atala.prism.crypto.japi.EC;
import io.iohk.atala.prism.crypto.japi.ECFacade;
import io.iohk.atala.prism.crypto.japi.ECFacade$;

public interface Credential {
    byte[] getContentBytes();
    CredentialContent getContent();

    static Credential parse(String credential) {
        return JsonBasedCredentialFacade$.MODULE$.parse(credential);
    }
}

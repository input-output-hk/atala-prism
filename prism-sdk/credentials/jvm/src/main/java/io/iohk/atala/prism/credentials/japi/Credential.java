package io.iohk.atala.prism.credentials.japi;

public interface Credential {
    byte[] getContentBytes();
    CredentialContent getContent();

    static Credential parse(String credential) throws CredentialParsingException {
        return JsonBasedCredentialFacade$.MODULE$.parse(credential);
    }
}

package io.iohk.atala.prism.credentials.japi;

import io.iohk.atala.prism.crypto.japi.ECPublicKey;

import java.util.Optional;

public class KeyData {
    // TODO: DO NOT USE ECPublicKey
    private final ECPublicKey publicKey;
    private final TimestampInfo addedOn;
    private final Optional<TimestampInfo> revokedOn;

    public KeyData(ECPublicKey publicKey,
                   TimestampInfo addedOn,
                   Optional<TimestampInfo> revokedOn) {
        this.publicKey = publicKey;
        this.addedOn = addedOn;
        this.revokedOn = revokedOn;
    }

    public ECPublicKey getPublicKey() {
        return publicKey;
    }

    public TimestampInfo getAddedOn() {
        return addedOn;
    }

    public Optional<TimestampInfo> getRevokedOn() {
        return revokedOn;
    }
}

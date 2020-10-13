package io.iohk.atala.credentials.japi;

import java.util.Optional;

public class CredentialData {
    private final TimestampInfo issuedOn;
    private final Optional<TimestampInfo> revokedOn;

    public CredentialData(TimestampInfo issuedOn, Optional<TimestampInfo> revokedOn) {
        this.issuedOn = issuedOn;
        this.revokedOn = revokedOn;
    }

    public TimestampInfo getIssuedOn() {
        return issuedOn;
    }

    public Optional<TimestampInfo> getRevokedOn() {
        return revokedOn;
    }
}

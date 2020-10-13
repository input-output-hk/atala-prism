package io.iohk.atala.credentials.japi;

import java.time.Instant;

public class TimestampInfo {
    // timestamp provided from the underlying blockchain
    private final Instant atalaBlockTimestamp;
    // transaction index inside the underlying blockchain block
    private final int atalaBlockSequenceNumber;
    private final int operationSequenceNumber;

    public TimestampInfo(Instant atalaBlockTimestamp, int atalaBlockSequenceNumber, int operationSequenceNumber) {
        this.atalaBlockTimestamp = atalaBlockTimestamp;
        this.atalaBlockSequenceNumber = atalaBlockSequenceNumber;
        this.operationSequenceNumber = operationSequenceNumber;
    }

    public Instant getAtalaBlockTimestamp() {
        return atalaBlockTimestamp;
    }

    public int getAtalaBlockSequenceNumber() {
        return atalaBlockSequenceNumber;
    }

    public int getOperationSequenceNumber() {
        return operationSequenceNumber;
    }
}

package io.iohk.cvp.grpc;

import io.iohk.prism.protos.ParticipantInfo;
import lombok.Data;

@Data
public class ParticipantInfoResponse {
    private final ParticipantInfo participantInfo;
    private final String token;
    private final Boolean alreadyAdded;

    public ParticipantInfoResponse(ParticipantInfo participantInfo, String token, Boolean alreadyAdded) {
        this.token = token;
        this.participantInfo = participantInfo;
        this.alreadyAdded = alreadyAdded;
    }
}

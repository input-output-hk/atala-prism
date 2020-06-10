package io.iohk.cvp.grpc;

import io.iohk.prism.protos.ParticipantInfo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class ParticipantInfoResponse {
    ParticipantInfo participantInfo;
    String token;
    public ParticipantInfoResponse(ParticipantInfo participantInfo, String token) {
        this.token = token;
        this.participantInfo = participantInfo;
    }
}

package io.iohk.cvp.viewmodel.dtos;

import io.iohk.prism.protos.ConnectionInfo;
import lombok.Data;

@Data
public class ConnectionListable {

  private final String connectionId;
  private final String verifierName;
  private final String issuerName;
  private final String did;
  private final byte[] verifierLogo;
  private Boolean isSelected = false;

  public ConnectionListable(ConnectionInfo connectionInfo) {
    this.connectionId = connectionInfo.getConnectionId();
    this.verifierName = connectionInfo.getParticipantInfo().getVerifier()
        .getName();
    this.did = connectionInfo.getParticipantInfo().getIssuer().getDID();
    this.issuerName = connectionInfo.getParticipantInfo().getIssuer()
            .getName();
    this.verifierLogo = connectionInfo.getParticipantInfo().getVerifier().getLogo().toByteArray();
  }

  public String getDid() {
    return did;
  }
}

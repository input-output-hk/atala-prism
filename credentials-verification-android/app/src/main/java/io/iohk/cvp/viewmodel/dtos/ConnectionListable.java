package io.iohk.cvp.viewmodel.dtos;

import io.iohk.cvp.io.connector.ConnectionInfo;
import lombok.Data;

@Data
public class ConnectionListable {

  private final String connectionId;
  private final String verifierName;
  private Boolean isSelected = false;

  public ConnectionListable(ConnectionInfo connectionInfo) {
    this.connectionId = connectionInfo.getConnectionId();
    this.verifierName = connectionInfo.getParticipantInfo().getVerifier()
        .getName();
  }
}

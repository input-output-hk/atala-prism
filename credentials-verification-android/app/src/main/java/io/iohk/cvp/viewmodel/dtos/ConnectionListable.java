package io.iohk.cvp.viewmodel.dtos;

import io.iohk.cvp.io.connector.ConnectionInfo;
import lombok.Getter;
import lombok.Setter;

public class ConnectionListable {

  private final String title;

  @Setter
  @Getter
  private Boolean isSelected = false;

  public ConnectionListable(ConnectionInfo connectionInfo) {
    this.title = connectionInfo.getConnectionId();
  }
}

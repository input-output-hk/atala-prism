package io.iohk.cvp.viewmodel.dtos;

import java.util.Optional;

import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.atala.prism.protos.ConnectionInfo;
import io.iohk.atala.prism.protos.IssuerInfo;
import io.iohk.atala.prism.protos.ParticipantInfo;
import io.iohk.atala.prism.protos.VerifierInfo;
import lombok.Data;

@Data
public class ConnectionListable {

  private final String connectionId;
  private final String name;
  private final String did;
  private final String keyderivationPath;
  private final byte[] logo;
  private Boolean isSelected = false;


  public ConnectionListable(Contact contact) {
    this.connectionId = contact.connectionId;
    this.name = contact.name;
    this.did = contact.did;
    this.logo = contact.logo;
    this.keyderivationPath = contact.keyDerivationPath;
  }

  public String getKeyDerivationPath(){
    return keyderivationPath;
  }
  public String getConnectionIdValue(){
    return connectionId;
  }

  public String getDid() {
    return did;
  }
}

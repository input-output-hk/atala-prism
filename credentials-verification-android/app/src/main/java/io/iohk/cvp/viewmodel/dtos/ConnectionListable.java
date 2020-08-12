package io.iohk.cvp.viewmodel.dtos;

import java.util.Optional;

import io.iohk.cvp.data.local.db.model.Contact;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.IssuerInfo;
import io.iohk.prism.protos.ParticipantInfo;
import io.iohk.prism.protos.VerifierInfo;
import lombok.Data;

@Data
public class ConnectionListable {

  private final String connectionId;
  private final String name;
  private final String did;
  private String userId = null;
  private final byte[] logo;
  private Boolean isSelected = false;

  public ConnectionListable(ConnectionInfo connectionInfo) {
    this.connectionId = connectionInfo.getConnectionId();

    if (Optional.ofNullable(connectionInfo.getParticipantInfo()).map(ParticipantInfo::getIssuer).map(IssuerInfo::getName).isPresent()) {
      IssuerInfo issuer = connectionInfo.getParticipantInfo().getIssuer();
      this.name = issuer.getName();
      this.did = issuer.getDID();
      this.logo = issuer.getLogo().toByteArray();
    } else {
      VerifierInfo verifier = connectionInfo.getParticipantInfo().getVerifier();
      this.name = verifier.getName();
      this.did = verifier.getDID();
      this.logo = verifier.getLogo().toByteArray();
    }
  }

  public ConnectionListable(Contact contact) {
    this.connectionId = contact.connectionId;
    this.name = contact.name;
    this.did = contact.did;
    this.logo = contact.logo;
    this.userId = contact.userId;
  }

  public String getUserIdValue(){
    return userId;
  }
  public String getConnectionIdValue(){
    return connectionId;
  }

  public String getDid() {
    return did;
  }
}

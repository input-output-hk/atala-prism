package io.iohk.cvp.viewmodel.dtos;

import lombok.Data;
import lombok.Getter;

@Data
public class CredentialIssuer {

  @Getter
  private String id;

  @Getter
  private String name;

}

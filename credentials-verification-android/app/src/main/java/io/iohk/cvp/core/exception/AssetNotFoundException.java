package io.iohk.cvp.core.exception;

import lombok.Getter;

public class AssetNotFoundException extends Exception {

  @Getter
  private final ErrorCode code;

  public AssetNotFoundException(String message, ErrorCode code) {
    super(message);
    this.code = code;
  }
}

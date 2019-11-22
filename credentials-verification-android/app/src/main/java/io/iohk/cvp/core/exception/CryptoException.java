package io.iohk.cvp.core.exception;

import lombok.Getter;

public class CryptoException extends Exception {

  @Getter
  private final ErrorCode code;

  public CryptoException(String message, ErrorCode code) {
    super(message);
    this.code = code;
  }
}

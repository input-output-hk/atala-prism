package io.iohk.cvp.core.exception;

import lombok.Getter;

public class SharedPrefencesDataNotFoundException extends Exception {

  @Getter
  private final ErrorCode code;

  public SharedPrefencesDataNotFoundException(String message, ErrorCode code) {
    super(message);
    this.code = code;
  }
}

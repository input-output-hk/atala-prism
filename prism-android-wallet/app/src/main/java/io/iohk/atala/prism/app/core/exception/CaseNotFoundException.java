package io.iohk.atala.prism.app.core.exception;

public class CaseNotFoundException extends Exception {

  private final ErrorCode code;

  public CaseNotFoundException(String message, ErrorCode code) {
    super(message);
    this.code = code;
  }

  public ErrorCode getCode() {
    return this.code;
  }

}

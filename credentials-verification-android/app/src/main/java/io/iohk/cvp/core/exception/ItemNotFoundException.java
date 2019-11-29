package io.iohk.cvp.core.exception;

public class ItemNotFoundException extends Exception {

  private final ErrorCode code;

  public ItemNotFoundException(String message, ErrorCode code) {
    super(message);
    this.code = code;
  }

  public ErrorCode getCode() {
    return this.code;
  }

}

package io.iohk.cvp.core.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

  STEP_NOT_FOUND(100, "Tutorial step not found");

  private int code;
  private String description;

  ErrorCode(int code, String description) {
    this.code = code;
    this.description = description;
  }
}

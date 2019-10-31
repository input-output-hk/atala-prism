package io.iohk.cvp.core.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

  ASSET_NOT_FOUND(1, "Asset not found"),

  STEP_NOT_FOUND(100, "Tutorial step not found"),
  TAB_NOT_FOUND(101, "Tab item not found"),

  FRAGMENT_NOT_FOUND(200, "Fragment not found");


  private int code;
  private String description;

  ErrorCode(int code, String description) {
    this.code = code;
    this.description = description;
  }
}

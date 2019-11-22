package io.iohk.cvp.core.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

  ASSET_NOT_FOUND(1, "Asset not found"),
  STEP_NOT_FOUND(100, "Tutorial step not found"),
  TAB_NOT_FOUND(101, "Tab item not found"),
  FRAGMENT_NOT_FOUND(200, "Fragment not found"),
  PRIVATE_KEY_NOT_FOUND(300, "Private key not found on shared preferences"),
  CRYPTO_KEY_FACTORY_NOT_INITIALIZED(400, "Key factory failed to initialize"),
  CRYPTO_UNKNOWN_PRIVATE_KEY_TYPE(401, "Private key isn't an instance of BCECPrivateKey"),
  CRYPTO_UNKNOWN_PUBLIC_KEY_TYPE(402, "Private key isn't an instance of BCECPublicKey");

  private int code;
  private String description;

  ErrorCode(int code, String description) {
    this.code = code;
    this.description = description;
  }
}

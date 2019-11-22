package io.iohk.cvp.views;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.utils.KeyStoreUtils;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Preferences {

  private static final String MY_PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION";
  private static final String PK_KEY = "wallet_pk";
  private Context context;

  public void savePrivateKey(byte[] pk) {
    KeyStoreUtils keyStoreUtils = new KeyStoreUtils();
    keyStoreUtils.generateKey();
    String encPk = keyStoreUtils.encryptData(pk);

    SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        .edit();
    editor.putString(PK_KEY, encPk);
    editor.apply();
  }

  public byte[] getPrivateKey() throws SharedPrefencesDataNotFoundException {
    KeyStoreUtils keyStoreUtils = new KeyStoreUtils();
    SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    Optional<String> pk = Optional.ofNullable(sharedPreferences.getString(PK_KEY, null));
    return keyStoreUtils.decryptData(Base64
        .decode(pk.orElseThrow(
            () -> new SharedPrefencesDataNotFoundException(
                "PrivateKey not found on shared preferences",
                ErrorCode.PRIVATE_KEY_NOT_FOUND)),
            Base64.DEFAULT));
  }
}

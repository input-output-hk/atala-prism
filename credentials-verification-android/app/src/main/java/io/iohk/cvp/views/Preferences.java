package io.iohk.cvp.views;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Preferences {

  private static final String MY_PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION";
  private static final String PK_KEY = "pk";
  private Context context;

  // TODO: receive and save correctly the symmetrical keys
  public void savePrivateKey(String pk) {
    // this is not a secure or correct way to save the symmetrical keys, but should be really close to this
    SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        .edit();
    editor.putString(PK_KEY, "this is the pk");
    editor.apply();
  }

  public Optional<String> getPrivateKey() {
    SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    return Optional.ofNullable(sharedPreferences.getString(PK_KEY, null));
  }
}

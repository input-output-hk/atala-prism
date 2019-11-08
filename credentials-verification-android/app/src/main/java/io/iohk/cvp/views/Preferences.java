package io.iohk.cvp.views;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Preferences {

  private static final String MY_PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION";

  private Context context;

  public void savePrivateKey(String pk) {
    // TODO
    SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        .edit();
    editor.putString("pk", "this is the pk");
    editor.apply();
  }

  public Optional<String> getPrivateKey() {
    SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    return Optional.ofNullable(sharedPreferences.getString("pk", null));
  }
}

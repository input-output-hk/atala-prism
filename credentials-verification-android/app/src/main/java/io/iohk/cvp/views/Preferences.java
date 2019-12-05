package io.iohk.cvp.views;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;
import com.google.protobuf.ByteString;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.io.connector.AddConnectionFromTokenResponse;
import io.iohk.cvp.utils.KeyStoreUtils;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Preferences {

  private static final String MY_PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION";
  private static final String PK_KEY = "wallet_pk";
  public static final String ACCEPTED_MESSAGES_KEY = "accepted_messages";
  public static final String REJECTED_MESSAGES_KEY = "rejected_messages";
  private static final String USER_ID_LIST_KEY = "user_id";
  private static final String CONNECTION_USER_ID_KEY = "connection_id_user_id";
  private static final String CONNECTION_LOGO_KEY = "connection_logo";
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

  public void saveMessage(String messageId, String prefKey) {
    editStringSet(messageId, prefKey);
  }

  public Set<String> getStoredMessages(String messageTypeKey) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    return sharedPreferences.getStringSet(messageTypeKey, new HashSet<>());
  }

  public void saveUserId(String userId) {
    editStringSet(userId, USER_ID_LIST_KEY);
  }

  public Set<String> getUserIds() {
    SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    return sharedPreferences
        .getStringSet(USER_ID_LIST_KEY, new HashSet<>());
  }

  private void editStringSet(String valueToAdd, String prefKey) {
    SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    Set<String> newItems = new HashSet<>(prefs.getStringSet(prefKey, new HashSet<>()));
    newItems.add(valueToAdd);
    Editor editor = prefs.edit();
    editor.putStringSet(prefKey, newItems);
    editor.apply();
  }

  public void saveConnectionWithUser(String connectionId, String userId) {
    SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putString(connectionId.concat(CONNECTION_USER_ID_KEY), userId);
    editor.apply();
  }

  public Optional<String> getUserIdByConnection(String connectionId) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    return Optional.ofNullable(sharedPreferences
        .getString(connectionId.concat(CONNECTION_USER_ID_KEY), null));
  }

  public void saveConnectionWithLogo(String connectionId, ByteString logo) {
    SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putString(connectionId.concat(CONNECTION_LOGO_KEY),
        Base64.encodeToString(logo.toByteArray(), Base64.DEFAULT));
    editor.apply();
  }

  public byte[] getConnectionLogo(String connectionId) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    String logoString = sharedPreferences
        .getString(connectionId.concat(CONNECTION_LOGO_KEY), "");

    return Base64.decode(logoString, Base64.DEFAULT);
  }

  public void addConnection(AddConnectionFromTokenResponse connectionInfo) {
    this.saveUserId(connectionInfo.getUserId());
    this.saveConnectionWithUser(
        connectionInfo.getConnection().getConnectionId(),
        connectionInfo.getUserId());
    this.saveConnectionWithLogo(
        connectionInfo.getConnection().getConnectionId(),
        connectionInfo.getConnection().getParticipantInfo().getIssuer().getLogo());
  }
}

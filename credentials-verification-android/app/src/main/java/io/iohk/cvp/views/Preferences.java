package io.iohk.cvp.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;

import com.google.protobuf.ByteString;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.core.exception.WrongPinLengthException;
import io.iohk.cvp.data.local.preferences.SecurityPin;
import io.iohk.cvp.utils.KeyStoreUtils;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;

import static android.content.Context.MODE_PRIVATE;

public class Preferences {

    public static final String ACCEPTED_MESSAGES_KEY = "accepted_messages";
    public static final String REJECTED_MESSAGES_KEY = "rejected_messages";
    public static final String USER_PROFILE_NAME = "user_profile_name";
    public static final String USER_PROFILE_COUNTRY = "user_profile_country";
    public static final String USER_PROFILE_BIRTH_DATE = "user_profile_birth_date";
    public static final String USER_PROFILE_EMAIL = "user_profile_email";
    public static final String CONNECTION_TOKEN_TO_ACCEPT = "connection_token_to_accept";
    public static final String BACKEND_IP = "backend_ip";
    public static final String BACKEND_PORT = "backend_port";
    private static final String MY_PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION";
    private static final String PK_KEY = "wallet_pk";
    private static final String USER_ID_LIST_KEY = "user_id";
    private static final String CONNECTION_USER_ID_KEY = "connection_id_user_id";
    private static final String CONNECTION_LOGO_KEY = "connection_logo";

    public static final String PROOF_REQUEST_SHARED_KEY = "proof_request_shared";
    public static final String PROOF_REQUEST_CANCEL_KEY = "proof_request_cancel";

    public static final String SECURITY_PIN = "security_pin";
    public static final String SECURITY_TOUCH_ENABLED = "security_touch_enabled";

    // This key get used always that app start, this help to detect when app is starting and avoid to call UnlockActivity before the first activity is launched.
    public static final String FIRST_LAUNCH = "first_launch";

    final private Context context;

    public Preferences(Context context){
        this.context = context;
    }

    public void savePrivateKey(byte[] pk) {
        KeyStoreUtils keyStoreUtils = new KeyStoreUtils();
        keyStoreUtils.generateKey();
        String encPk = keyStoreUtils.encryptData(pk);

        Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
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

    public boolean isPrivateKeyStored() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Optional<String> pk = Optional.ofNullable(sharedPreferences.getString(PK_KEY, null));
        return pk.isPresent();
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

    public boolean hasUserIdsStored() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return !sharedPreferences.getStringSet(USER_ID_LIST_KEY, new HashSet<>()).isEmpty();
    }

    public void saveSecurityPin(SecurityPin pin) {
        editString(pin.getPinString(), SECURITY_PIN);
    }

    public SecurityPin getSecurityPin() throws WrongPinLengthException {
       return new SecurityPin(getString(SECURITY_PIN));
    }

    public boolean isPinConfigured() {
        return StringUtils.isNotBlank(getString(SECURITY_PIN));
    }

    public void saveSecurityTouch(Boolean enable) {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putBoolean(SECURITY_TOUCH_ENABLED, enable);
        editor.apply();
    }

    public Boolean getSecurityTouch() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(SECURITY_TOUCH_ENABLED, false);
    }

    private void editString(String valueToAdd, String prefKey) {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(prefKey, valueToAdd);
        editor.apply();
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

    public void saveUserProfile(String name, String country, String birthDate, String email) {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(USER_PROFILE_NAME, name);
        editor.putString(USER_PROFILE_COUNTRY, country);
        editor.putString(USER_PROFILE_BIRTH_DATE, birthDate);
        editor.putString(USER_PROFILE_EMAIL, email);
        editor.apply();
    }

    public String getString(String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences
                .getString(key, "");
    }

    public void saveConnectionTokenToAccept(String token) {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(CONNECTION_TOKEN_TO_ACCEPT, token);
        editor.apply();
    }

    public void saveBackendData(String ip, String port) {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(BACKEND_IP, ip);

        int portValue = port.equals("") ? 0 : Integer.valueOf(port);
        editor.putInt(BACKEND_PORT, portValue);
        editor.apply();
    }

    public Integer getInt(String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences
                .getInt(key, 0);
    }

    public void setIsFirstLaunch(boolean firstLaunch) {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putBoolean(FIRST_LAUNCH, firstLaunch);
        editor.apply();
    }


    public Boolean isFirstLaunch() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(FIRST_LAUNCH, true);
    }

    public void deleteUserConnections(List<String> connectionIdList) {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Editor editor = prefs.edit();
        connectionIdList.forEach(s -> {
            if(prefs.contains(s.concat(CONNECTION_USER_ID_KEY)))
                editor.remove(s.concat(CONNECTION_USER_ID_KEY));
            if(prefs.contains(s.concat(CONNECTION_LOGO_KEY)))
                editor.remove(s.concat(CONNECTION_LOGO_KEY));
        });
        List<String> keyToRemove = Arrays.asList(USER_ID_LIST_KEY, CONNECTION_TOKEN_TO_ACCEPT, PROOF_REQUEST_SHARED_KEY, PROOF_REQUEST_CANCEL_KEY, ACCEPTED_MESSAGES_KEY, REJECTED_MESSAGES_KEY);
        keyToRemove.forEach(editor::remove);
        editor.commit();
    }
}
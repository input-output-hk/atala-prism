package io.iohk.cvp.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.iohk.atala.crypto.japi.ECKeyPair;
import io.iohk.cvp.core.exception.WrongPinLengthException;
import io.iohk.cvp.data.local.preferences.SecurityPin;
import io.iohk.cvp.utils.CryptoUtils;

import static android.content.Context.MODE_PRIVATE;

public class Preferences {

    public static final String USER_PROFILE_NAME = "user_profile_name";
    public static final String USER_PROFILE_COUNTRY = "user_profile_country";
    public static final String USER_PROFILE_BIRTH_DATE = "user_profile_birth_date";
    public static final String USER_PROFILE_EMAIL = "user_profile_email";
    public static final String CONNECTION_TOKEN_TO_ACCEPT = "connection_token_to_accept";
    public static final String BACKEND_IP = "backend_ip";
    public static final String BACKEND_PORT = "backend_port";
    private static final String MY_PREFS_NAME = "IOHK.ATALA.CREDENTIAL.VERIFICATION";

    public static final String SECURITY_PIN = "security_pin";
    public static final String SECURITY_TOUCH_ENABLED = "security_touch_enabled";

    // This key get used always that app start, this help to detect when app is starting and avoid to call UnlockActivity before the first activity is launched.
    public static final String FIRST_LAUNCH = "first_launch";

    private static final String CURRENT_VALUE_INDEX = "current_value_index";
    private static final String MNEMONIC_LIST = "mnemonic_list";

    final private Context context;
    private String DELIMETED_CHARACTER = ",";


    public Preferences(Context context){
        this.context = context;
    }

    public boolean isPrivateKeyStored() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Optional<String> pk = Optional.ofNullable(sharedPreferences.getString(MNEMONIC_LIST, null));
        return pk.isPresent();
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

    public int getCurrentIndex() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(CURRENT_VALUE_INDEX, -1);
    }

    @NotNull
    public List<String> getMnemonicList() {
        return getOrderedCollection(MNEMONIC_LIST);
    }

    public void saveMnemonicList(List<String> mnemonicList) {
        saveOrderedArray(mnemonicList, MNEMONIC_LIST);
    }

    private void saveOrderedArray(List<String> list, String key) {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String listAsString = list.stream()
                .collect(Collectors.joining(DELIMETED_CHARACTER));
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, listAsString);
        editor.apply();
    }

    public List<String> getOrderedCollection(String key){
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String listAsString = prefs.getString(key, "");
        String[] myNewList = listAsString.split(DELIMETED_CHARACTER);

        return Arrays.asList(myNewList);
    }

    public void increaseIndex() {
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        int currentIndex = prefs.getInt(CURRENT_VALUE_INDEX, -1);
        Editor editor = prefs.edit();
        editor.putInt(CURRENT_VALUE_INDEX, ++currentIndex);
        editor.apply();
    }

    public ECKeyPair getKeyPairFromPath(String keyDerivationPath) {
        return CryptoUtils.Companion.getKeyPairFromPath(keyDerivationPath, getMnemonicList());
    }
}
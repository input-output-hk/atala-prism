package io.iohk.atala.prism.app.utils;

public class FirebaseAnalyticsEvents {

    public static final String CREATE_ACCOUNT = "create_account";
    public static final String CONTINUE_AFTER_TC_PP = "user_continue_post_accept";
    public static final String ACCEPT_RECOVERY_PHRASE_CONTINUE = "user_accept_recovery_phrase_continue";
    public static final String VERIFY_RECOVERY_PHRASE_SUCCESS = "verify_recovery_phrase_success";
    public static final String VERIFY_RECOVERY_PHRASE_FAIL = "verify_recovery_phrase_fail";

    public static final String SECURE_APP_FINGERPRINT = "secure_app_fingerprint";
    public static final String SECURE_APP_FINGERPRINT_PASSCODE = "secure_app_fingerprint_passcode";

    public static final String NEW_CREDENTIAL_VIEW = "new_credential_view";

    public static final String NEW_CONNECTION_CONFIRM = "new_connection_confirm";
    public static final String NEW_CONNECTION_DECLINE = "new_connection_decline";

    public static final String RESET_DATA = "reset_data";
    public static final String SUPPORT = "support";
}

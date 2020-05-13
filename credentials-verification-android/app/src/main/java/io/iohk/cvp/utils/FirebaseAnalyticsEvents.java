package io.iohk.cvp.utils;

public class FirebaseAnalyticsEvents {

    public static final String CREATE_ACCOUNT = "create_account";

    public static final String ACCEPT_TCS = "user_accept_tcs";
    public static final String ACCEPT_PP = "user_accept_pp";
    public static final String CONTINUE_AFTER_TC_PP = "user_continue_post_accept";

    public static final String ACCEPT_RECOVERY_PHRASE = "user_accept_recovery_phrase";
    public static final String ACCEPT_RECOVERY_PHRASE_CONTINUE = "user_accept_recovery_phrase_continue";

    public static final String VERIFY_RECOVERY_PHRASE_SUCCESS = "verify_recovery_phrase_success";
    public static final String VERIFY_RECOVERY_PHRASE_FIAL = "verify_recovery_phrase_fail";

    public static final String NEW_CREDENTIAL_CONFIRM = "new_credential_confirm";
    public static final String NEW_CREDENTIAL_DECLINE = "new_credential_decline";

    public static final String NEW_CONNECTION_CONFIRM = "new_connection_confirm";
    public static final String NEW_CONNECTION_DECLINE = "new_connection_decline";


}

package io.iohk.atala.prism.app.ui.main.settings;

import android.content.Context;
import android.content.Intent;

import io.iohk.atala.prism.app.utils.Constants;

/*
 * @TODO this needs to be removed when the navigation components are implemented
 * */
public class TermsAndConditionHelper {
    private static void showWebContent(String url, Context ctx) {
        Intent intent = new Intent(ctx.getApplicationContext(), WebTermsAndConditionsActivity.class);
        intent.putExtra(WebTermsAndConditionsActivity.WEB_VIEW_URL, url);
        ctx.startActivity(intent);
    }

    public static void showTermsAndConditions(Context context) {
        showWebContent(Constants.LEGAL_BASE_URL.concat(Constants.LEGAL_TERMS_AND_CONDITIONS), context);
    }

    public static void showPrivacyPolicy(Context context) {
        showWebContent(Constants.LEGAL_BASE_URL.concat(Constants.LEGAL_PRIVACY_POLICY), context);
    }
}

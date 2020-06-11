package io.iohk.cvp.views;

import static io.iohk.cvp.utils.ActivitiesRequestCodes.BRAINTREE_REQUEST_ACTIVITY;
import static io.iohk.cvp.views.activities.SeedPhraseVerificationActivity.FIRST_WORD_INDEX_KEY;
import static io.iohk.cvp.views.activities.SeedPhraseVerificationActivity.SECOND_WORD_INDEX_KEY;
import static io.iohk.cvp.views.activities.SeedPhraseVerificationActivity.SEED_PHRASE_KEY;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.braintreepayments.api.dropin.DropInRequest;

import io.iohk.cvp.R;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.views.activities.AccountCreatedActivity;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.activities.QrCodeScanner;
import io.iohk.cvp.views.activities.SeedPhraseVerificationActivity;
import io.iohk.cvp.views.activities.TermsAndConditionsActivity;
import io.iohk.cvp.views.activities.UnlockActivity;
import io.iohk.cvp.views.activities.WalletSetupActivity;
import io.iohk.cvp.views.activities.WebViewActivity;
import io.iohk.cvp.views.activities.WelcomeActivity;
import io.iohk.cvp.views.fragments.CvpDialogFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.fragments.PopUpFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Navigator {

    //Activities
    public void showWelcomeActivity(Activity from) {
        startNewActivity(from, WelcomeActivity.class, null);
    }

    public void showConnections(Activity from, List<Integer> flags) {
        startNewActivity(from, MainActivity.class, flags);
    }

    public void showTermsAndConditions(Activity from) {
        startNewActivity(from, TermsAndConditionsActivity.class, null);
    }

    public void showWalletSetup(Activity from) {
        startNewActivity(from, WalletSetupActivity.class, null);
    }

    public void showSeedPhraseVerification(Activity from, List<String> seedPhrase,
                                           Integer firstWordIndexToCheck, Integer secondWordIndexToCheck) {
        Intent intent = getIntent(from, SeedPhraseVerificationActivity.class, null);
        Bundle bundle = new Bundle();
        bundle.putStringArray(SEED_PHRASE_KEY, Arrays.copyOf(seedPhrase.toArray(),
                seedPhrase.size(),
                String[].class));
        bundle.putInt(FIRST_WORD_INDEX_KEY, firstWordIndexToCheck);
        bundle.putInt(SECOND_WORD_INDEX_KEY, secondWordIndexToCheck);
        intent.putExtras(bundle);
        from.startActivity(intent);
    }

    public void showAccountCreated(Activity from) {
        List<Integer> flags = new ArrayList<>();
        flags.add(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        flags.add(Intent.FLAG_ACTIVITY_NEW_TASK);
        startNewActivity(from, AccountCreatedActivity.class, flags);
    }

    public void showWebView(Activity from) {
        startNewActivity(from, WebViewActivity.class, null);
    }

    public void showQrScanner(CvpFragment from) {
        Intent intent = new Intent(Objects.requireNonNull(from.getActivity()).getApplicationContext(),
                QrCodeScanner.class);
        intent.putExtra(IntentDataConstants.QR_SCANNER_MODE_KEY,
                IntentDataConstants.QR_SCANNER_MODE_QR_CODE);
        from.startActivityForResult(intent, ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY);
    }

    public void showPayment(Activity from, String tokenizationKey) {
        DropInRequest dropInRequest = new DropInRequest().clientToken(tokenizationKey);
        Intent intent = dropInRequest.getIntent(from);
        from.startActivityForResult(intent, BRAINTREE_REQUEST_ACTIVITY);
    }

    private void startNewActivity(Activity from, Class activityClass,
                                  List<Integer> flags) {
        Intent intent = getIntent(from, activityClass, flags);
        from.startActivity(intent);
    }

    private Intent getIntent(Activity from, Class activityClass, List<Integer> flags) {
        Intent intent = new Intent(from.getApplicationContext(), activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (flags != null) {
            flags.forEach(intent::addFlags);
        }

        return intent;
    }

    public void showPopUp(FragmentManager fragmentManager, String errorMessage) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(IntentDataConstants.POP_UP_IS_ERROR, errorMessage != null);
        bundle.putString(IntentDataConstants.ERROR_MSG_DESCRIPTION_KEY, errorMessage);

        PopUpFragment fragment = new PopUpFragment();
        fragment.setArguments(bundle);

        String tag = "ERROR_POP_UP";

        if (fragmentManager.findFragmentByTag(tag) == null) {
            fragmentManager
                    .beginTransaction().add(fragment, tag)
                    .show(fragment).commit();
        }
    }

    public void showAppPermissionSettings(Activity from) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + from.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        from.startActivity(intent);
    }

    public void showFragment(FragmentManager supportFragmentManager, CvpFragment cvpFragment, String tag) {
        FragmentTransaction ft = supportFragmentManager.beginTransaction();
        ft.replace(R.id.fragment_layout, cvpFragment, tag);
        ft.addToBackStack(tag);
        ft.commit();

    }

    public void showFragmentOnTop(FragmentManager supportFragmentManager, CvpFragment cvpFragment) {
       showFragment(supportFragmentManager, cvpFragment, null);
    }

    public void showFragmentOnTopOfMenu(FragmentManager supportFragmentManager,
                                        CvpFragment cvpFragment) {
        FragmentTransaction ft = supportFragmentManager.beginTransaction();
        ft.replace(R.id.fragment_layout_over_menu, cvpFragment);
        ft.addToBackStack(null);
        ft.commit();

    }

    public void showDialogFragment(FragmentManager supportFragmentManager,
                                   CvpDialogFragment cvpFragment, String tag) {

        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager
                    .beginTransaction().add(cvpFragment, tag)
                    .show(cvpFragment)
                    .commit();
        }
    }

    public void showUnlockScreen(Context ctx) {
        Intent myIntent = new Intent(ctx, UnlockActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(myIntent);
    }
}

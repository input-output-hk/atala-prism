package io.iohk.cvp.views;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.views.activities.AccountCreatedActivity;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.activities.QrCodeScanner;
import io.iohk.cvp.views.activities.SeedPhraseVerificationActivity;
import io.iohk.cvp.views.activities.TermsAndConditionsActivity;
import io.iohk.cvp.views.activities.WalletSetupActivity;
import io.iohk.cvp.views.activities.WebViewActivity;
import io.iohk.cvp.views.activities.WelcomeActivity;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.PopUpFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Navigator {

  //Activities
  public void showWelcomeActivity(Activity from) {
    startNewActivity(from, WelcomeActivity.class, null);
  }

  public void showConnections(Activity from) {
    startNewActivity(from, MainActivity.class, null);
  }

  public void showTermsAndConditions(Activity from) {
    startNewActivity(from, TermsAndConditionsActivity.class, null);
  }

  public void showWalletSetup(Activity from) {
    startNewActivity(from, WalletSetupActivity.class, null);
  }

  public void showSeedPhraseVerification(Activity from) {
    startNewActivity(from, SeedPhraseVerificationActivity.class, null);
  }

  public void showAccountCreated(Activity from) {
    startNewActivity(from, AccountCreatedActivity.class, new ArrayList<>(FLAG_ACTIVITY_CLEAR_TASK));
  }

  public void showWebView(Activity from) {
    startNewActivity(from, WebViewActivity.class, null);
  }

  public void showQrScanner(FirstConnectionFragment from) {
    Intent intent = new Intent(Objects.requireNonNull(from.getActivity()).getApplicationContext(),
        QrCodeScanner.class);
    intent.putExtra(IntentDataConstants.QR_SCANNER_MODE_KEY,
        IntentDataConstants.QR_SCANNER_MODE_QR_CODE);
    from.startActivityForResult(intent, ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY);
  }

  private void startNewActivity(Activity from, Class activityClass,
      List<Integer> flags) {
    Intent intent = new Intent(from.getApplicationContext(), activityClass);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

    if (flags != null) {
      flags.forEach(intent::addFlags);
    }

    from.startActivity(intent);
  }

  public void showPermissionDeniedPopUp(FragmentManager fragmentManager) {
    Bundle bundle = new Bundle();
    PopUpFragment fragment = new PopUpFragment();
    fragment.setArguments(bundle);
    fragmentManager
        .beginTransaction().add(fragment, "PERMISSION_DENIED_POPUP")
        .show(fragment).commit();
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

  public void showFragment(FragmentManager supportFragmentManager, CvpFragment cvpFragment) {
    FragmentTransaction ft = supportFragmentManager.beginTransaction();
    ft.replace(R.id.fragment_layout, cvpFragment);
    ft.commit();
  }

  public void showFragmentOnTop(FragmentManager supportFragmentManager, CvpFragment cvpFragment) {
    FragmentTransaction ft = supportFragmentManager.beginTransaction();
    ft.replace(R.id.fragment_layout, cvpFragment);
    ft.addToBackStack(null);
    ft.commit();
  }

  public void showFragmentOnTopOfMenu(FragmentManager supportFragmentManager,
      CvpFragment cvpFragment) {
    FragmentTransaction ft = supportFragmentManager.beginTransaction();
    // FIXME missing layout
    //ft.replace(R.id.fragment_layout_over_menu, cvpFragment);
    ft.addToBackStack(null);
    ft.commit();
  }
}

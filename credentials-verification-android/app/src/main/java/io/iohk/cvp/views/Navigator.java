package io.iohk.cvp.views;

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

public class Navigator {

  //Activities
  public void showWellcomeActivity(Activity from) {
    Intent intent = new Intent(from.getApplicationContext(), WelcomeActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    from.startActivity(intent);
  }

  public void showConnections(Activity from) {
    Intent intent = new Intent(from.getApplicationContext(), MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    from.startActivity(intent);
  }

  public void showTermsAndConditions(Activity from) {
    Intent intent = new Intent(from.getApplicationContext(), TermsAndConditionsActivity.class);
    from.startActivity(intent);
  }

  public void showWalletSetup(Activity from) {
    Intent intent = new Intent(from.getApplicationContext(), WalletSetupActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    from.startActivity(intent);
  }

  public void showSeedPhraseVerification(Activity from) {
    Intent intent = new Intent(from.getApplicationContext(), SeedPhraseVerificationActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    from.startActivity(intent);
  }

  public void showAccountCreated(Activity from) {
    Intent intent = new Intent(from.getApplicationContext(), AccountCreatedActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    from.startActivity(intent);
  }

  public void showWebView(Activity from) {
    Intent intent = new Intent(from.getApplicationContext(), WebViewActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    from.startActivity(intent);
  }

  public void showQrScanner(FirstConnectionFragment from) {
    Intent intent = new Intent(from.getActivity().getApplicationContext(), QrCodeScanner.class);
    intent.putExtra(IntentDataConstants.QR_SCANNER_MODE_KEY,
        IntentDataConstants.QR_SCANNER_MODE_QR_CODE);
    from.startActivityForResult(intent, ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY);
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
}

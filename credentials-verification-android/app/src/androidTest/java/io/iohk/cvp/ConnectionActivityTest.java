package io.iohk.cvp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import io.iohk.cvp.utils.ActivitiesRequestCodes;
import io.iohk.cvp.utils.IntentDataConstants;
import io.iohk.cvp.views.activities.QrCodeScanner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConnectionActivityTest {


  @Rule
  public IntentsTestRule<ConnectionActivity> mActivityRule = new IntentsTestRule<>(
      ConnectionActivity.class);

  @Test
  public void scanQrResultIsSuccessful() {
    // This mocks the qr code scan result
    stubCameraIntent();

    // Checking that the button is visible, this means that te code scan was successful (for now).
    // TODO refactor this when navigation to connection info is developed
    onView(withId(R.id.scan_qr))
        .check(matches(withText(R.string.scan_qr)));
  }

  private void stubCameraIntent() {
    ActivityResult result = createQrScannerActivityResultStub();
    intending(hasComponent(QrCodeScanner.class.getName())).respondWith(result);
    mActivityRule.getActivity()
        .startActivityForResult(
            new Intent(mActivityRule.getActivity().getApplicationContext(), QrCodeScanner.class),
            ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY);
  }

  private ActivityResult createQrScannerActivityResultStub() {
    Bundle bundle = new Bundle();
    bundle.putString(IntentDataConstants.QR_RESULT, "1234");

    // Create the Intent that will include the bundle.
    Intent resultData = new Intent();
    resultData.putExtras(bundle);

    // Create the ActivityResult with the Intent.
    return new ActivityResult(Activity.RESULT_OK, resultData);
  }
}

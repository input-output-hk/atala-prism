package io.iohk.cvp.views.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.Result;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.IntentDataConstants;
import java.util.Objects;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrCodeScanner extends AppCompatActivity implements ZXingScannerView.ResultHandler {

  private ZXingScannerView mScannerView;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    mScannerView = new ZXingScannerView(this);
    setContentView(mScannerView);
    Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.qr_scanner_activity_title);
  }

  @Override
  public void onResume() {
    super.onResume();
    mScannerView.setResultHandler(this);
    mScannerView.startCamera();
  }

  @Override
  public void onPause() {
    super.onPause();
    mScannerView.stopCamera();
  }

  @Override
  public void handleResult(Result rawResult) {
    Intent intent = new Intent();
    intent.putExtra(IntentDataConstants.QR_RESULT, rawResult.getText());
    setResult(RESULT_OK, intent);
    finish();
  }
}
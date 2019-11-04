package io.iohk.cvp.views.activities;

import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import io.iohk.cvp.R;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import java.util.Optional;

public class WebViewActivity extends AppCompatActivity {

  WebView webView;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.web_view_activity);

    webView = findViewById(R.id.web_view);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebViewClient(new WebViewClient());
    webView.loadUrl("https://www.zendesk.com/");

  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    // Check if the key event was the Back button and if there's history
    if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
      webView.goBack();
      return true;
    }
    // If it wasn't the Back key or there's no web page history, bubble up to the default
    // system behavior (probably exit the activity)
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onResume() {
    super.onResume();
    Optional<ActionBar> supportActionBar = Optional.ofNullable(getSupportActionBar());
    supportActionBar.ifPresent(
        actionBar -> new RootAppBar(R.string.support_title).configureActionBar(actionBar));
  }

}

package io.iohk.cvp.views.activities;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Calendar;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.LargeDescriptionDialogFragment;
import io.iohk.cvp.views.utils.components.CheckboxWithDescription;

public class WebTermsAndConditionsActivity extends Activity {

  public static String WEB_VIEW_URL = "webViewUrl";

  @Inject
  Navigator navigator;

  @BindView(R.id.web_view)
  public WebView webView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_web_terms_cond);
    ButterKnife.bind(this);

    getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebViewClient(new WebViewClient());
    webView.loadUrl(getIntent().getExtras().getString(WEB_VIEW_URL));

  }

  @OnClick(R.id.fab)
  public void onContinueClick() {
    finish();
  }

}
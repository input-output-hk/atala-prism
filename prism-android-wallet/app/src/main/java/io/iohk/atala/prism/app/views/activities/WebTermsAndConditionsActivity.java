package io.iohk.atala.prism.app.views.activities;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.views.Navigator;

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
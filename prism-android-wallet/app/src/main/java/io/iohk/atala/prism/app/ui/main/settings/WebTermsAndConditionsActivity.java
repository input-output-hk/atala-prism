package io.iohk.atala.prism.app.ui.main.settings;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.iohk.cvp.R;

/*
 * TODO this needs to be removed to use WebViewDialogFragment
 * */
public class WebTermsAndConditionsActivity extends Activity {

    public static String WEB_VIEW_URL = "webViewUrl";

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
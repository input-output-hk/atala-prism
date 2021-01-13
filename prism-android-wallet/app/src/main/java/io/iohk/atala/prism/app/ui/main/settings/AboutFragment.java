package io.iohk.atala.prism.app.ui.main.settings;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.atala.prism.app.ui.CvpFragment;
import io.iohk.cvp.BuildConfig;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.ui.Navigator;
import io.iohk.atala.prism.app.ui.utils.AppBarConfigurator;
import io.iohk.atala.prism.app.ui.utils.NoAppBar;
import lombok.Setter;

@Setter
public class AboutFragment extends CvpFragment {

    @BindString(R.string.terms_and_conditions_activity_title)
    public String termsAndConditionsTitle;

    @BindString(R.string.privacy_policies_agreement)
    public String policiesTitle;

    @Inject
    public AboutFragment() {
    }

    @Inject
    Navigator navigator;

    @BindView(R.id.labelBuilt)
    TextView labelBuilt;

    @BindView(R.id.versionLabel)
    TextView versionLabel;

    @Override
    protected int getViewId() {
        return R.layout.fragment_about;
    }

    @Override
    public ViewModel getViewModel() {
        return null;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new NoAppBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        String text = getResources().getString(R.string.about_built);
        SpannableString ss = new SpannableString(text);
        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        StyleSpan boldSpan1 = new StyleSpan(Typeface.BOLD);
        ss.setSpan(boldSpan, 9, 13, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        ss.setSpan(boldSpan1, 25, 30, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        labelBuilt.setText(ss);
        if (BuildConfig.DEBUG) {
            versionLabel.setText(BuildConfig.VERSION_NAME + " - Testing");
        } else {
            versionLabel.setText(BuildConfig.VERSION_NAME);
        }

        return view;
    }

    @OnClick(R.id.termsLabel)
    void onTermsClick() {
        TermsAndConditionHelper.showTermsAndConditions(getContext());
    }

    @OnClick(R.id.policyLabel)
    void onPolicyClick() {
        TermsAndConditionHelper.showPrivacyPolicy(getContext());
    }


}

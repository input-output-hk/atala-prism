package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.NoAppBar;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;

import java.util.Objects;

import javax.inject.Inject;

public class PaymentCongratsFragment extends CvpFragment {

    @Inject
    Navigator navigator;

    @BindView(R.id.button)
    public Button continueButton;

    @BindView(R.id.text_view_title)
    public TextView textViewTitle;

    @BindView(R.id.text_view_description)
    public TextView textViewDescription;

    @Override
    protected int getViewId() {
        return R.layout.layout_congratulations;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        continueButton.setText(R.string.continue_string);
        textViewTitle.setText(R.string.credential_payment_successful);
        textViewDescription.setVisibility(View.GONE);
        return v;
    }

    @Override
    public ViewModel getViewModel() {
        return null;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new NoAppBar();
    }

    @OnClick(R.id.button)
    public void onContinueClick() {
        ((MainActivity) requireActivity())
                .onNavigation(BottomAppBarOption.HOME);
    }
}
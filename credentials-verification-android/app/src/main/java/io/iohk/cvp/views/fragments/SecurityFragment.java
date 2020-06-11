package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.NoAppBar;
import lombok.Setter;

@Setter
public class SecurityFragment extends CvpFragment {

    @Inject
    public SecurityFragment() {
    }

    @Inject
    Navigator navigator;

    @BindView(R.id.switchTouch)
    Switch switchTouch;

    @BindView(R.id.changePin)
    TextView changePin;

    @Override
    protected int getViewId() {
        return R.layout.fragment_security;
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

        if(new Preferences(getContext()).getSecurityTouch()){
            switchTouch.setChecked(true);
        }
        switchTouch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Preferences(getContext()).saveSecurityTouch(isChecked);
            }
        });

        changePin.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 navigator.showFragmentOnTopOfMenu(getFragmentManager(), new SecurityChangePinFragment());
             }
         });

        return view;
    }
}

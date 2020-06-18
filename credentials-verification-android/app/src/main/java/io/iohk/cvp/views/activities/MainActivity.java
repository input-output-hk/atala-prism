package io.iohk.cvp.views.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.CaseNotFoundException;
import io.iohk.cvp.core.exception.CryptoException;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.utils.CryptoUtils;
import io.iohk.cvp.viewmodel.MainViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.ContactsFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.fragments.MyCredentials;
import io.iohk.cvp.views.fragments.ProfileFragment;
import io.iohk.cvp.views.fragments.SettingsFragment;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBar;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarListener;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;
import io.iohk.prism.protos.ConnectionInfo;
import lombok.Getter;

import static io.iohk.cvp.utils.ActivitiesRequestCodes.BRAINTREE_REQUEST_ACTIVITY;
import static io.iohk.cvp.views.Preferences.CONNECTION_TOKEN_TO_ACCEPT;

public class MainActivity extends CvpActivity<MainViewModel> implements BottomAppBarListener {

    public static final String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT";

    @Inject
    HomeFragment homeFragment;

    @Inject
    MyCredentials myCredentialsFragment;

    @Inject
    SettingsFragment settingsFragment;

    @Inject
    ContactsFragment contactsFragment;

    @Inject
    ProfileFragment profileFragment;

    @Inject
    @Getter
    Navigator navigator;

    @BindView(R.id.fab)
    FloatingActionButton fab;

    @BindView(R.id.bottom_appbar)
    public BottomAppBar bottomAppBar;

    @BindView(R.id.fragment_layout)
    public FrameLayout frameLayout;

    @BindView(R.id.fragment_layout_over_menu)
    public FrameLayout frameLayoutOverMenu;

    @BindColor(R.color.colorPrimary)
    ColorStateList colorRed;

    @BindColor(R.color.black)
    ColorStateList colorBlack;

    MenuItem paymentHistoryMenuItem;

    @Inject
    ViewModelProvider.Factory factory;

    private FirebaseAnalytics mFirebaseAnalytics;

    private List<ConnectionInfo> issuerConnections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        super.onCreate(savedInstanceState);

        bottomAppBar.setListener(this);
        fab.setBackgroundTintList(colorRed);
        issuerConnections = new ArrayList<>();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (new Preferences(getApplicationContext()).hasUserIdsStored()) {
            ft.replace(R.id.fragment_layout, homeFragment, MAIN_FRAGMENT_TAG);
        } else {
            FirstConnectionFragment f = FirstConnectionFragment.newInstance(R.string.notifications, issuerConnections);
            ft.replace(R.id.fragment_layout, f, MAIN_FRAGMENT_TAG);
        }
        ft.commit();

        Preferences prefs = new Preferences(this);
        if(prefs.isPinConfigured())
            navigator.showUnlockScreen(this);

    }



    @Override
    public MainViewModel getViewModel() {
        MainViewModel viewModel = ViewModelProviders.of(this, factory).get(MainViewModel.class);
        viewModel.setContext(getApplicationContext());
        return viewModel;
    }

    protected int getView() {
        return R.layout.activity_main;
    }

    protected int getTitleValue() {
        return R.string.connections_activity_title;
    }

    @Override
    public void onNavigation(BottomAppBarOption option, String userId) {
        if (BottomAppBarOption.HOME.equals(option) || BottomAppBarOption.FIRSTCONNECTION.equals(option)) {
            fab.setBackgroundTintList(colorRed);
        } else {
            fab.setBackgroundTintList(colorBlack);
        }
        bottomAppBar.setItemColors(option);

        getFragmentToRender(option)
                .ifPresent(cvpFragment -> {
                    Fragment currentFragment = this.getSupportFragmentManager()
                            .findFragmentByTag(MAIN_FRAGMENT_TAG);

                    if (currentFragment instanceof ContactsFragment && cvpFragment instanceof ContactsFragment) {
                        Set<String> userIds = new HashSet<>();
                        userIds.add(userId);
                        ((ContactsFragment) currentFragment).listConnections(userIds);
                    } else {
                        navigator.showFragment(getSupportFragmentManager(), cvpFragment, MAIN_FRAGMENT_TAG);
                    }
                });
    }

    private Optional<CvpFragment> getFragmentToRender(BottomAppBarOption option) {
        switch (option) {
            case CONTACTS:
                if (new Preferences(getApplicationContext()).hasUserIdsStored()) {
                    return Optional.of(contactsFragment);
                }

                FirstConnectionFragment f = FirstConnectionFragment.newInstance(R.string.contacts, issuerConnections);
                return Optional.of(f);
            case CREDENTIAL:
                return Optional.of(myCredentialsFragment);
            case HOME:
                return Optional.of(homeFragment);
            case SETTINGS:
                return Optional.of(settingsFragment);
            case PROFILE:
                return Optional.of(profileFragment);
            case FIRSTCONNECTION:
                FirstConnectionFragment fragment = FirstConnectionFragment.newInstance(R.string.notifications, issuerConnections);
                return Optional.of(fragment);
            default:
                Crashlytics.logException(
                        new CaseNotFoundException("Couldn't find fragment for option " + option,
                                ErrorCode.STEP_NOT_FOUND));
                return Optional.empty();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(this.getTitleValue());
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        paymentHistoryMenuItem = menu.findItem(R.id.action_payment_history);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BRAINTREE_REQUEST_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();
                String strNonce = nonce.getNonce();

                Preferences prefs = new Preferences(this);
                // TODO send real amount
                sendPayments(new BigDecimal(150.1, MathContext.DECIMAL32), strNonce,
                        prefs.getString(CONNECTION_TOKEN_TO_ACCEPT),
                        prefs);
            } else {
                if (resultCode != RESULT_CANCELED) {
                    Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                    Crashlytics.logException(error);
                    // TODO show error message
                }
            }
        }
    }

    @OnClick(R.id.fab)
    public void onFabClick() {
        onNavigation(BottomAppBarOption.HOME, null);
    }

    private void sendPayments(BigDecimal amount, String nonce, String connectionToken,
                              Preferences prefs) {
        try {
            viewModel
                    .addConnectionFromToken(connectionToken, CryptoUtils.getPublicKey(prefs), "")
                    .observe(this, response -> {
                        if (response.getError() != null) {
                            getNavigator().showPopUp(getSupportFragmentManager(), getResources().getString(
                                    R.string.server_error_message));
                            return;
                        }
                        AddConnectionFromTokenResponse info = response.getResult();
                        prefs.addConnection(info);
                        onNavigation(BottomAppBarOption.CONTACTS, info.getUserId());
                        bottomAppBar.setItemColors(BottomAppBarOption.CONTACTS);
                    });
        } catch (SharedPrefencesDataNotFoundException | InvalidKeySpecException | CryptoException e) {
            Crashlytics.logException(e);
            // TODO show error message
        }
    }

    public void acceptConnection(String connectionToken, Preferences prefs) {
        try {
            viewModel
                    .addConnectionFromToken(connectionToken, CryptoUtils.getPublicKey(prefs), "")
                    .observe(this, response -> {
                        if (response.getError() != null) {
                            getNavigator().showPopUp(getSupportFragmentManager(), getResources().getString(
                                    R.string.server_error_message));
                            return;
                        }
                        AddConnectionFromTokenResponse info = response.getResult();
                        prefs.addConnection(info);
                        onNavigation(BottomAppBarOption.CONTACTS, info.getUserId());
                        bottomAppBar.setItemColors(BottomAppBarOption.CONTACTS);
                    });

        } catch (SharedPrefencesDataNotFoundException | InvalidKeySpecException | CryptoException e) {
            Crashlytics.logException(e);
            // TODO show error message
        }
    }

    public void sentFirebaseAnalyticsEvent(String eventName){
        mFirebaseAnalytics.logEvent(eventName, null);
    }

    public void setIssuerConnections(List<ConnectionInfo> issuerConnections) {
        this.issuerConnections.addAll(issuerConnections.stream()
                .filter(conn -> !this.issuerConnections.contains(conn)).collect(Collectors.toList()));
    }

    public List<ConnectionInfo> getIssuerConnections() {
        return this.issuerConnections;
    }

    @Override
    protected void onStart() {
        super.onStart();

    }


}
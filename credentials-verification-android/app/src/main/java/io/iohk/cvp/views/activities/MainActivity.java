package io.iohk.cvp.views.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.atala.crypto.DerivationPath;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.CaseNotFoundException;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.viewmodel.MainViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.ContactsFragment;
import io.iohk.cvp.views.fragments.CvpFragment;
import io.iohk.cvp.views.fragments.FirstConnectionFragment;
import io.iohk.cvp.views.fragments.HomeFragment;
import io.iohk.cvp.views.fragments.MyCredentialsFragment;
import io.iohk.cvp.views.fragments.ProfileFragment;
import io.iohk.cvp.views.fragments.SettingsFragment;
import io.iohk.cvp.views.interfaces.ConnectionManageable;
import io.iohk.cvp.views.interfaces.FirebaseEventLogger;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBar;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarListener;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import io.iohk.prism.protos.AddConnectionFromTokenResponse;
import lombok.Getter;

import static io.iohk.cvp.utils.ActivitiesRequestCodes.BRAINTREE_REQUEST_ACTIVITY;
import static io.iohk.cvp.views.Preferences.CONNECTION_TOKEN_TO_ACCEPT;
import static io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption.CONTACTS;

public class MainActivity extends CvpActivity<MainViewModel> implements BottomAppBarListener, FirebaseEventLogger, ConnectionManageable {

    public static final String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT";

    private static final String INITIAL_TRANSACTION = "initialTransaction";

    @Inject
    HomeFragment homeFragment;

    @Inject
    MyCredentialsFragment myCredentialsFragment;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        super.onCreate(savedInstanceState);

        bottomAppBar.setListener(this);
        fab.setBackgroundTintList(colorRed);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        init();

        Preferences prefs = new Preferences(this);
        if (prefs.isPinConfigured())
            navigator.showUnlockScreen(this);
    }

    private void init() {
        viewModel.getHasConnectionsInitialScreenLiveData().observe(this, asyncTaskResult -> {
            if (asyncTaskResult.getError() != null) {
                FirebaseCrashlytics.getInstance().recordException(asyncTaskResult.getError());
                return;
            }
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            if (asyncTaskResult.getResult()) {
                FirstConnectionFragment f = FirstConnectionFragment.newInstance(R.string.notifications);
                ft.replace(R.id.fragment_layout, f, MAIN_FRAGMENT_TAG);
            } else {
                ft.replace(R.id.fragment_layout, homeFragment, MAIN_FRAGMENT_TAG);
            }
            ft.commit();
        });
        viewModel.getHasConnectionsMoveToContactLiveData().observe(this, asyncTaskResult -> {
            if (asyncTaskResult.getError() != null) {
                FirebaseCrashlytics.getInstance().recordException(asyncTaskResult.getError());
                return;
            }

            if (asyncTaskResult.getResult()) {
                FirstConnectionFragment firstConnectionFragment = FirstConnectionFragment.newInstance(R.string.contacts);
                changeFragment(firstConnectionFragment);
            } else {
                changeFragment(contactsFragment);
            }
        });

        viewModel.checkIfHasConnectionsInitialScreen();
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
    public void onBackPressed() {
        Fragment currentFragment = getCurrentFragment();

        if (isBottomBarOptionScreen(currentFragment) || isFirstConnetionContacts(currentFragment)) {
            getSupportFragmentManager().popBackStack(INITIAL_TRANSACTION, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            bottomAppBar.setItemColors(null);
            fab.setBackgroundTintList(colorRed);
        } else if (isInitialScreen(currentFragment)) {
            this.finish();
        } else {
            getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
            getSupportFragmentManager().popBackStack();
        }
    }

    private boolean isFirstConnetionContacts(Fragment currentFragment) {
        return currentFragment instanceof FirstConnectionFragment && ((FirstConnectionFragment) currentFragment).getIdTitle() == R.string.contacts;
    }

    private boolean isBottomBarOptionScreen(Fragment currentFragment) {
        return currentFragment instanceof MyCredentialsFragment || currentFragment instanceof ContactsFragment || currentFragment instanceof SettingsFragment || currentFragment instanceof ProfileFragment;
    }

    @Override
    public void onNavigation(BottomAppBarOption option) {
        if (BottomAppBarOption.HOME.equals(option) || BottomAppBarOption.FIRSTCONNECTION.equals(option)) {
            fab.setBackgroundTintList(colorRed);
        } else {
            fab.setBackgroundTintList(colorBlack);
        }
        bottomAppBar.setItemColors(option);

        if (option == CONTACTS) {
            viewModel.checkIfHasConnectionsMoveToContacts();
            return;
        }

        getFragmentToRender(option)
                .ifPresent(cvpFragment -> {
                    changeFragment(cvpFragment);
                });
    }

    private void changeFragment(CvpFragment cvpFragment) {
        Fragment currentFragment = getCurrentFragment();

        if (currentFragment instanceof ContactsFragment && cvpFragment instanceof ContactsFragment) {
            ((ContactsFragment) currentFragment).getViewModel().getAllMessages();
        } else {
            String transactionTag = isInitialScreen(currentFragment) ? INITIAL_TRANSACTION : null;
            navigator.showFragment(getSupportFragmentManager(), cvpFragment, MAIN_FRAGMENT_TAG, transactionTag);
        }
    }

    private Optional<CvpFragment> getFragmentToRender(BottomAppBarOption option) {
        switch (option) {
            case CREDENTIAL:
                return Optional.of(myCredentialsFragment);
            case HOME:
                return Optional.of(homeFragment);
            case SETTINGS:
                return Optional.of(settingsFragment);
            case PROFILE:
                return Optional.of(profileFragment);
            case FIRSTCONNECTION:
                FirstConnectionFragment fragment = FirstConnectionFragment.newInstance(R.string.notifications);
                return Optional.of(fragment);
            default:
                FirebaseCrashlytics.getInstance().recordException(new CaseNotFoundException("Couldn't find fragment for option " + option,
                        ErrorCode.STEP_NOT_FOUND));
                return Optional.empty();
        }
    }

    private Fragment getCurrentFragment() {
        return this.getSupportFragmentManager()
                .findFragmentByTag(MAIN_FRAGMENT_TAG);
    }

    private boolean isInitialScreen(Fragment currentFragment) {
        return currentFragment instanceof HomeFragment || currentFragment instanceof FirstConnectionFragment;
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
        paymentHistoryMenuItem = menu.findItem(R.id.action_history);
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
                    FirebaseCrashlytics.getInstance().recordException(error);
                    // TODO show error message
                }
            }
        }
    }

    @OnClick(R.id.fab)
    public void onFabClick() {
        onNavigation(BottomAppBarOption.HOME);
    }

    private void sendPayments(BigDecimal amount, String nonce, String connectionToken,
                              Preferences prefs) {
        viewModel
                .addConnectionFromToken(connectionToken, "")
                .observe(this, response -> {
                    if (response.getError() != null) {
                        getNavigator().showPopUp(getSupportFragmentManager(), getResources().getString(
                                R.string.server_error_message));
                        return;
                    }
                    AddConnectionFromTokenResponse info = response.getResult();
                    onNavigation(CONTACTS);
                    bottomAppBar.setItemColors(CONTACTS);
                });
    }

    public void acceptConnection(String connectionToken) {
        LiveData<AsyncTaskResult<AddConnectionFromTokenResponse>> asyncTaskResultLiveData = viewModel
                .addConnectionFromToken(connectionToken, "");
        if (asyncTaskResultLiveData.hasActiveObservers()) {
            return;
        }
        asyncTaskResultLiveData.observe(this, response -> {
            if (response.getError() != null) {
                getNavigator().showPopUp(getSupportFragmentManager(), getResources().getString(
                        R.string.server_error_message));
                return;
            }
            onNavigation(CONTACTS);
            bottomAppBar.setItemColors(CONTACTS);
        });

    }

    public void sentFirebaseAnalyticsEvent(String eventName) {
        mFirebaseAnalytics.logEvent(eventName, null);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }
}
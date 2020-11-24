package io.iohk.atala.prism.app.views.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import io.iohk.atala.prism.app.grpc.AsyncTaskResult;
import io.iohk.atala.prism.app.views.fragments.NotificationsFragment;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.core.exception.CaseNotFoundException;
import io.iohk.atala.prism.app.core.exception.ErrorCode;
import io.iohk.atala.prism.app.viewmodel.MainViewModel;
import io.iohk.atala.prism.app.views.Navigator;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;
import io.iohk.atala.prism.app.views.fragments.ContactsFragment;
import io.iohk.atala.prism.app.views.fragments.CvpDialogFragment;
import io.iohk.atala.prism.app.views.fragments.CvpFragment;
import io.iohk.atala.prism.app.views.fragments.MyCredentialsFragment;
import io.iohk.atala.prism.app.views.fragments.ProfileFragment;
import io.iohk.atala.prism.app.views.fragments.SettingsFragment;
import io.iohk.atala.prism.app.views.fragments.ShareProofRequestDialogFragment;
import io.iohk.atala.prism.app.views.interfaces.ConnectionManageable;
import io.iohk.atala.prism.app.views.interfaces.FirebaseEventLogger;
import io.iohk.atala.prism.app.views.interfaces.MainActivityEventHandler;
import io.iohk.atala.prism.app.views.utils.components.bottomAppBar.BottomAppBar;
import io.iohk.atala.prism.app.views.utils.components.bottomAppBar.BottomAppBarListener;
import io.iohk.atala.prism.app.views.utils.components.bottomAppBar.BottomAppBarOption;
import io.iohk.atala.prism.protos.AddConnectionFromTokenResponse;
import lombok.Getter;

import static io.iohk.atala.prism.app.utils.ActivitiesRequestCodes.BRAINTREE_REQUEST_ACTIVITY;
import static io.iohk.atala.prism.app.data.local.preferences.Preferences.CONNECTION_TOKEN_TO_ACCEPT;
import static io.iohk.atala.prism.app.views.utils.components.bottomAppBar.BottomAppBarOption.CONTACTS;

public class MainActivity extends CvpActivity<MainViewModel> implements BottomAppBarListener, FirebaseEventLogger, ConnectionManageable, MainActivityEventHandler {

    public static final String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT";

    private static final String INITIAL_TRANSACTION = "initialTransaction";

    @Inject
    NotificationsFragment notificationsFragment;

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
        onNavigation(BottomAppBarOption.NOTIFICATIONS);
        // handle when exist a message with a proof requests
        viewModel.getCredentialsRequests().observe(this, credentialsRequests -> {
            if (!credentialsRequests.isEmpty()) {
                // TODO this code only shows the dialog of the first message, it needs to fix this so that it can handle more than one
                CvpDialogFragment dialog = ShareProofRequestDialogFragment.newInstance(credentialsRequests.get(0));
                getNavigator().showDialogFragment(getSupportFragmentManager(), dialog, null);
            }
        });
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

        if (isBottomBarOptionScreen(currentFragment)) {
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

    private boolean isBottomBarOptionScreen(Fragment currentFragment) {
        return currentFragment instanceof MyCredentialsFragment || currentFragment instanceof ContactsFragment || currentFragment instanceof SettingsFragment || currentFragment instanceof ProfileFragment;
    }

    @Override
    public void onNavigation(BottomAppBarOption option) {
        if (BottomAppBarOption.NOTIFICATIONS.equals(option)) {
            fab.setBackgroundTintList(colorRed);
        } else {
            fab.setBackgroundTintList(colorBlack);
        }
        bottomAppBar.setItemColors(option);
        getFragmentToRender(option)
                .ifPresent(cvpFragment -> {
                    changeFragment(cvpFragment);
                });
    }

    private void changeFragment(CvpFragment cvpFragment) {
        Fragment currentFragment = getCurrentFragment();
        String transactionTag = isInitialScreen(currentFragment) ? INITIAL_TRANSACTION : null;
        navigator.showFragment(getSupportFragmentManager(), cvpFragment, MAIN_FRAGMENT_TAG, transactionTag);
    }

    private Optional<CvpFragment> getFragmentToRender(BottomAppBarOption option) {
        switch (option) {
            case CREDENTIAL:
                return Optional.of(myCredentialsFragment);
            case NOTIFICATIONS:
                return Optional.of(notificationsFragment);
            case SETTINGS:
                return Optional.of(settingsFragment);
            case PROFILE:
                return Optional.of(profileFragment);
            case CONTACTS:
                return Optional.of(contactsFragment);
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
        return currentFragment instanceof NotificationsFragment;
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
        onNavigation(BottomAppBarOption.NOTIFICATIONS);
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


    /*
     * MainActivityEventHandler is a temporary solution that helps us to know when a new
     * Contact / Connection has been added and thus request a messages synchronization.
     * this will be discarded when there is a stream of events in the backend or we have
     * the appropriate data repositories in this application.
     */
    @Override
    public void handleEvent(MainActivityEvent event) {
        if (event.equals(MainActivityEvent.NEW_CONTACT)) {
            // navigate to contact TAB
            onNavigation(CONTACTS);
            bottomAppBar.setItemColors(CONTACTS);
            // request for a message sync
            viewModel.syncMessages();
        }
    }
}
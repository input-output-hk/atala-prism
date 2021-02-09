package io.iohk.atala.prism.app.ui.main;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Optional;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest;
import io.iohk.atala.prism.app.neo.sync.AuthenticatorService;
import io.iohk.atala.prism.app.ui.CvpActivity;
import io.iohk.atala.prism.app.ui.main.notifications.NotificationsFragment;
import io.iohk.atala.prism.app.ui.commondialogs.ProofRequestDialogFragment;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.core.exception.CaseNotFoundException;
import io.iohk.atala.prism.app.core.exception.ErrorCode;
import io.iohk.atala.prism.app.ui.Navigator;
import io.iohk.atala.prism.app.ui.main.contacts.ContactsFragment;
import io.iohk.atala.prism.app.ui.CvpFragment;
import io.iohk.atala.prism.app.ui.main.credentials.MyCredentialsFragment;
import io.iohk.atala.prism.app.ui.main.profile.ProfileFragment;
import io.iohk.atala.prism.app.ui.main.settings.SettingsFragment;
import io.iohk.atala.prism.app.ui.utils.interfaces.MainActivityEventHandler;
import io.iohk.atala.prism.app.ui.utils.components.bottomAppBar.BottomAppBar;
import io.iohk.atala.prism.app.ui.utils.components.bottomAppBar.BottomAppBarListener;
import io.iohk.atala.prism.app.ui.utils.components.bottomAppBar.BottomAppBarOption;
import lombok.Getter;

import static io.iohk.atala.prism.app.ui.utils.components.bottomAppBar.BottomAppBarOption.CONTACTS;

public class MainActivity extends CvpActivity<MainViewModel> implements BottomAppBarListener, MainActivityEventHandler {

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

    private Account genericSyncAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        super.onCreate(savedInstanceState);

        bottomAppBar.setListener(this);
        fab.setBackgroundTintList(colorRed);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        init();

        genericSyncAccount = AuthenticatorService.Companion.buildGenericAccountForSync(this);
    }

    private void init() {
        viewModel.checkSecuritySettings();
        onNavigation(BottomAppBarOption.NOTIFICATIONS);

        // handle when exist a message with a proof requests
        viewModel.getProofRequest().observe(this, event -> {
            if (event == null) {
                return;
            }
            ProofRequest proofRequest = event.getContentIfNotHandled();
            if (proofRequest != null) {
                ProofRequestDialogFragment dialog = ProofRequestDialogFragment.Companion.build(proofRequest.getId());
                dialog.show(getSupportFragmentManager(), null);
            }
        });

        viewModel.getRequestSync().observe(this, event -> {
            if (event.getContentIfNotHandled()) {
                requestForSync();
            }
        });

        viewModel.getSecurityViewShouldBeVisible().observe(this, event -> {
            if (event.getContentIfNotHandled()) {
                navigator.showUnlockScreen(this);
            }
        });
    }

    @Override
    public MainViewModel getViewModel() {
        MainViewModel viewModel = ViewModelProviders.of(this, factory).get(MainViewModel.class);
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

    @OnClick(R.id.fab)
    public void onFabClick() {
        onNavigation(BottomAppBarOption.NOTIFICATIONS);
    }

    /*
     * MainActivityEventHandler is a temporary solution that helps us to know when a new
     * Contact / Connection has been added or a proof request accepted and this request a
     * messages synchronization.
     * This will be discarded when there is a stream of events in the backend or we have
     * the appropriate data repositories in this application.
     */
    @Override
    public void handleEvent(MainActivityEvent event) {
        if (event.equals(MainActivityEvent.SYNC_REQUEST)) {
            // navigate to contact TAB
            onNavigation(CONTACTS);
            bottomAppBar.setItemColors(CONTACTS);
        }
    }

    /*
     * Is a temporary solution this will be discarded when there is a stream of events in the backend or we have
     * the appropriate data repositories in this application.
     * */
    private void requestForSync() {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        /*
         * Request the sync for the generic account, authority, and
         * manual sync settings
         */
        ContentResolver.requestSync(genericSyncAccount, AuthenticatorService.ACCOUNT_AUTHORITY, settingsBundle);
    }
}
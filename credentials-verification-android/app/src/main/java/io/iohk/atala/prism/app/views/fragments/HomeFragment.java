package io.iohk.atala.prism.app.views.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.atala.prism.app.grpc.AsyncTaskResult;
import io.iohk.cvp.R;
import io.iohk.atala.prism.app.data.DataManager;
import io.iohk.atala.prism.app.data.local.db.model.Credential;
import io.iohk.atala.prism.app.utils.ActivitiesRequestCodes;
import io.iohk.atala.prism.app.utils.FirebaseAnalyticsEvents;
import io.iohk.atala.prism.app.utils.IntentDataConstants;
import io.iohk.atala.prism.app.viewmodel.CredentialsViewModel;
import io.iohk.atala.prism.app.views.activities.MainActivity;
import io.iohk.atala.prism.app.views.fragments.utils.ActionBarUtils;
import io.iohk.atala.prism.app.views.fragments.utils.AppBarConfigurator;
import io.iohk.atala.prism.app.views.fragments.utils.RootAppBar;
import io.iohk.atala.prism.app.views.utils.adapters.CredentialClickListener;
import io.iohk.atala.prism.app.views.utils.adapters.NewCredentialsRecyclerViewAdapter;
import io.iohk.atala.prism.app.views.utils.components.bottomAppBar.BottomAppBarOption;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO This needs its own [ViewModel]
@Setter
@NoArgsConstructor
public class HomeFragment extends CvpFragment<CredentialsViewModel> implements CredentialClickListener {

    @BindView(R.id.new_credentials_container)
    ConstraintLayout newCredentialsContainer;

    @BindView(R.id.loading)
    RelativeLayout loading;

    @BindView(R.id.credentials_list)
    RecyclerView credentialsRecyclerView;

    @BindView(R.id.fragment_layout)
    FrameLayout fragmentLayout;

    @Inject
    DataManager dataManager;

    @Inject
    ViewModelProvider.Factory factory;

    private NewCredentialsRecyclerViewAdapter credentialsAdapter;

    @Override
    protected int getViewId() {
        return R.layout.fragment_credentials;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new RootAppBar(R.string.notifications);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        credentialsAdapter = new NewCredentialsRecyclerViewAdapter(R.layout.row_new_credential, this, true, getContext());
        credentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        credentialsRecyclerView.setAdapter(credentialsAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loading.setVisibility(View.VISIBLE);
        listNewCredentials();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initObservers();
    }

    private void initObservers() {
        LiveData<AsyncTaskResult<List<Credential>>> live = viewModel.getCredentialLiveData();
        live.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result.getError() != null) {
                    getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                            R.string.server_error_message));
                    return;
                }
                List<Credential> messages = result.getResult();
                if (messages != null) {
                    credentialsAdapter.addMesseges(messages);
                    if (messages.isEmpty()) {
                        ((MainActivity) getActivity()).onNavigation(BottomAppBarOption.FIRSTCONNECTION);
                    } else {
                        //SHOW QR BUTTON
                        setHasOptionsMenu(true);
                    }
                }
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                loading.setVisibility(View.GONE);
            }
        });
    }

    public void listNewCredentials() {
        credentialsAdapter.clearMessages();
        getViewModel().getNewCredentials();
    }

    @Override
    public CredentialsViewModel getViewModel() {

        CredentialsViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(CredentialsViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    @Override
    public void onCredentialClickListener(Boolean isNew, Credential credential) {

        ((MainActivity) getActivity()).sentFirebaseAnalyticsEvent(FirebaseAnalyticsEvents.NEW_CREDENTIAL_VIEW);

        CredentialDetailFragment credentialFragment = new CredentialDetailFragment();
        credentialFragment.setCredential(credential);
        credentialFragment.setCredentialIsNew(isNew);

        navigator.showFragmentOnTop(
                requireActivity().getSupportFragmentManager(), credentialFragment);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        ActionBarUtils.setupMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (ActionBarUtils.menuItemClicked(navigator, item, this))
            return true;
        // If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivitiesRequestCodes.QR_SCANNER_REQUEST_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK && data.hasExtra(IntentDataConstants.QR_RESULT)) {
                final String token = data.getStringExtra(IntentDataConstants.QR_RESULT);
                // TODO momentary solution, the use of "safeArgs" will be implemented
                AcceptConnectionDialogFragment dialog = AcceptConnectionDialogFragment.Companion.build(token);
                dialog.show(requireActivity().getSupportFragmentManager(), null);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
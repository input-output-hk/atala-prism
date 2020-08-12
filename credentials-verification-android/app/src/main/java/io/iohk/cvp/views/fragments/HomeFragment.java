package io.iohk.cvp.views.fragments;

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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.data.DataManager;
import io.iohk.cvp.data.local.db.model.Credential;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.utils.ActivityUtils;
import io.iohk.cvp.utils.FirebaseAnalyticsEvents;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.ActionBarUtils;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.CredentialClickListener;
import io.iohk.cvp.views.utils.adapters.NewCredentialsRecyclerViewAdapter;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    CredentialDetailFragment credentialFragment;

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
        registerTokenInfoObserver();
        initObservers();
    }

    private void registerTokenInfoObserver() {
        ActivityUtils.registerObserver((MainActivity) getActivity(),
                viewModel);
    }

    private void initObservers() {

        MutableLiveData<AsyncTaskResult<List<Credential>>> live = viewModel.getCredentialLiveData();
        live.observe(getViewLifecycleOwner(), result -> {
            try {
                if (result.getError() != null) {
                    getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                            R.string.server_error_message));
                    return;
                }
                List<Credential> messages = result.getResult();
                if(messages != null) {
                    credentialsAdapter.addMesseges(messages);
                    if (messages.isEmpty()) {
                        ((MainActivity) getActivity()).onNavigation(BottomAppBarOption.FIRSTCONNECTION, null);
                    } else {
                        //SHOW QR BUTTON
                        setHasOptionsMenu(true);
                    }
                }
            } catch (Exception e) {
                Crashlytics.logException(e);
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

        ((MainActivity)getActivity()).sentFirebaseAnalyticsEvent(FirebaseAnalyticsEvents.NEW_CREDENTIAL_VIEW);
        credentialFragment.setCredential(credential);
        credentialFragment.setCredentialIsNew(isNew);

        navigator.showFragmentOnTop(
                Objects.requireNonNull(getActivity()).getSupportFragmentManager(), credentialFragment);
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
        super.onActivityResult(requestCode, resultCode, data);
        ActivityUtils.onQrcodeResult(requestCode, resultCode, viewModel, data);
    }
}
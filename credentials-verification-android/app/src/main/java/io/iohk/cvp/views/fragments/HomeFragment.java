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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.utils.ActivityUtils;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.NewCredentialsRecyclerViewAdapter;
import io.iohk.cvp.views.utils.components.bottomAppBar.BottomAppBarOption;
import io.iohk.prism.protos.AtalaMessage;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.Credential;
import io.iohk.prism.protos.ParticipantInfo;
import io.iohk.prism.protos.ReceivedMessage;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class HomeFragment extends CvpFragment<CredentialsViewModel> {

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

    private List<ConnectionInfo> issuerConnections;

    private ViewModelProvider.Factory factory;
    private MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> liveData;
    private LiveData<AsyncTaskResult<List<ConnectionInfo>>> liveConnectionsData;
    private NewCredentialsRecyclerViewAdapter credentialsAdapter;

    @Inject
    HomeFragment(ViewModelProvider.Factory factory) {
        this.factory = factory;
    }

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

        Preferences pref = new Preferences(getContext());

        View view = super.onCreateView(inflater, container, savedInstanceState);
        credentialsAdapter = new NewCredentialsRecyclerViewAdapter(R.layout.row_new_credential, this, true, pref);
        credentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        credentialsRecyclerView.setAdapter(credentialsAdapter);

        issuerConnections = new ArrayList<ConnectionInfo>();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loading.setVisibility(View.VISIBLE);
        listConnections();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerTokenInfoObserver();
    }

    private void registerTokenInfoObserver() {
            ActivityUtils.registerObserver((MainActivity) getActivity(),
                    viewModel, this, issuerConnections);
    }

    public void listNewCredentials() {
        credentialsAdapter.clearMessages();
        liveData = viewModel.getMessages(this.getUserIds());
        if (!liveData.hasActiveObservers()) {
            liveData.observe(this, result -> {
                try {
                    if (result.getResult() == null) {
                        return;
                    }

                    if (result.getError() != null) {
                        getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                                R.string.server_error_message));
                    } else {
                        Preferences pref = new Preferences(getContext());
                        Set<String> acceptedMessagesIds = pref.getStoredMessages(Preferences.ACCEPTED_MESSAGES_KEY);
                        Set<String> rejectedMessagesIds = pref.getStoredMessages(Preferences.REJECTED_MESSAGES_KEY);

                        List<ReceivedMessage> messages = result.getResult();
                        List<ReceivedMessage> newMessages = messages.stream()
                                .filter(msg -> {
                                    try {
                                        AtalaMessage current = AtalaMessage.parseFrom(msg.getMessage());
                                        if (!acceptedMessagesIds.contains(msg.getId()) && !rejectedMessagesIds
                                                .contains(msg.getId()) && !current.getIssuerSentCredential().getCredential().getTypeId().isEmpty()) {
                                            return true;
                                        }
                                        return false;
                                    } catch (Exception e) {
                                        return false;
                                    }
                                }).collect(Collectors.toList());

                        credentialsAdapter.addMesseges(newMessages);

                        if (newMessages.isEmpty()) {
                            ((MainActivity) getActivity()).onNavigation(BottomAppBarOption.FIRSTCONNECTION, null);
                        }else{
                            //SHOW QR BUTTON
                            setHasOptionsMenu(true);
                        }
                        loading.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
            });
        }
    }

    public void listConnections() {
        liveConnectionsData = viewModel.getConnections(this.getUserIds());
        if (liveConnectionsData.hasActiveObservers()) {
            return;
        }

        liveConnectionsData.observe(this, response -> {
            if (response.getResult() == null) {
                return;
            }
            issuerConnections = response.getResult().stream()
                    .filter(conn -> conn.getParticipantInfo().getParticipantCase().getNumber()
                            == ParticipantInfo.ISSUER_FIELD_NUMBER)
                    .collect(Collectors.toList());
            ((MainActivity)getActivity()).setIssuerConnections(issuerConnections);
            listNewCredentials();
        });
    }

    @Override
    public CredentialsViewModel getViewModel() {
        CredentialsViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(CredentialsViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    public void onCredentialClicked(Boolean isNew, Credential credential,
                                    String connectionId, String messageId) {

        credentialFragment.setCredential(credential);
        credentialFragment.setCredentialIsNew(isNew);
        credentialFragment.setConnectionId(connectionId);
        credentialFragment.setMessageId(messageId);

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
        MenuItem paymentHistoryMenuItem;
        paymentHistoryMenuItem = menu.findItem(R.id.action_new_connection);
        paymentHistoryMenuItem.setVisible(true);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_new_connection) {
            navigator.showQrScanner(this);
            return true;
        }
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
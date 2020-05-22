package io.iohk.cvp.views.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.SupportErrorDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.protobuf.InvalidProtocolBufferException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.utils.ActivityUtils;
import io.iohk.cvp.utils.comparator.ConnectionInfoComparator;
import io.iohk.cvp.viewmodel.ConnectionsActivityViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.activities.MainActivity;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.ConnectionTabsAdapter;
import io.iohk.cvp.views.utils.adapters.UniversitiesRecyclerViewAdapter;
import io.iohk.prism.protos.AtalaMessage;
import io.iohk.prism.protos.ConnectionInfo;
import io.iohk.prism.protos.Credential;
import io.iohk.prism.protos.ParticipantInfo;
import io.iohk.prism.protos.ProofRequest;
import io.iohk.prism.protos.ReceivedMessage;
import lombok.Setter;

@Setter
public class ContactsFragment extends CvpFragment<ConnectionsActivityViewModel> {

    @BindView(R.id.loading)
    RelativeLayout loading;

    @Inject
    ViewModelProvider.Factory factory;

    @BindView(R.id.connections_list_tabs)
    TabLayout tabs;

    @BindView(R.id.connections_list_view_pager)
    ViewPager viewPager;

    @Inject
    ConnectionsListFragment connectionsListFragment;

    private LiveData<AsyncTaskResult<List<ConnectionInfo>>> liveData;

    private MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> credentialLiveData;

    private List<ReceivedMessage> proofRequestMessages;

    private List<ConnectionInfo> shareConnections;

    private List<String> proofRequestOpen;

    private List<ConnectionInfo> issuerConnections;

    @Inject
    public ContactsFragment() {
    }

    @Override
    public ConnectionsActivityViewModel getViewModel() {
        ConnectionsActivityViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(ConnectionsActivityViewModel.class);
        viewModel.setContext(getContext());
        return viewModel;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem paymentHistoryMenuItem;
        paymentHistoryMenuItem = menu.findItem(R.id.action_new_connection);
        paymentHistoryMenuItem.setVisible(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        connectionsListFragment.setAdapter(new UniversitiesRecyclerViewAdapter());

        issuerConnections = new ArrayList<>();
        proofRequestMessages = new ArrayList<>();
        shareConnections = new ArrayList<>();
        proofRequestOpen = new ArrayList<>();

        ConnectionTabsAdapter adapter = new ConnectionTabsAdapter(
                getChildFragmentManager(), 1, connectionsListFragment);

        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setCurrentItem(0);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        connectionsListFragment.clearConnecitons();
        listConnections(this.getUserIds());
    }

    public void listConnections(Set<String> userIds) {
        loading.setVisibility(View.VISIBLE);
        liveData = viewModel.getConnections(userIds);
        if (!liveData.hasActiveObservers()) {
            liveData.observe(this, response -> {
                if (response.getResult() == null) {
                    return;
                }

                FragmentManager fm = getFragmentManager();
                if (response.getError() != null) {
                    SupportErrorDialogFragment.newInstance(new Dialog(getContext()))
                            .show(fm, "");
                    getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                            R.string.server_error_message));
                    return;
                }

                List<ConnectionInfo> connections = response.getResult();
                shareConnections.addAll(connections.stream()
                        .filter(conn -> {
                            if (conn.getParticipantInfo().getParticipantCase().getNumber() == ParticipantInfo.ISSUER_FIELD_NUMBER) {
                                for (ConnectionInfo issuerConnection : shareConnections) {
                                    if (conn.getParticipantInfo().getIssuer().getName().equals(issuerConnection.getParticipantInfo()
                                            .getIssuer().getName())) {
                                        return false;
                                    }
                                }
                                getProofRequest();
                                return true;
                            }
                            return false;
                        }).collect(Collectors.toList()));

                issuerConnections = connections.stream()
                        .filter(conn -> conn.getParticipantInfo().getParticipantCase().getNumber()
                                == ParticipantInfo.ISSUER_FIELD_NUMBER)
                        .collect(Collectors.toList());
                issuerConnections.sort(new ConnectionInfoComparator());

                connectionsListFragment.addConnections(issuerConnections);
                ((MainActivity)getActivity()).setIssuerConnections(issuerConnections);

                loading.setVisibility(View.GONE);
                getProofRequest();
            });
        }
    }

    public void getProofRequest() {
        credentialLiveData = viewModel.getMessages(this.getUserIds());
        if (!credentialLiveData.hasActiveObservers()) {
            credentialLiveData.observe(this, result -> {
                try {
                    if (result.getError() == null) {

                        Preferences prefs = new Preferences(getContext());

                        Set<String> acceptedProofRequesIds = prefs.getStoredMessages(Preferences.PROOF_REQUEST_SHARED_KEY);
                        Set<String> cancelProofRequestIds = prefs.getStoredMessages(Preferences.PROOF_REQUEST_CANCEL_KEY);

                        List<ReceivedMessage> messages = result.getResult();
                        proofRequestMessages.addAll(messages.stream()
                                .filter(msg -> {
                                    try {
                                        AtalaMessage newMessage = AtalaMessage.parseFrom(msg.getMessage());
                                        if (!newMessage.getProofRequest().getTypeIdsList().isEmpty() &&
                                                !acceptedProofRequesIds.contains(newMessage.getProofRequest().getConnectionToken()) &&
                                                !cancelProofRequestIds.contains(newMessage.getProofRequest().getConnectionToken())) {

                                            for (ReceivedMessage receivedMessage : proofRequestMessages) {
                                                AtalaMessage current = AtalaMessage.parseFrom(receivedMessage.getMessage());
                                                if (current.getProofRequest().getConnectionToken().equals(newMessage.getProofRequest().getConnectionToken())) {
                                                    return false;
                                                }
                                            }
                                            return true;
                                        }
                                        return false;
                                    } catch (Exception e) {
                                        return false;
                                    }
                                }).collect(Collectors.toList()));

                        if (!proofRequestMessages.isEmpty() && !messages.isEmpty()) {
                            for (ReceivedMessage proofRequestMessage : proofRequestMessages) {
                                ProofRequest proofRequest = AtalaMessage.parseFrom(proofRequestMessage.getMessage()).getProofRequest();
                                //Search for conection
                                ConnectionInfo shareConnection = null;
                                for (ConnectionInfo connection : shareConnections) {
                                    if (connection.getToken().equals(proofRequest.getConnectionToken())) {
                                        shareConnection = connection;
                                        break;
                                    }
                                }

                                //Search for credentials
                                List<Credential> credentialsToShare = messages.stream()
                                        .flatMap(message -> {
                                            try {
                                                Credential credential =  AtalaMessage.parseFrom(message.getMessage()).getIssuerSentCredential().getCredential();
                                                return Stream.of(credential);
                                            } catch (InvalidProtocolBufferException e) {
                                                Log.d("ATALA - Contacs Fragment","Impossible to parse Credential");
                                                return Stream.empty();
                                            }
                                        })
                                        .filter(acceptedMessage ->
                                                proofRequest.getTypeIdsList().contains(acceptedMessage.getTypeId())
                                        )
                                        .collect(Collectors.toList());

                                if (shareConnection != null && credentialsToShare.size() == proofRequest.getTypeIdsList().size() &&
                                        !proofRequestOpen.contains(proofRequest.getConnectionToken())) {

                                    proofRequestOpen.add(proofRequest.getConnectionToken());
                                    getNavigator().showDialogFragment(getFragmentManager(),
                                            ShareProofRequestDialogFragment.newInstance(proofRequest, credentialsToShare, shareConnection),
                                            "SHARE_PROOF_REQUEST_DIALOG_FRAGMENT");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
            });
        }
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
    protected int getViewId() {
        return R.layout.fragment_connections;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new RootAppBar(R.string.contacts);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityUtils.onQrcodeResult(requestCode, resultCode, (MainActivity) getActivity(),
                viewModel, data, this, issuerConnections);
    }
}
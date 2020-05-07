package io.iohk.cvp.views.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import io.iohk.cvp.views.utils.adapters.CredentialsRecyclerViewAdapter;
import io.iohk.prism.protos.AtalaMessage;
import io.iohk.prism.protos.Credential;
import io.iohk.prism.protos.ReceivedMessage;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class MyCredentials extends CvpFragment<CredentialsViewModel> {

    @BindView(R.id.credentials_list)
    RecyclerView credentialsRecyclerView;

    @BindView(R.id.fragment_layout)
    FrameLayout fragmentLayout;

    @BindView(R.id.no_credentials_container)
    LinearLayout noCredentialsContainer;

    @BindView(R.id.loading)
    RelativeLayout loading;

    @Inject
    CredentialDetailFragment credentialFragment;
    private ViewModelProvider.Factory factory;
    private MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> liveData;
    private CredentialsRecyclerViewAdapter credentialsAdapter;

    @Inject
    MyCredentials(ViewModelProvider.Factory factory) {
        this.factory = factory;
    }

    @Override
    protected int getViewId() {
        return R.layout.fragment_home;
    }

    @Override
    protected AppBarConfigurator getAppBarConfigurator() {
        return new RootAppBar(R.string.home_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);
        Preferences prefs = new Preferences(getContext());
        credentialsAdapter = new CredentialsRecyclerViewAdapter(R.layout.row_credential, this, false, prefs);
        credentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        credentialsRecyclerView.setAdapter(credentialsAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        listMyCredentials();
    }

    public void listMyCredentials() {
        loading.setVisibility(View.VISIBLE);
        credentialsAdapter.clearMessages();
        liveData = viewModel.getMessages(this.getUserIds());
        if (!liveData.hasActiveObservers()) {
            liveData.observe(this, result -> {
                try {

                    if(result.getResult() == null){
                        return;
                    }

                    if (result.getError() != null) {
                        getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                                R.string.server_error_message));
                    } else {
                        Preferences prefs = new Preferences(getContext());
                        Set<String> acceptedMessagesIds = prefs.getStoredMessages(Preferences.ACCEPTED_MESSAGES_KEY);

                        List<ReceivedMessage> messages = result.getResult();
                        List<ReceivedMessage> acceptedMessages = messages.stream()
                                .filter(msg -> acceptedMessagesIds.contains(msg.getId())).collect(
                                        Collectors.toList());

                        credentialsAdapter.addMesseges(acceptedMessages);
                        if (acceptedMessages.isEmpty()) {
                            credentialsRecyclerView.setVisibility(View.GONE);
                            noCredentialsContainer.setVisibility(View.VISIBLE);
                        }

                        loading.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    loading.setVisibility(View.GONE);
                    Crashlytics.logException(e);
                }
            });
        }
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
}
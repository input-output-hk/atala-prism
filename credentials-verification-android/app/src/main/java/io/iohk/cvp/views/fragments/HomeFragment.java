package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import com.crashlytics.android.Crashlytics;
import io.iohk.cvp.R;
import io.iohk.cvp.grpc.AsyncTaskResult;
import io.iohk.cvp.io.connector.ReceivedMessage;
import io.iohk.cvp.io.credential.SentCredential;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.CredentialsRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class HomeFragment extends CvpFragment<CredentialsViewModel> {

  @BindView(R.id.credentials_list)
  RecyclerView credentialsRecyclerView;
  @BindView(R.id.new_credentials_list)
  RecyclerView newCredentialsRecyclerView;
  @BindView(R.id.fragment_layout)
  FrameLayout fragmentLayout;
  @BindView(R.id.new_credentials_container)
  ConstraintLayout newCredentialsContainer;
  @BindView(R.id.username)
  TextView textViewUsername;
  @BindView(R.id.no_credentials_container)
  LinearLayout noCredentialsContainer;
  @Inject
  CredentialDetailFragment credentialFragment;
  private ViewModelProvider.Factory factory;
  private MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>> liveData;
  private CredentialsRecyclerViewAdapter newCredentialsAdapter;
  private CredentialsRecyclerViewAdapter credentialsAdapter;

  @Inject
  HomeFragment(ViewModelProvider.Factory factory) {
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

    newCredentialsAdapter = new CredentialsRecyclerViewAdapter(R.layout.row_new_credential, this,
        true, prefs);
    credentialsAdapter = new CredentialsRecyclerViewAdapter(R.layout.row_credential, this, false,
        prefs);

    textViewUsername.setText(Objects.requireNonNull(getContext())
        .getString(R.string.hello_username, prefs.getString(Preferences.USER_PROFILE_NAME)));

    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    // Init credentialsRecyclerView
    credentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    credentialsRecyclerView.setAdapter(credentialsAdapter);

    // Init newCredentialsRecyclerView
    newCredentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    newCredentialsRecyclerView.setAdapter(newCredentialsAdapter);

    newCredentialsAdapter.clearMessages();
    credentialsAdapter.clearMessages();

    //HARDCODED NEW CREDENTIAL
    List<ReceivedMessage> hardcoreMessages = new ArrayList<>();
    ReceivedMessage message = ReceivedMessage.getDefaultInstance();
    hardcoreMessages.add(message);
    newCredentialsAdapter.addMesseges(hardcoreMessages);

    try {
      liveData = viewModel.getMessages(this.getUserIds());

      if (!liveData.hasActiveObservers()) {
        liveData.observe(this, result -> {

          if (result.getError() != null) {
            getNavigator().showPopUp(getFragmentManager(), getResources().getString(
                R.string.server_error_message));
          } else {
            Preferences prefs = new Preferences(getContext());

            Set<String> acceptedMessagesIds = prefs
                .getStoredMessages(Preferences.ACCEPTED_MESSAGES_KEY);
            Set<String> rejectedMessagesIds = prefs
                .getStoredMessages(Preferences.REJECTED_MESSAGES_KEY);

            List<ReceivedMessage> messages = result.getResult();

            List<ReceivedMessage> newMessages = messages.stream()
                .filter(msg -> !acceptedMessagesIds.contains(msg.getId()) && !rejectedMessagesIds
                    .contains(msg.getId())).collect(
                    Collectors.toList());

            newCredentialsAdapter.addMesseges(newMessages);

            List<ReceivedMessage> acceptedMessages = messages.stream()
                .filter(msg -> acceptedMessagesIds.contains(msg.getId())).collect(
                    Collectors.toList());

            //HARDCODED SAVED CREDENTIAL
            ReceivedMessage hardcoreOldMessage = ReceivedMessage.getDefaultInstance();
            acceptedMessages.add(hardcoreOldMessage);

            credentialsAdapter.clearMessages();
            credentialsAdapter.addMesseges(acceptedMessages);

            if (!acceptedMessages.isEmpty()) {
              credentialsRecyclerView.setVisibility(View.VISIBLE);
              noCredentialsContainer.setVisibility(View.GONE);
            }

            if (!newMessages.isEmpty()) {
              newCredentialsContainer.setVisibility(View.VISIBLE);
            }
          }
        });
      }
    } catch (Exception e) {
      Crashlytics.logException(e);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    liveData.removeObservers(this);
    viewModel.clearMessages();
  }


  @Override
  public CredentialsViewModel getViewModel() {
    CredentialsViewModel viewModel = ViewModelProviders.of(this, factory)
        .get(CredentialsViewModel.class);
    viewModel.setContext(getContext());
    return viewModel;
  }

  public void onCredentialClicked(Boolean isNew, SentCredential credential,
      String connectionId, String messageId) {
    credentialFragment.setCredential(credential);
    credentialFragment.setCredentialIsNew(isNew);
    credentialFragment.setConnectionId(connectionId);
    credentialFragment.setMessageId(messageId);
    navigator.showFragmentOnTop(
        Objects.requireNonNull(getActivity()).getSupportFragmentManager(), credentialFragment);
  }
}

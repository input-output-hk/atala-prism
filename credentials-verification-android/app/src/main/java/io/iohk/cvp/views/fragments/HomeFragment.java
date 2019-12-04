package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.ReceivedMessage;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.CredentialsRecyclerViewAdapter;
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

  private ViewModelProvider.Factory factory;

  @Inject
  HomeFragment(ViewModelProvider.Factory factory) {
    this.factory = factory;
  }

  @Inject
  Navigator navigator;

  @BindView(R.id.credentials_list)
  RecyclerView credentialsRecyclerView;

  @BindView(R.id.new_credentials_list)
  RecyclerView newCredentialsRecyclerView;

  @BindView(R.id.fragment_layout)
  FrameLayout fragmentLayout;

  @Inject
  CredentialDetailFragment credentialFragment;

  private CredentialsRecyclerViewAdapter newCredentialsAdapter;
  private CredentialsRecyclerViewAdapter credentialsAdapter;

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
    newCredentialsAdapter = new CredentialsRecyclerViewAdapter(R.layout.row_new_credential, this,
        true);
    credentialsAdapter = new CredentialsRecyclerViewAdapter(R.layout.row_credential, this, false);

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

    LiveData<List<ReceivedMessage>> liveData = viewModel.getMessages(this.getUserIds());

    if (!liveData.hasActiveObservers()) {
      liveData.observe(this, messages -> {
        Preferences prefs = new Preferences(getContext());

        Set<String> acceptedMessagesIds = prefs
            .getStoredMessages(Preferences.ACCEPTED_MESSAGES_KEY);
        Set<String> rejectedMessagesIds = prefs
            .getStoredMessages(Preferences.REJECTED_MESSAGES_KEY);

        List<ReceivedMessage> newMessages = messages.stream()
            .filter(msg -> !acceptedMessagesIds.contains(msg.getId()) && !rejectedMessagesIds
                .contains(msg.getId())).collect(
                Collectors.toList());

        newCredentialsAdapter.addMesseges(newMessages);

        List<ReceivedMessage> acceptedMessages = messages.stream()
            .filter(msg -> acceptedMessagesIds.contains(msg.getId())).collect(
                Collectors.toList());

        credentialsAdapter.addMesseges(acceptedMessages);
      });
    }
  }


  @Override
  public CredentialsViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(CredentialsViewModel.class);
  }

  public void onCredentialClicked(Boolean isNew, String credentialId) {
    credentialFragment.setCredentialId(credentialId);
    credentialFragment.setCredentialIsNew(isNew);
    navigator.showFragmentOnTop(
        Objects.requireNonNull(getActivity()).getSupportFragmentManager(), credentialFragment);
  }
}

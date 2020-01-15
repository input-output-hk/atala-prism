package io.iohk.cvp.views.fragments;

import static io.iohk.cvp.utils.IntentDataConstants.CREDENTIAL_DATA_KEY;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.OnClick;
import com.crashlytics.android.Crashlytics;
import com.google.protobuf.InvalidProtocolBufferException;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.ErrorCode;
import io.iohk.cvp.core.exception.SharedPrefencesDataNotFoundException;
import io.iohk.cvp.io.credential.Credential;
import io.iohk.cvp.io.credential.HolderSentCredential;
import io.iohk.cvp.io.credential.SentCredential;
import io.iohk.cvp.viewmodel.ConnectionsListablesViewModel;
import io.iohk.cvp.viewmodel.dtos.ConnectionListable;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.utils.adapters.ShareCredentialRecyclerViewAdapter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ShareCredentialDialogFragment extends CvpFragment<ConnectionsListablesViewModel> {

  private ViewModelProvider.Factory factory;

  @BindView(R.id.background)
  public ConstraintLayout background;

  @BindView(R.id.verifier_recycler_view)
  public RecyclerView recyclerView;

  @BindView(R.id.share_button)
  public Button button;

  private ShareCredentialRecyclerViewAdapter adapter;

  private Set<String> selectedVerifiers = new HashSet<>();


  @Inject
  ShareCredentialDialogFragment(ViewModelProvider.Factory factory) {
    this.factory = factory;
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    adapter = new ShareCredentialRecyclerViewAdapter(this);
    recyclerView.setLayoutManager(
        new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false)
    );
    recyclerView.setAdapter(adapter);

    LiveData<List<ConnectionListable>> liveData = viewModel.getConnections(this.getUserIds());

    if (!liveData.hasActiveObservers()) {
      liveData.observe(this, connections -> {
        adapter.addConnections(connections);
        adapter.notifyDataSetChanged();
      });
    }
    return view;
  }

  @OnClick(R.id.background)
  void onBackgroundClick() {
    Objects.requireNonNull(getActivity()).onBackPressed();
  }

  @Override
  public ConnectionsListablesViewModel getViewModel() {
    ConnectionsListablesViewModel viewModel = ViewModelProviders.of(this, factory)
        .get(ConnectionsListablesViewModel.class);
    viewModel.setContext(getContext());
    return viewModel;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return null;
  }

  @Override
  protected int getViewId() {
    return R.layout.component_share_credential_dialog;
  }

  public void updateButtonState() {
    button.setEnabled(adapter.areConnectionsSelected());
  }

  @OnClick(R.id.share_button)
  public void onShareClick() {
    Preferences prefs = new Preferences(getContext());
    selectedVerifiers.forEach(connectionId -> {
      try {
        String userId = prefs.getUserIdByConnection(connectionId).orElseThrow(() ->
            new SharedPrefencesDataNotFoundException(
                "Couldn't find user id for connection id " + connectionId,
                ErrorCode.USER_ID_NOT_FOUND));

        HolderSentCredential sentCredential = HolderSentCredential.newBuilder().setCredential(
            Credential.parseFrom(getArguments().getByteArray(CREDENTIAL_DATA_KEY))).build();

        viewModel
            .sendMessage(userId, connectionId,
                SentCredential.newBuilder().setHolderSentCredential(sentCredential)
                    .build().toByteString());
      } catch (SharedPrefencesDataNotFoundException | InvalidProtocolBufferException e) {
        Crashlytics.logException(e);
      }
    });
    onBackgroundClick();
  }

  public void updateSelectedVerifiers(String connectionId, Boolean isSelected) {
    if (isSelected) {
      selectedVerifiers.add(connectionId);
    } else {
      selectedVerifiers.remove(connectionId);
    }
  }
}
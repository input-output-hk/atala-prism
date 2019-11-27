package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.ConnectionsListablesViewModel;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.utils.adapters.ShareCredentialRecyclerViewAdapter;
import java.util.Objects;
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
    viewModel.getConnections().observe(this, connections -> {
      adapter.setConnections(connections);
      adapter.notifyDataSetChanged();
    });
    return view;
  }

  @OnClick(R.id.background)
  void onBackgroundClick() {
    Objects.requireNonNull(getActivity()).onBackPressed();
  }

  @Override
  public ConnectionsListablesViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(ConnectionsListablesViewModel.class);
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
}
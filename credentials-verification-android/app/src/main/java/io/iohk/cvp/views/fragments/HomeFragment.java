package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.RootAppBar;
import io.iohk.cvp.views.utils.adapters.CredentialsRecyclerViewAdapter;
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

  private final CredentialsRecyclerViewAdapter newCredentialsAdapter
      = new CredentialsRecyclerViewAdapter(R.layout.row_new_credential, this, true);
  private final CredentialsRecyclerViewAdapter credentialsAdapter
      = new CredentialsRecyclerViewAdapter(R.layout.row_credential, this, false);

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

    // Init credentialsRecyclerView
    credentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    credentialsRecyclerView.setAdapter(credentialsAdapter);

    // Init newCredentialsRecyclerView
    newCredentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    newCredentialsRecyclerView.setAdapter(newCredentialsAdapter);

    viewModel.getCredentials().observe(this, credentials -> {
      credentialsAdapter.setCredentials(credentials);
      credentialsAdapter.notifyDataSetChanged();

      newCredentialsAdapter.setCredentials(credentials);
      newCredentialsAdapter.notifyDataSetChanged();
    });
    return view;
  }

  @Override
  public CredentialsViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(CredentialsViewModel.class);
  }

  public void onCredentialClicked(String credentialId) {
    credentialFragment.setCredentialId(credentialId);
    navigator.showFragmentOnTop(
        Objects.requireNonNull(getActivity()).getSupportFragmentManager(), credentialFragment);
  }
}

package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import io.iohk.cvp.io.connector.CredentialType;
import java.util.Objects;

import javax.inject.Inject;

import io.iohk.cvp.R;
import io.iohk.cvp.viewmodel.CredentialsViewModel;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class CredentialDetailFragment extends CvpFragment<CredentialsViewModel> {

  private ViewModelProvider.Factory factory;

  @Inject
  CredentialDetailFragment(ViewModelProvider.Factory factory) {
    this.factory = factory;
  }

  @Override
  protected int getViewId() {
    return R.layout.fragment_credential_detail;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return supportActionBar -> {
      setHasOptionsMenu(true);
      supportActionBar.setHomeButtonEnabled(true);
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      // configure the appbar title, FIXME for now it is hardcode as "University Degree"
      supportActionBar.setTitle(R.string.university_degree);
    };
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      Objects.requireNonNull(getActivity()).onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    viewModel.getCredential().observe(this, credential -> {
      // TODO: here the UI should be feed with the credential data
    });
    return view;
  }

  @Override
  public CredentialsViewModel getViewModel() {
    return ViewModelProviders.of(this, factory).get(CredentialsViewModel.class);
  }
}

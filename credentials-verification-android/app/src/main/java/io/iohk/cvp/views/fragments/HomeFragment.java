package io.iohk.cvp.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.Credential;
import io.iohk.cvp.views.utils.adapters.CredentialsRecyclerViewAdapter;
import io.iohk.cvp.views.utils.adapters.NewCredentialsRecyclerViewAdapter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class HomeFragment extends CvpFragment {

  @BindView(R.id.credentials_list)
  RecyclerView credentialsRecyclerView;

  @BindView(R.id.new_credentials_list)
  RecyclerView newCredentialsRecyclerView;

  private NewCredentialsRecyclerViewAdapter newCredentialsAdapter;
  private CredentialsRecyclerViewAdapter credentialsAdapter;

  @Override
  protected int getViewId() {
    return R.layout.fragment_home;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    LinearLayoutManager linearLayoutManagerCredentials = new LinearLayoutManager(getContext());

    credentialsRecyclerView.setLayoutManager(linearLayoutManagerCredentials);

    //TODO use view model and make grpc call to fetch real connections info
    List<Credential> lista = Arrays
      .asList(Credential.getDefaultInstance(), Credential.getDefaultInstance(),
        Credential.getDefaultInstance(), Credential.getDefaultInstance());

    credentialsAdapter = new CredentialsRecyclerViewAdapter();
    credentialsAdapter.setCredentials(lista);
    credentialsRecyclerView.setAdapter(credentialsAdapter);

    LinearLayoutManager linearLayoutManagerNewCredentials = new LinearLayoutManager(getContext());

    newCredentialsRecyclerView.setLayoutManager(linearLayoutManagerNewCredentials);

    List<Credential> newCredentials = Arrays
      .asList(Credential.getDefaultInstance(), Credential.getDefaultInstance(),
        Credential.getDefaultInstance(), Credential.getDefaultInstance());

    newCredentialsAdapter = new NewCredentialsRecyclerViewAdapter();
    newCredentialsAdapter.setCredentials(newCredentials);
    newCredentialsRecyclerView.setAdapter(newCredentialsAdapter);
    return view;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }
}

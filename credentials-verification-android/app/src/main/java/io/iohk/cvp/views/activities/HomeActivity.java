package io.iohk.cvp.views.activities;

import android.os.Bundle;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import io.iohk.cvp.R;
import io.iohk.cvp.io.connector.Credential;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.utils.adapters.CredentialsRecyclerViewAdapter;
import io.iohk.cvp.views.utils.adapters.NewCredentialsRecyclerViewAdapter;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

public class HomeActivity extends CvpActivity {

  @Inject
  Navigator navigator;

  @BindView(R.id.credentials_list)
  RecyclerView credentialsRecyclerView;

  @BindView(R.id.new_credentials_list)
  RecyclerView newCredentialsRecyclerView;

  private NewCredentialsRecyclerViewAdapter newCredentialsAdapter;
  private CredentialsRecyclerViewAdapter credentialsAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinearLayoutManager linearLayoutManagerCredentials = new LinearLayoutManager(this);

    credentialsRecyclerView.setLayoutManager(linearLayoutManagerCredentials);

    //TODO use view model and make grpc call to fetch real connections info
    List<Credential> lista = Arrays
        .asList(Credential.getDefaultInstance(), Credential.getDefaultInstance(),
            Credential.getDefaultInstance(), Credential.getDefaultInstance());

    credentialsAdapter = new CredentialsRecyclerViewAdapter();
    credentialsAdapter.setCredentials(lista);
    credentialsRecyclerView.setAdapter(credentialsAdapter);

    LinearLayoutManager linearLayoutManagerNewCredentials = new LinearLayoutManager(this);

    newCredentialsRecyclerView.setLayoutManager(linearLayoutManagerNewCredentials);

    List<Credential> newCredentials = Arrays
        .asList(Credential.getDefaultInstance(), Credential.getDefaultInstance(),
            Credential.getDefaultInstance(), Credential.getDefaultInstance());

    newCredentialsAdapter = new NewCredentialsRecyclerViewAdapter();
    newCredentialsAdapter.setCredentials(newCredentials);
    newCredentialsRecyclerView.setAdapter(newCredentialsAdapter);

  }


  @Override
  protected Navigator getNavigator() {
    return navigator;
  }

  @Override
  protected int getView() {
    return R.layout.home_activity;
  }

  @Override
  protected int getTitleValue() {
    return R.string.home_title;
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }
}

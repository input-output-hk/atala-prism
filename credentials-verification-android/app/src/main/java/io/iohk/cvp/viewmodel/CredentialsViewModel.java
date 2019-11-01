package io.iohk.cvp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import io.iohk.cvp.io.connector.Credential;

public class CredentialsViewModel extends ViewModel {

  private final MutableLiveData<List<Credential>> credentials = new MutableLiveData<>();
  private final MutableLiveData<Credential> credential = new MutableLiveData<>();

  @Inject
  public CredentialsViewModel() {
    //TODO use view model and make grpc call to fetch real connections info
    List<Credential> list = Arrays
        .asList(Credential.getDefaultInstance(), Credential.getDefaultInstance(),
            Credential.getDefaultInstance(), Credential.getDefaultInstance());

    credentials.setValue(list);
    credential.setValue(Credential.getDefaultInstance());
  }

  public LiveData<List<Credential>> getCredentials() {
    return credentials;
  }

  public LiveData<Credential> getCredential() {
    return credential;
  }
}

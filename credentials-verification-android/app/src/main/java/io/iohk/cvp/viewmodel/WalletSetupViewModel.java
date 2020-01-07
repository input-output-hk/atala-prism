package io.iohk.cvp.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import javax.inject.Inject;

public class WalletSetupViewModel extends ViewModel {

  private MutableLiveData<List<String>> seedPhrase = new MutableLiveData<>();

  @Inject
  public WalletSetupViewModel() {

  }

  
}

package io.iohk.cvp.views.activities;

import android.content.Context;
import android.os.Bundle;
import androidx.lifecycle.ViewModel;
import butterknife.ButterKnife;
import io.iohk.cvp.views.Navigator;
import dagger.android.support.DaggerAppCompatActivity;

public abstract class CvpActivity<T extends ViewModel> extends DaggerAppCompatActivity {

  private T viewModel;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(this.getView());
    if (findViewById(this.getView()) != null) {
      if (savedInstanceState != null) {
        return;
      }
    }
    ButterKnife.bind(this);
    this.viewModel = viewModel == null ? getViewModel() : viewModel;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(this.getTitleValue());
    }
  }

  @Override
  public Context getApplicationContext() {
    return this;
  }

  public CvpActivity getActivity() {
    return this;
  }

  protected abstract Navigator getNavigator();

  protected abstract int getView();

  protected abstract int getTitleValue();

  public abstract T getViewModel();

}

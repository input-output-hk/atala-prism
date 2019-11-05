package io.iohk.cvp.views.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.lifecycle.ViewModel;
import butterknife.ButterKnife;
import dagger.android.support.DaggerAppCompatActivity;
import io.iohk.cvp.R;
import io.iohk.cvp.views.Navigator;

public abstract class CvpActivity<T extends ViewModel> extends DaggerAppCompatActivity {

  protected T viewModel;

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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  protected abstract Navigator getNavigator();

  protected abstract int getView();

  protected abstract int getTitleValue();

  public abstract T getViewModel();

}

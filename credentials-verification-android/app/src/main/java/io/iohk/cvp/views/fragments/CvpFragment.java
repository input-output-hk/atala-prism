package io.iohk.cvp.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;

public abstract class CvpFragment<T extends ViewModel> extends DaggerFragment {

  T viewModel;

  @Getter
  @Inject
  Navigator navigator;

  public abstract T getViewModel();

  protected abstract AppBarConfigurator getAppBarConfigurator();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    android.view.View view = inflater.inflate(getViewId(), container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    viewModel = getViewModel();
  }

  @Override
  public void onResume() {
    super.onResume();
    Optional<ActionBar> supportActionBar = activity().map(AppCompatActivity::getSupportActionBar);
    supportActionBar.ifPresent(actionBar -> {
      if (getAppBarConfigurator() != null) {
        getAppBarConfigurator().configureActionBar(actionBar);
      }
    });
  }

  public Optional<AppCompatActivity> activity() {
    return Optional.ofNullable(getActivity())
        .map(fragmentActivity -> (AppCompatActivity) fragmentActivity);
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  protected abstract int getViewId();

  protected Set<String> getUserIds() {
    Preferences prefs = new Preferences(getContext());
    return prefs.getUserIds();
  }

}

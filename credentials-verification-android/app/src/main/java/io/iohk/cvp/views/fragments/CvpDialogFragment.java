package io.iohk.cvp.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import butterknife.ButterKnife;
import dagger.android.support.DaggerDialogFragment;
import io.iohk.cvp.views.Navigator;
import javax.inject.Inject;
import lombok.Getter;

public abstract class CvpDialogFragment<T extends ViewModel> extends DaggerDialogFragment {

  T viewModel;

  @Getter
  @Inject
  Navigator navigator;

  public abstract T getViewModel();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(getViewId(), container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    viewModel = getViewModel();
  }

  protected abstract int getViewId();

}

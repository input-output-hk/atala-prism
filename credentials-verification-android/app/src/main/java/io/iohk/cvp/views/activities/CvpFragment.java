package io.iohk.cvp.views.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import butterknife.ButterKnife;

public abstract class CvpFragment<T extends ViewModel> extends Fragment {

  private T viewModel;

  public abstract T getViewModel();

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
  public void onDetach() {
    super.onDetach();
  }

  protected abstract int getViewId();
}

package io.iohk.cvp.views.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.Objects;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.utils.AssetsUtils;
import io.iohk.cvp.utils.DateUtils;
import io.iohk.cvp.views.fragments.utils.AppBarConfigurator;
import io.iohk.cvp.views.fragments.utils.NoAppBar;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LargeDescriptionDialogFragment extends CvpFragment {

  @BindView(R.id.text_view_title)
  public TextView textViewTitle;

  @BindView(R.id.text_view_subtitle)
  public TextView textViewSubTitle;

  @BindView(R.id.text_view_full_description)
  public TextView textViewDescription;

  @BindView(R.id.fab)
  public FloatingActionButton floatingActionButtonClose;

  @BindColor(R.color.colorPrimary)
  ColorStateList colorRed;

  private String title;
  private Calendar lastUpdated;
  private int assetResourceId;

  public LargeDescriptionDialogFragment(String title, Calendar lastUpdated, int assetResourceId) {
    this.title = title;
    this.lastUpdated = lastUpdated;
    this.assetResourceId = assetResourceId;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    textViewTitle.setText(title);
    textViewSubTitle.setText(new DateUtils(getContext()).format(lastUpdated));
    floatingActionButtonClose.setBackgroundTintList(colorRed);

    new AssetsUtils(getContext())
      .getTextFromAsset(assetResourceId)
      .ifPresent(text ->
        textViewDescription.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT))
      );
    textViewDescription.setMovementMethod(new ScrollingMovementMethod());
    return view;
  }

  @OnClick(R.id.fab)
  void onClose() {
    Objects.requireNonNull(getActivity()).onBackPressed();
  }


  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  protected AppBarConfigurator getAppBarConfigurator() {
    return new NoAppBar();
  }

  @Override
  protected int getViewId() {
    return R.layout.component_large_content_dialog;
  }
}
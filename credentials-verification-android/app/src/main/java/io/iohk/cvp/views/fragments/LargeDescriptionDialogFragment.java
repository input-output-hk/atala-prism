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

import com.crashlytics.android.Crashlytics;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Objects;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.OnClick;
import io.iohk.cvp.R;
import io.iohk.cvp.core.exception.AssetNotFoundException;
import io.iohk.cvp.core.exception.CaseNotFoundException;
import io.iohk.cvp.core.exception.ErrorCode;
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
  private String assetName;

  public LargeDescriptionDialogFragment(String title, Calendar lastUpdated, String assetName) {
    this.title = title;
    this.lastUpdated = lastUpdated;
    this.assetName = assetName;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    textViewTitle.setText(title);
    java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
    textViewSubTitle.setText(dateFormat.format(lastUpdated.getTime()));
    floatingActionButtonClose.setBackgroundTintList(colorRed);
    textViewDescription.setText(Html.fromHtml(getTextFromAsset(assetName), Html.FROM_HTML_MODE_COMPACT));
    textViewDescription.setMovementMethod(new ScrollingMovementMethod());
    return view;
  }

  @OnClick(R.id.fab)
  public void onClose() {
    Objects.requireNonNull(getActivity()).onBackPressed();
  }


  private String getTextFromAsset(final String fileName) {
    try {
      InputStream stream = Objects.requireNonNull(getContext()).getAssets().open(fileName);
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int nRead;
      byte[] data = new byte[1024];
      while ((nRead = stream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();
      byte[] byteArray = buffer.toByteArray();

      return new String(byteArray, StandardCharsets.UTF_8);
    } catch (IOException e) {
      Crashlytics.logException(
        new AssetNotFoundException("Couldn't find the asset with the name " + fileName + ", exception:" + e.getMessage(),
          ErrorCode.ASSET_NOT_FOUND));
      e.printStackTrace();
      return "";
    }
  }

  @Override
  public ViewModel getViewModel() {
    return null;
  }

  @Override
  protected int getViewId() {
    return R.layout.component_large_content_dialog;
  }
}
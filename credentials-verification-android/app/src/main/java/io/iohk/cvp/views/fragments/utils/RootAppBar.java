package io.iohk.cvp.views.fragments.utils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.ActionBar;
import lombok.AllArgsConstructor;

public class RootAppBar extends VisibleAppBar {

  public RootAppBar(int titleId, int textColor) {
    super(titleId, textColor);
  }

  public RootAppBar(int titleId) {
    super(titleId);
  }

  @Override
  public void configureActionBar(ActionBar supportActionBar) {
    super.configureActionBar(supportActionBar);
    supportActionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    supportActionBar.setHomeButtonEnabled(false);
    supportActionBar.setDisplayHomeAsUpEnabled(false);
  }
}

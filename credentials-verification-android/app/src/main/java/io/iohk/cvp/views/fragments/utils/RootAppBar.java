package io.iohk.cvp.views.fragments.utils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.ActionBar;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RootAppBar implements AppBarConfigurator {

  private final int titleId;

  @Override
  public void configureActionBar(ActionBar supportActionBar) {
    supportActionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    supportActionBar.setHomeButtonEnabled(false);
    supportActionBar.setDisplayHomeAsUpEnabled(false);
    supportActionBar.setTitle(titleId);
  }
}

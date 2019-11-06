package io.iohk.cvp.views.fragments.utils;

import androidx.appcompat.app.ActionBar;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RootAppBar implements AppBarConfigurator {

  private final int titleId;

  @Override
  public void configureActionBar(ActionBar supportActionBar) {
    supportActionBar.setHomeButtonEnabled(false);
    supportActionBar.setDisplayHomeAsUpEnabled(false);
    supportActionBar.setTitle(titleId);
  }
}

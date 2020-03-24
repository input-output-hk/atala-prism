package io.iohk.cvp.views.fragments.utils;

import androidx.appcompat.app.ActionBar;

public class NoAppBar implements AppBarConfigurator {
  @Override
  public void configureActionBar(ActionBar supportActionBar) {
    supportActionBar.hide();
  }
}

package io.iohk.atala.prism.app.ui.utils;

import androidx.appcompat.app.ActionBar;

public class StackedAppBar extends VisibleAppBar {

    public StackedAppBar(int titleId) {
        super(titleId);
    }

    @Override
    public void configureActionBar(ActionBar supportActionBar) {
        super.configureActionBar(supportActionBar);
        supportActionBar.setHomeButtonEnabled(true);
        supportActionBar.setDisplayHomeAsUpEnabled(true);
    }
}

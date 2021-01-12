package io.iohk.atala.prism.app.views.fragments.utils;

import android.graphics.Color;

import androidx.appcompat.app.ActionBar;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class VisibleAppBar implements AppBarConfigurator {

    private final int titleId;
    private int textColor = Color.BLACK;

    VisibleAppBar(int titleId) {
        this.titleId = titleId;
    }

    @Override
    public void configureActionBar(ActionBar supportActionBar) {
        supportActionBar.show();
        supportActionBar.setTitle(titleId);
        ActionBarUtils.setTextColor(supportActionBar, textColor);
    }
}

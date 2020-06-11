package io.iohk.cvp.views.utils;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import io.iohk.cvp.core.CvpApplication;
import io.iohk.cvp.views.Navigator;
import io.iohk.cvp.views.Preferences;

public class ForegroundBackgroundListener implements LifecycleObserver {

    private final Context ctx;
    private final Preferences prefs;

    Navigator navigator;


    public ForegroundBackgroundListener(Context ctx) {
        this.ctx = ctx;
        prefs = new Preferences(ctx);
        navigator = new Navigator();
    }



    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onStart() {
        if(!prefs.isFirstLaunch() && prefs.isPinConfigured() ){
            navigator.showUnlockScreen(ctx);
        } else {
           prefs.setIsFirstLaunch(false);
        }
    }
}

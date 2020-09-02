package io.iohk.cvp.views.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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

    /*
     * TODO: Refactor "isFirstLaunch" for now this property is used to handle the
     *  security pin view, we need to find a better way to handle that
     * */
    public static Boolean isFirstLaunch = false;


    public ForegroundBackgroundListener(Context ctx) {
        this.ctx = ctx;
        prefs = new Preferences(ctx);
        navigator = new Navigator();
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onStart() {
        if (!ForegroundBackgroundListener.isFirstLaunch && prefs.isPinConfigured()) {
            navigator.showUnlockScreen(ctx);
        } else {
            ForegroundBackgroundListener.isFirstLaunch = false;
        }
    }
}
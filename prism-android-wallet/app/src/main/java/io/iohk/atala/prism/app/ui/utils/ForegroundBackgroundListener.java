package io.iohk.atala.prism.app.ui.utils;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import io.iohk.atala.prism.app.neo.common.IntentUtils;
import io.iohk.atala.prism.app.data.local.preferences.Preferences;

public class ForegroundBackgroundListener implements LifecycleObserver {

    private final Context ctx;
    private final Preferences prefs;

    /*
     * TODO: Refactor "isFirstLaunch" for now this property is used to handle the
     *  security pin view, we need to find a better way to handle that
     * */
    public static Boolean isFirstLaunch = false;


    public ForegroundBackgroundListener(Context ctx) {
        this.ctx = ctx;
        prefs = new Preferences(ctx);
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onStart() {
        if (!ForegroundBackgroundListener.isFirstLaunch && prefs.isPinConfigured()) {
            ctx.startActivity(IntentUtils.Companion.intentUnlockScreen(ctx));
        } else {
            ForegroundBackgroundListener.isFirstLaunch = false;
        }
    }
}
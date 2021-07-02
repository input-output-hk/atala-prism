package io.iohk.atala.prism.app.ui.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import io.iohk.atala.prism.app.core.ConnectorListenerService;
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
        // Bind with ConnectorListenerService
        Intent intent = new Intent(ctx, ConnectorListenerService.class);
        ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if (!ForegroundBackgroundListener.isFirstLaunch && prefs.isPinConfigured()) {
            ctx.startActivity(IntentUtils.Companion.intentUnlockScreen(ctx));
        } else {
            ForegroundBackgroundListener.isFirstLaunch = false;
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onStop(){
        // Unbind ConnectorListenerService
        ctx.unbindService(connection);
    }

    /**
     * It is used to bind the application to the ConnectorListenerService, in "onServiceConnected"
     * you can get the ConnectorListenerService instance if this is required
     * */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {}
        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };
}
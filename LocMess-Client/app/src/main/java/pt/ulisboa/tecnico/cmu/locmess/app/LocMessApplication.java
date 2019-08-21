package pt.ulisboa.tecnico.cmu.locmess.app;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;


public class LocMessApplication extends Application {
    private static LocMessApplication mInstance;

    public boolean backgroundServiceBounded;
    public LocMessBackgroundService locMessBackgroundService;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        Log.d("+++APP+++", "OnCreate APP");

    }

    public static LocMessApplication getInstance() { return mInstance; }

    ServiceConnection locMessBackgroundServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("+++APP+++", "onServiceConnected");

            backgroundServiceBounded = true;
            LocMessBackgroundService.LocalBinder localBinder = (LocMessBackgroundService.LocalBinder)service;
            locMessBackgroundService = localBinder.getLocMessBackgroundServiceInstance();

            // Run background service
            Intent intent = new Intent(getApplicationContext(), LocMessBackgroundService.class);
            locMessBackgroundService.startService(intent);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("+++APP+++", "onServiceDisconnected");
            backgroundServiceBounded = false;
            locMessBackgroundService = null;
        }
    };

    public void enableLocMessBackgroundService(){
        Log.d("+++APP+++", "enableLocMessBackgroundService");
        Intent mIntent = new Intent(getApplicationContext(), LocMessBackgroundService.class);
        bindService(mIntent, locMessBackgroundServiceConnection, BIND_AUTO_CREATE);
    }

    public void disableLocMessBackgroundService(){
        Log.d("+++APP+++", "disableLocMessBackgroundService");
        if(backgroundServiceBounded){
            Intent intent = new Intent(getApplicationContext(), LocMessBackgroundService.class);
            locMessBackgroundService.stopService(intent);
            unbindService(locMessBackgroundServiceConnection);
            locMessBackgroundService=null;
            backgroundServiceBounded = false;
        }
    }




}

package com.tim.wifibook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Tim Sandberg on 3/2/14.
 */
public class BootupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsFragment.MyPREFERENCES,Context.MODE_PRIVATE);
        boolean startup_pref = prefs.getBoolean(SettingsFragment.LAUNCH_ON_STARTUP, false);
        Log.d(TAG,"Startup intent received (" + String.valueOf(startup_pref) +")");
        if(startup_pref){
            Intent i = new Intent(context, WifiService.class);
            context.startService(i);
        }

    }
}

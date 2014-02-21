package com.tim.wifibook;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

/**
 * Created by Tim Sandberg on 1/16/14.
 */
public class NetworkManager {
    private static final String TAG = "NetworkManager";
    public List<WifiConfiguration> mNetworks;
    private static NetworkManager sNetworkManager;
    private Context mAppContext;
    private boolean isServiceOn;
    private WifiManager mWifiManager;

    private NetworkManager(Context appContext){
        mWifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
        mAppContext = appContext;
        mNetworks = mWifiManager.getConfiguredNetworks();
        isServiceOn = false;
    }

    public static NetworkManager get(Context c) {
        if(sNetworkManager == null){
            sNetworkManager = new NetworkManager(c.getApplicationContext());
        }
        return sNetworkManager;
    }

    public List<WifiConfiguration> getNetworks() {
        return mNetworks;
    }

    public WifiConfiguration getNetwork(String SSID){
        for(WifiConfiguration wc: mNetworks){
            if(wc.SSID.equals(SSID))
                return wc;
        }

        Log.d(TAG,"Network not found");
        return null;
    }
}

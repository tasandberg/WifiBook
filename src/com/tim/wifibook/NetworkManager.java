package com.tim.wifibook;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Tim Sandberg on 1/16/14.
 */
public class NetworkManager {
    private static final String TAG = "NetworkManager";
    public ArrayList<Network> mNetworks;
    private static NetworkManager sNetworkManager;
    private Context mAppContext;
    private boolean isServiceOn;

    private NetworkManager(Context appContext){
        mAppContext = appContext;
        mNetworks = new ArrayList<Network>();
        isServiceOn = false;
    }

    public static NetworkManager get(Context c) {
        if(sNetworkManager == null){
            sNetworkManager = new NetworkManager(c.getApplicationContext());
        }
        return sNetworkManager;
    }

    public ArrayList<Network> getNetworks() {
        return mNetworks;
    }

    public Network getNetwork(String name){
        for(Network n: mNetworks){
            if(n.getName().equals(name))
                return n;
        }
        Log.d(TAG,"Network not found");
        return null;
    }
}

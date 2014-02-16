package com.tim.wifibook;

/*
Testing Git Commit
 */

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim Sandberg on 1/15/14.
 */

public class WifiService extends Service {
    final static int notificationID = 7278;
    public static boolean isRunning;
    private static final String TAG = "WifiService";
    private WifiManager mWifiManager;
    private List<ScanResult> result_list;
    private ArrayList<Network> mNetworks;
    private int count = 1;
    private Handler handler;
    private static final int CHECK_INTERVAL = 1000 * 10; //2 minutes

    @Override
    public void onCreate(){
        Log.d(TAG, "onCreate() called in Service");
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        readWepConfig();
        //registerReceiver(mBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        startNotification(getApplicationContext());
        mNetworks = NetworkManager.get(getApplicationContext())
                .getNetworks();
        if(!mWifiManager.isWifiEnabled()){
            Log.d(TAG,"Enabling Wifi for scan");
            mWifiManager.setWifiEnabled(true);
        }

        handler = new Handler();
        handler.post(scanNetworks);

        return Service.START_STICKY;
    }

    private Runnable scanNetworks = new Runnable(){
        @Override
        public void run() {
            registerReceiver(mBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mWifiManager.startScan();
            handler.postDelayed(this,CHECK_INTERVAL);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        handler.removeCallbacks(scanNetworks);
        isRunning = false;
        Log.d(TAG,"onDestroy: unregistering receiver..");
        unregisterReceiver(mBroadcastReceiver);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }




    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received broadcast " + count);
            result_list = mWifiManager.getScanResults();
            readWepConfig();
            count++;
            boolean networksInRange = knownNetworksInRange(result_list, mNetworks);
            if(networksInRange){
                return;
            } else if(!networksInRange) {
                mWifiManager.setWifiEnabled(false);
            }

            unregisterReceiver(this);

        }

    };


    public static boolean isServiceAlarmOn(Context context) {
        Intent i = new Intent(context,WifiService.class);
        PendingIntent pi = PendingIntent.getService(context, 0,i,PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }


    //GATHER LIST OF ALL CONFIGURED NETWORKS
    void readWepConfig() {
        List<WifiConfiguration> items = mWifiManager.getConfiguredNetworks();
        NetworkManager.get(getApplicationContext()).getNetworks().clear();
        try{
            for(int i = 0;i<items.size();i++){
                WifiConfiguration config = items.get(i);
                Network n = new Network(config.SSID.replace("\"",""));
                NetworkManager.get(getApplicationContext()).getNetworks().add(n);
            }
            Log.d(TAG,"Networks refreshed");
            ArrayList<Network> netList = NetworkManager.get(getApplicationContext()).getNetworks();
            for(int i = 0;i<netList.size();i++){
                Log.d(TAG, "Network " + i + ": " + netList.get(i).getName());
            }
        } catch(Exception e){
            Log.e(TAG,"Exception " + e);
        }
    }


    public boolean knownNetworksInRange(List<ScanResult> results, ArrayList<Network> myNetworks){
        boolean preferredNetworkFound = false;
        ArrayList<Network> preferredNetworks = new ArrayList<Network>();
        for(Network n: myNetworks){
            if(n.isActive()) {
                preferredNetworks.add(n);
            }
        }
        for(ScanResult r : results){
            String SSID = r.SSID;
            Log.d(TAG, "Checking for found network: " + r.SSID);
            for(int i = 0; i < preferredNetworks.size(); i++){
                if(SSID.equals(preferredNetworks.get(i).getName())){
                    Log.d(TAG,"Configured network found (" + preferredNetworks.get(i) + ")");
                    preferredNetworkFound = true;
                    return preferredNetworkFound;
                }
            }
        }
        Log.d(TAG,"No preferred networks found, disabling wifi");
        return preferredNetworkFound;
    }

    // DEFINE AND START NOTIFICATION
    public void startNotification(Context context) {
        Notification notification;
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);


        if((Build.VERSION.SDK_INT) >= Build.VERSION_CODES.DONUT) {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setTicker("WifiBook").setContentTitle("TITLE").setContentText("CONTENT")
                    .setWhen(System.currentTimeMillis()).setAutoCancel(false)
                    .setOngoing(true).setPriority(Notification.PRIORITY_HIGH)
                    .setContentIntent(pendIntent);
            notification = builder.build();

        } else {
            notification = new Notification(R.drawable.elephi_icon, "WifiBook",
                    System.currentTimeMillis());
            notification.setLatestEventInfo(context, "Wifibook?!","WifiBook on", pendIntent);
        }

        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(notificationID, notification);

    }



}



/*** GRAVEYARD ***/


/* REMOVED TO CONVERT FROM ALARMMANAGER TO HANDLER USE
    IN ORDER TO CLEAN UP START/STOP OF SERVICE

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i = new Intent(context, WifiService.class);
        PendingIntent pi = PendingIntent.getService(context,0,i,0);

        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        if(isOn){
            alarmManager.setRepeating(AlarmManager.RTC,
                    System.currentTimeMillis(), CHECK_INTERVAL, pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }
*/

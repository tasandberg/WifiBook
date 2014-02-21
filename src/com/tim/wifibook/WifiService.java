package com.tim.wifibook;

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
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

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
    private List<WifiConfiguration> mNetworks;
    private int count = 1;
    private Handler handler;
    private static final int CHECK_INTERVAL = 1000 * 10; //1 min

    @Override
    public void onCreate(){
        Log.d(TAG, "onCreate() called in Service");
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mNetworks = mWifiManager.getConfiguredNetworks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        startNotification(getApplicationContext());
        handler = new Handler();
        handler.post(scanNetworks);
        return Service.START_STICKY;
    }

    private Runnable scanNetworks = new Runnable(){
        @Override
        public void run() {
            if(!mWifiManager.isWifiEnabled()){
                Log.d(TAG,"Enabling Wifi for scan");
                mWifiManager.setWifiEnabled(true);
            }
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
        try {
            unregisterReceiver(mBroadcastReceiver);
        } catch (Exception e) {
            Log.d(TAG, "Receiver already unregistered");
        }

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
            count++;
            mNetworks = mWifiManager.getConfiguredNetworks();
            boolean networksInRange = knownNetworksInRange(result_list, mNetworks);
            if(networksInRange){
                return;
            } else if(!networksInRange) {
                mWifiManager.setWifiEnabled(false);
            }


        }

    };


    public static boolean isServiceAlarmOn(Context context) {
        Intent i = new Intent(context,WifiService.class);
        PendingIntent pi = PendingIntent.getService(context, 0,i,PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    /*
    //GATHER LIST OF ALL CONFIGURED NETWORKS
    List<WifiConfiguration> readWepConfig() {
        List<WifiConfiguration> items = mWifiManager.getConfiguredNetworks();
        Log.d(TAG,"Networks refreshed: ");
        if(items.isEmpty()) {
            Log.d(TAG,"List Empty");
            return items;
        }

        for(int i = 0;i<items.size();i++){
            Log.d(TAG, "Network " + i + ": " + items.get(i).SSID);
        }

        return items;

    }


        catch(Exception e){

            Toast.makeText(getApplicationContext(), "Error gathering configured networks", Toast.LENGTH_SHORT).show();
            Log.e(TAG,"Networks did not refresh (" + e + ")");
        }
        */



    public boolean knownNetworksInRange(List<ScanResult> results, List<WifiConfiguration> myNetworks){
        boolean preferredNetworkFound = false;
        Log.d(TAG,"Saved networks:");
        for (int i = 0; i < myNetworks.size()-1; i++) {
            Log.d(TAG,myNetworks.get(i).SSID);
        }

        for(ScanResult r : results){
            String SSID = r.SSID;
            Log.d(TAG, "Checking for found network: " + r.SSID);
            for(int i = 0; i < myNetworks.size(); i++){
                if(SSID.equals(myNetworks.get(i).SSID.replace("\"", ""))){
                    Log.d(TAG,"Configured network found (" +myNetworks.get(i).SSID.replace("\"","") + ")");
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

        PendingIntent pendIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        String status = isRunning ? "On" : "Off";
        Notification notification = new NotificationCompat.Builder(this)
                .setTicker("WifiBook is managing your wifi connection")
                .setSmallIcon(R.drawable.elephi_clear)
                .setContentTitle("WifiBook")
                .setContentText("Wifi Management On")
                .setContentIntent(pendIntent)
                .setAutoCancel(false)
                .build();

        /*
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
        */
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

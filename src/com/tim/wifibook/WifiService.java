package com.tim.wifibook;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Tim Sandberg on 1/15/14.
 */

public class WifiService extends Service {
    final static int notificationID = 7278;
    public static boolean isRunning;
    public static boolean inRange;
    public static String current_network;
    private static final String TAG = "WifiService";
    private WifiManager mWifiManager;
    private List<ScanResult> result_list;
    private List<WifiConfiguration> mNetworks;
    private int count = 1;
    private Handler handler;
    private static int CHECK_INTERVAL;
    SharedPreferences prefs;

    @Override
    public void onCreate(){
        Log.d(TAG, "onCreate() called in Service");
        prefs = getSharedPreferences(SettingsFragment.MyPREFERENCES, Context.MODE_PRIVATE);

        Log.d(TAG,"ScanInterval: " + String.valueOf(CHECK_INTERVAL));
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mNetworks = mWifiManager.getConfiguredNetworks();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        if(inRange) current_network = getCurrentNetworkName();
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
            int interval = prefs.getInt(SettingsFragment.SCAN_INTERVAL, 50000);
            if(interval != CHECK_INTERVAL) {
                CHECK_INTERVAL = interval;
                Log.d(TAG,"Interval updated (" + CHECK_INTERVAL + ")");
            }
            handler.postDelayed(this,CHECK_INTERVAL);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        handler.removeCallbacks(scanNetworks);
        inRange = false;
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
            mWifiManager.setWifiEnabled(true);
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

    public void connectionToast(boolean connectionStatus, String newNetwork) {
        String message = connectionStatus ?
                "Left WifiRange, Radio Off" : "Connecting to " + newNetwork;

        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.elephi_toast, 0, 0, 0);
        /*
        Drawable bg = getResources().getDrawable(R.drawable.toast_light_bg);
        if(Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            tv.setBackgroundDrawable(bg);
        } else {
            tv.setBackground(bg);
        }
        */
        Toast toast = new Toast(getApplicationContext());
        DisplayMetrics dm = getResources().getDisplayMetrics();
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, (int) (dm.heightPixels * .75) );
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(tv);
        toast.show();
    }

    public boolean knownNetworksInRange(List<ScanResult> results, List<WifiConfiguration> myNetworks){
        boolean preferredNetworkFound = false;
        Log.d(TAG,"Saved networks:");
        for (int i = 0; i < myNetworks.size()-1; i++) {
            Log.d(TAG,myNetworks.get(i).SSID);
        }

        for (ScanResult r : results){
            String SSID = r.SSID;
            Log.d(TAG, "Checking for found network: " + r.SSID);
            for (int i = 0; i < myNetworks.size(); i++){
                if (SSID.equals(myNetworks.get(i).SSID.replace("\"", ""))){
                    String newNetwork = myNetworks.get(i).SSID.replace("\"","");
                    if (!inRange) {
                        inRange = true;
                        current_network = newNetwork;
                        makeImageToast(getApplicationContext(),
                                newNetwork + " in range, connecting",
                                Toast.LENGTH_LONG).show();
                    }
                    Log.d(TAG,"Configured network found (" +newNetwork + ")");
                    preferredNetworkFound = true;
                    return preferredNetworkFound;
                }
            }
        }
        Log.d(TAG,"No preferred networks found, disabling wifi");
        if(inRange) {
            inRange = false;
            makeImageToast(getApplicationContext(),
                    "Wifi out of range, disabling",
                    Toast.LENGTH_LONG).show();
        }
        return preferredNetworkFound;
    }

    // DEFINE AND START NOTIFICATION
    public void startNotification(Context context) {

        PendingIntent pendIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        String s = "Wifi management on";
        if(!getCurrentNetworkName().equals("0x")) s += " (" + getCurrentNetworkName().replace("\"","") + ")";
        if(inRange) s += " ("+mWifiManager.getConnectionInfo().getSSID() +")";
            Notification notification = new NotificationCompat.Builder(this)
                .setTicker("WifiBook is managing your wifi connection")
                .setSmallIcon(R.drawable.elephi_toast)
                .setContentTitle("WifiBook")
                .setContentText(s)
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

    public static Toast makeImageToast(Context context, CharSequence text, int length) {
        Toast toast = Toast.makeText(context, text, length);

        View rootView = toast.getView();
        LinearLayout linearLayout = null;
        View messageTextView = null;

        // check (expected) toast layout
        if (rootView instanceof LinearLayout) {
            linearLayout = (LinearLayout) rootView;

            if (linearLayout.getChildCount() == 1) {
                View child = linearLayout.getChildAt(0);

                if (child instanceof TextView) {
                    messageTextView = (TextView) child;
                }
            }
        }

        // cancel modification because toast layout is not what we expected
        if (linearLayout == null || messageTextView == null) {
            return toast;
        }

        ViewGroup.LayoutParams textParams = messageTextView.getLayoutParams();
        ((LinearLayout.LayoutParams) textParams).gravity = Gravity.CENTER_VERTICAL;

        // convert dip dimension
        float density = context.getResources().getDisplayMetrics().density;
        int imageSize = (int) (density * 25 + 0.5f);
        int imageMargin = (int) (density * 15 + 0.5f);

        // setup image view layout parameters
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
        imageParams.setMargins(0, 0, imageMargin, 0);
        imageParams.gravity = Gravity.CENTER_VERTICAL;

        // setup image view
        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.elephi_toast);
        imageView.setLayoutParams(imageParams);

        // modify root layout
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.addView(imageView, 0);

        return toast;
    }

    public String getCurrentNetworkName(){
        WifiInfo wInfo = mWifiManager.getConnectionInfo();
        String s = wInfo.getSSID();
        return s;
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

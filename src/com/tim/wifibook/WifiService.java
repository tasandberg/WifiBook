package com.tim.wifibook;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.content.res.Configuration;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
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
        super.onCreate();
        Log.d(TAG, "onCreate() called in Service");
        prefs = getSharedPreferences(SettingsFragment.MyPREFERENCES, Context.MODE_PRIVATE);
        Log.d(TAG,"ScanInterval: " + String.valueOf(CHECK_INTERVAL));
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mNetworks = mWifiManager.getConfiguredNetworks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
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
            if(!isRunning) {
                getApplicationContext().stopService(new Intent(getApplicationContext(),WifiService.class));
                stopForeground(true);
            }
            else {
                registerReceiver(mBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                mWifiManager.setWifiEnabled(true);
                mWifiManager.startScan();
                int interval = prefs.getInt(SettingsFragment.SCAN_INTERVAL, 50000);
                if(interval != CHECK_INTERVAL) {
                    CHECK_INTERVAL = interval;
                    Log.d(TAG,"Interval updated (" + CHECK_INTERVAL + ")");
                }
                handler.postDelayed(this,CHECK_INTERVAL);
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.d(TAG,"Service onDestroy()");
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
            result_list = mWifiManager.getScanResults();
            count++;
            mNetworks = mWifiManager.getConfiguredNetworks();
            inRange = knownNetworksInRange(result_list, mNetworks);
            if(inRange){
                return;
            } else if(!inRange) {
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
        if(!mWifiManager.isWifiEnabled()){
            Log.d(TAG,"WEIRD: Wifi disabled for knownNetworks call");
            mWifiManager.setWifiEnabled(true);
            return inRange;
        }
        /*
        Log.d(TAG,"Saved networks:");
        for(WifiConfiguration wc : myNetworks) {
            Log.d(TAG,wc.SSID);
        }
        */

        for (ScanResult r : results){
            String SSID = r.SSID;
            //Log.d(TAG, "Checking for found network: " + r.SSID);
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
        Notification notification = new Notification(R.drawable.icon, "WifiBook is managing your wifi " +
                "connection",
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this,"WifiBook",
                "Managing wifi connections", pendingIntent);

        startForeground(notificationID, notification);


        /*
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        String s = "Managing wifi";
        if(inRange) s += " ("+mWifiManager.getConnectionInfo().getSSID() +")";
            Notification notification = new NotificationCompat.Builder(this)
                .setTicker("WifiBook is managing your wifi connection")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.icon))
                .setSmallIcon(R.drawable.elephi_toast)
                .setContentTitle("WifiBook")
                .setContentText(s)
                .setContentIntent(pendIntent)
                .setAutoCancel(false)
                .build();

        startForeground(notificationID, notification);
        */

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
        imageView.setImageResource(R.drawable.icon);
        imageView.setLayoutParams(imageParams);

        // modify root layout
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.addView(imageView, 0);

        return toast;
    }

    public String getCurrentNetworkName(){
        if(mWifiManager.isWifiEnabled()) {
            WifiInfo wInfo = mWifiManager.getConnectionInfo();
            String s = wInfo.getSSID();
            return s;
        }
        else {
            return "";
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG,"onConfigChanged()");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG,"onLowMemory()");
        super.onLowMemory();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG,"onRebind()");
        super.onRebind(intent);
    }

}

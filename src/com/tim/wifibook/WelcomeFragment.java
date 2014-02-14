package com.tim.wifibook;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

/**
 * Created by Tim Sandberg on 1/18/14.
 */

public class WelcomeFragment extends Fragment {
    private static final String TAG = "WelcomeFragment";
    public ImageView iconView;
    public ArrayList<Network> mPrefNetworks;
    boolean shouldStartAlarm;
    Handler handler;
    WifiManager mWifiManager;
    int fragVal;

    static WelcomeFragment init(int val){
        WelcomeFragment wf = new WelcomeFragment();
        Bundle args = new Bundle();
        args.putInt("val",val);
        wf.setArguments(args);
        return wf;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        shouldStartAlarm = !WifiService.isServiceAlarmOn(getActivity());
        mPrefNetworks = NetworkManager.get(getActivity()).getNetworks();
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        fragVal = getArguments() != null ? getArguments().getInt("val") : 1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_welcome,container, false);
        final LinearLayout ll = (LinearLayout) v.findViewById(R.id.welcome_view);
        Log.d(TAG, "onCreate in WelcomeFragment");

        //***  Status TextView  and Updater ********//
        final TextView status_view = (TextView)ll.findViewById(R.id.status_textView);
        ImageView settings_button = (ImageView)v.findViewById(R.id.settingsButton);
        settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(),SettingsActivity.class);
                startActivity(i);
            }
        });

        final Runnable updateStatus = new Runnable(){
            @Override
            public void run() {
                String current_network = checkConnectionStatus(mPrefNetworks);
                if(!current_network.equals("0x") && !(current_network).equals("<unknown ssid>")) {
                    status_view.setText("Status: Connected to " + current_network);
                }  else {
                    status_view.setText("Status: No connection");
                }
                String appender = "\n";
                if(shouldStartAlarm) appender += "Wifi Management OFF";
                else appender += "Wifi Management ON";
                status_view.append(appender);
                handler.postDelayed(this,2000);
                }
            };
        handler = new Handler();
        handler.post(updateStatus);



        //****** Toggle Button  *********//
        final ToggleButton on_off = (ToggleButton) v.findViewById(R.id.onOff);
        if(!shouldStartAlarm){
            Log.d(TAG, "Toggle button.. nothing checked");
            on_off.setChecked(false);
            WifiService.setServiceAlarm(getActivity(),false);
        }
            on_off.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), WifiService.class);

                    if(!on_off.isChecked()){
                        Log.d(TAG,"Service stopped");
                        WifiService.setServiceAlarm(getActivity(), false);

                        if(getActivity().startService(intent) != null) {
                            Log.d(TAG, "Cancelling again...");
                            getActivity().stopService(intent);
                            WifiService.setServiceAlarm(getActivity(),false);

                        }
                    }
                    else{
                        Log.d(TAG,"Service started");
                        WifiService.setServiceAlarm(getActivity(), true);
                        shouldStartAlarm = false;

                    }
                    handler.post(updateStatus);
                }
            });

        if(!shouldStartAlarm) {
            on_off.setChecked(true);
        }
        return v;
    }

    public String checkConnectionStatus(ArrayList<Network> mPrefNetworks){
        WifiInfo wInfo = mWifiManager.getConnectionInfo();
        String s = wInfo.getSSID();
        return s;
    }

    @Override
    public void onResume(){
        Log.d(TAG, "On resume");
        super.onResume();
    }


    


}

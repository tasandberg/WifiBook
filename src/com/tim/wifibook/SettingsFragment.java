package com.tim.wifibook;

import android.content.*;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tim Sandberg on 2/3/14.
 */
public class SettingsFragment extends Fragment {
    ArrayList<WifiConfiguration> mNetworks;
    public static final String MyPREFERENCES = "MyPrefs";
    WifiManager mWifiManager;
    SharedPreferences prefs;
    public static ArrayList<WifiConfiguration> mScanResults;
    public boolean scanRetrieved = false;
    public static String SCAN_INTERVAL = "Scan Interval";
    private static final String TAG = "SettingsFragment";


    public static  ArrayList<WifiConfiguration> getScanResults() {
        return mScanResults;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        mWifiManager.startScan();
        prefs = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        scanRetrieved = false;
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            mNetworks = (ArrayList) mWifiManager.getConfiguredNetworks();
            if(!WifiService.isRunning) {
                mWifiManager.setWifiEnabled(false);
            }
        } else {
            mNetworks = (ArrayList) mWifiManager.getConfiguredNetworks();
        }

        for(int i = 0; i < mNetworks.size(); i++) {
            Log.d(TAG,"Network: " + mNetworks.get(i).SSID);
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Scan received");
                List<ScanResult> scanList = mWifiManager.getScanResults();
                ArrayList<WifiConfiguration> wcArray = new ArrayList<WifiConfiguration>();
                for(ScanResult r : scanList) {
                   WifiConfiguration wc = new WifiConfiguration();
                   wc.SSID = r.SSID;
                   wc.networkId =  -1;
                   wc.BSSID = r.BSSID;
                   wcArray.add(wc);
                }
                scanRetrieved = true;
                mScanResults = wcArray;
                getActivity().unregisterReceiver(this);
            }
        };
        getActivity().registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            getActivity().getActionBar().setTitle("Settings");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_settings, container, false);
        final RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.wifimode_radios);
        final TextView tv = (TextView) v.findViewById(R.id.mode_blurb);
        int checked_button = radioGroup.getCheckedRadioButtonId();
        if(WifiService.isRunning) radioGroup.check(R.id.radio_auto);
        else radioGroup.check(R.id.radio_manual);
        switch(checked_button){
            case R.id.radio_auto:
                tv.setText(R.string.auto_blurb);
                break;
            case R.id.radio_manual:
                tv.setText(R.string.manual_blurb);
                break;
            default:
                break;
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int checked_button = radioGroup.getCheckedRadioButtonId();
                Intent i = new Intent(getActivity(), WifiService.class);
                switch(checked_button){
                    case R.id.radio_auto:
                        if(!WifiService.isRunning)
                            getActivity().startService(i);
                        tv.setText(R.string.auto_blurb);
                        break;
                    case R.id.radio_manual:
                        if(WifiService.isRunning)
                            getActivity().stopService(i);
                        Log.d(TAG, "Service stopped");
                        tv.setText(R.string.manual_blurb);
                        break;
                    default:
                        break;
                }
            }
        });

        /* SEEK BAR SEGMENT */
        SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        final TextView frequencyTv = (TextView) v.findViewById(R.id.frequency_view);
        int interval = prefs.getInt(SCAN_INTERVAL, 30000);
        int progress;
        switch(interval) {
            case 10000:
                progress = 0;
                break;
            case 30000:
                progress = 33;
                break;
            case 60000:
                progress = 66;
                break;
            case 300000:
                progress = 99;
                break;
            default:
                progress = 30000;
                break;
        }
        seekBar.setProgress(progress);
        switch(progress){
            case 0:
                frequencyTv.setText("Every 10s");
                break;
            case 33:
                frequencyTv.setText("Every 30s");
                break;
            case 66:
                frequencyTv.setText("Every 1m");
                break;
            case 99:
                frequencyTv.setText("Every 5m");
                break;
            default:
                break;
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBar.playSoundEffect(android.view.SoundEffectConstants.CLICK);

                int step = 33;
                int interval = 1000;
                progress = (int) (Math.round(progress / step) * step);
                seekBar.setProgress(progress);
                switch (progress) {
                    case 0:
                        frequencyTv.setText("Every 10s");
                        interval *= 10;
                        break;
                    case 33:
                        frequencyTv.setText("Every 30s");
                        interval *= 30;
                        break;
                    case 66:
                        frequencyTv.setText("Every 1m");
                        interval *= 60;
                        break;
                    case 99:
                        frequencyTv.setText("Every 5m");
                        interval *= 300;
                        break;
                    default:
                        break;
                }
                SharedPreferences.Editor edit = prefs.edit();
                edit.putInt(SCAN_INTERVAL, interval);
                edit.commit();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Button manageButton = (Button) v.findViewById(R.id.manage_btn);
        manageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(i);
            }
        });



        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }










}






        /*

        final LinearLayout myNetworksCntr = (LinearLayout) v.findViewById(R.id.mynetworksCntr);
        final LinearLayout localNetworkCntr = (LinearLayout) v.findViewById(R.id.localNetworksCntr);

        final ListView myNetworkListView = (ListView) v.findViewById(R.id.myNetworksList);
        final ListView localNetworkListView = (ListView) v.findViewById(R.id.newNetworksList);

        myNetworkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                NetworkOptionsFragment dialog = NetworkOptionsFragment
                        .newInstance(((WifiConfiguration)parent.getItemAtPosition(position)).SSID);
                dialog.show(fm,"NetOptions");
            }
        });

        localNetworkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                NewNetworkFragment dialog =NewNetworkFragment
                        .newInstance(((WifiConfiguration)parent.getItemAtPosition(position)).SSID);
                dialog.show(fm,"NEW_NETWORK");
            }
        });


        if(scanRetrieved) {
            NetworkAdapter scanAdapter = new NetworkAdapter(mScanResults);
            localNetworkListView.setAdapter(scanAdapter);
        }


        myNetworkListView.setAdapter(new NetworkAdapter(mNetworks));


        Button mgmt_button = (Button) v.findViewById(R.id.manage_btn);
        mgmt_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myNetworksCntr.getVisibility() == View.GONE) {
                    myNetworksCntr.setVisibility(View.VISIBLE);
                    localNetworkCntr.setVisibility(View.GONE);
                } else {
                   myNetworksCntr.setVisibility(View.GONE);
                }
            }
        });

        Button newNetwork_button = (Button) v.findViewById(R.id.newNetwork_button);
        final ProgressBar spinner = (ProgressBar) v.findViewById(R.id.progressBar1);

        newNetwork_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"New Btn Clicked");
                if(localNetworkCntr.getVisibility() == View.GONE) {
                    while(!scanRetrieved) {
                        spinner.setVisibility(View.VISIBLE);
                    }
                    if(scanRetrieved) {
                        NetworkAdapter scanAdapter = new NetworkAdapter(mScanResults);
                        localNetworkListView.setAdapter(scanAdapter);
                    }
                    localNetworkCntr.setVisibility(View.VISIBLE);
                    myNetworksCntr.setVisibility(View.GONE);


                } else {
                    localNetworkCntr.setVisibility(View.GONE);
                }
            }
        });



        return v;
    }

    private class NetworkAdapter extends ArrayAdapter<WifiConfiguration> {

        public NetworkAdapter(ArrayList<WifiConfiguration> networks) {
            super(getActivity(), 0,networks);
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup Parent) {
            if(convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.network_list_item,null);
            }

            WifiConfiguration wc = getItem(pos);

            TextView networkName =
                    (TextView)convertView.findViewById(R.id.network_name_view);
            networkName.setText(wc.SSID.replace("\"",""));
            return convertView;
        }
    }
*/




package com.tim.wifibook;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Tim Sandberg on 2/20/14.
 */
public class NewNetworkFragment extends android.support.v4.app.DialogFragment {
    private static final String TAG = "NewNetworkFragment";
    public static final String EXTRA_NETWORK =
            "com.tim.wifibook.network";

    private WifiConfiguration newNetwork;
    private String newSSID;

    public static NewNetworkFragment newInstance(String SSID) {
        Bundle args = new Bundle();
        args.putString(EXTRA_NETWORK, SSID);
        NewNetworkFragment fragment = new NewNetworkFragment();
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater()
                .inflate(R.layout.fragment_newnet,null);
        newSSID= (String) getArguments().getString(EXTRA_NETWORK);

        Log.d(TAG,"New Network Dialog: " + newSSID);
        final WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        //ArrayList<WifiConfiguration> localNetworks = SettingsFragment.getScanResults();
        ArrayList<WifiConfiguration> localNetworks = (ArrayList) wifiManager.getConfiguredNetworks();
        String netProfile = "";
        for(WifiConfiguration wc : localNetworks){
            if(wc.SSID.equals("\"EDORAS\"")){
                newNetwork = wc;
            }
        }

        netProfile =
                "SSID " + newNetwork.SSID +
                "\nBSSID + " + newNetwork.BSSID +
                "\nallowedProtocols: " + newNetwork.allowedProtocols +
                "\nallowedKeyManagement: " + newNetwork.allowedKeyManagement +
                "\nstatus: " + newNetwork.status +
                "\nwepKeys: " + newNetwork.wepKeys.toString();


        TextView tv = (TextView) v.findViewById(R.id.newNetworkView);
        tv.setText(netProfile);


        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle("New Network")
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }


}

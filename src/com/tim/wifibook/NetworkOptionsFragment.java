package com.tim.wifibook;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.List;

/**
 * Created by Tim Sandberg on 2/20/14.
 */


public class NetworkOptionsFragment extends android.support.v4.app.DialogFragment {
    private static final String TAG = "NetworkOptionsFragment";
    public static final String EXTRA_NETWORK =
            "com.tim.wifibook.network";

    private WifiConfiguration network;

    public static NetworkOptionsFragment newInstance(String SSID) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_NETWORK, SSID);

        NetworkOptionsFragment fragment = new NetworkOptionsFragment();
        fragment.setArguments(args);

        return fragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater()
                .inflate(R.layout.fragment_newnet,null);
        final String network = (String) getArguments().getString(EXTRA_NETWORK);
        Log.d(TAG,"Network Dialog for: " + network);
        final WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        ImageButton delete_btn = (ImageButton) v.findViewById(R.id.deleteButton);
        delete_btn.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!wifiManager.isWifiEnabled())
                    wifiManager.setWifiEnabled(true);
                List<WifiConfiguration> myNetworks = wifiManager.getConfiguredNetworks();
                for(WifiConfiguration wc : myNetworks){
                    if(wc.SSID.equals(network)){
                        Log.d(TAG,"Deleting " + wc.SSID);
                        wifiManager.removeNetwork(wc.networkId);
                    }
                }
            }
        });
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle("Forget " + network + "?")
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }


}

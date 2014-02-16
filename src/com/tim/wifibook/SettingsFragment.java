package com.tim.wifibook;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Created by Tim Sandberg on 2/3/14.
 */
public class SettingsFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            getActivity().getActionBar().setTitle("Settings");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        final RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.wifimode_radios);
        final TextView tv = (TextView) v.findViewById(R.id.mode_blurb);
        int checked_button = radioGroup.getCheckedRadioButtonId();
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
            }
        });
        ListView lv = (ListView) v.findViewById(android.R.id.list);

        return v;
    }
}

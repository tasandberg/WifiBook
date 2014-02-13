package com.tim.wifibook;

import android.support.v4.app.Fragment;

/**
 * Created by Tim Sandberg on 2/3/14.
 */
public class SettingsActivity extends SingleFragmentActivity {


    @Override
    protected Fragment createFragment() {
        return new SettingsFragment();
    }
}

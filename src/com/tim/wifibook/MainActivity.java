package com.tim.wifibook;

import android.support.v4.app.Fragment;

/**
 * Created by tim on 1/15/14.
 */
public class MainActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new WelcomeFragment();
    }

}

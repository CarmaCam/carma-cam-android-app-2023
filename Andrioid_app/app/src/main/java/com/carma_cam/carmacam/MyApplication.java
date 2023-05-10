package com.carma_cam.carmacam;

import android.app.Application;
import android.util.Log;

/**
 * Created by jingwen on 09/03/2018.
 */

public class MyApplication extends Application {
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MY APPLICATION","Called");
        instance = this;
    }

    public static MyApplication getInstance(){

        return instance;
    }
}

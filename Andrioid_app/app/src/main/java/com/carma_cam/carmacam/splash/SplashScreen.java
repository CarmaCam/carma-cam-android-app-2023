package com.carma_cam.carmacam.splash;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.carma_cam.carmacam.R;
import com.carma_cam.carmacam.login.LoginActivity;
import com.carma_cam.carmacam.main.MainActivity;

import java.util.Calendar;


public class SplashScreen extends AppCompatActivity {
    private final int SPLASH_SCREEN_TIME = 2000;//START THE SYSTEM AFTER 2 SECONDS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Full);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash_screen);

        Log.d("Splash Screen", "start");

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
                startActivity(intent);
                finish();

            }
        }, SPLASH_SCREEN_TIME);
        getTime();
    }

    public void getTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 19 || hour <= 7) {
            MainActivity.SCENE_MODE = MainActivity.NIGHTMODE;

        } else {
            MainActivity.SCENE_MODE = MainActivity.DAYTIMEMODE;
        }

    }

}

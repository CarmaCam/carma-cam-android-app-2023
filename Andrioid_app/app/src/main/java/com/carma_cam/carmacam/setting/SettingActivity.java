package com.carma_cam.carmacam.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;

import com.carma_cam.carmacam.R;
import com.carma_cam.carmacam.login.LoginActivity;

public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "SettingActivity";

    private EditText mZoomEt;
    public static Float mZoomValue;
    private SeekBar mSeekBar;
    private double mProgress;

    private Button startButton;
    private RadioGroup radioGroup;
    private Switch videoBGSwitch;
    private int mode = 0;
    private boolean enableVideoBG;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        sharedPref = SettingActivity.this.getSharedPreferences(
                "carmacamsettings", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        mZoomEt = findViewById(R.id.zoomEt);
        mSeekBar = findViewById(R.id.seekBar);

        mZoomValue = mSeekBar.getProgress() * 0.01f;
        Log.d(TAG, "mProgress" + mProgress);
        if (!mZoomEt.getText().toString().isEmpty()) {
            mZoomValue = Float.parseFloat(mZoomEt.getText().toString());
            mProgress = mSeekBar.getProgress() * 0.01;
        }
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = mSeekBar.getProgress() * 0.01;
                Log.d(TAG, "mProgress" + mProgress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // start --> save
        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d(TAG, "MPROGRESS+++" + mProgress);


                Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
                intent.putExtra("mProgress", mProgress);
//                intent.putExtra("mode",mode);
                startActivity(intent);
                finish();
            }
        });
        radioGroup = (RadioGroup)findViewById(R.id.radio_group);
        int currentMode = getIntent().getIntExtra("mode", 0);
        Log.d("mode", "current setting: " + currentMode);
        switch (currentMode) {
            case 0:
                radioGroup.check(R.id.radioBtn_driveMode);
                break;
            case 1:
                radioGroup.check(R.id.radioBtn_parkMode);
                break;
            case 2:
                radioGroup.check(R.id.radioBtn_lawEnforcement);
                break;
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton check = (RadioButton)findViewById(radioGroup.getCheckedRadioButtonId());
                //System.out.println("hahha"+check.getText().toString());
                //System.out.println(check.getText().toString());

                if(check.getText().toString().equals("Drive Mode")){

                    mode = 0;
                    editor.putInt("drivemode", mode);
                    editor.commit();
                }
                else if(check.getText().toString().equals("Park Mode")) {
                    mode = 1;
                    editor.putInt("drivemode", mode);
                    editor.commit();
                }
                else{
                    mode = 2;
                    editor.putInt("drivemode", mode);
                    editor.commit();
                }

            }
        });

        videoBGSwitch = findViewById(R.id.enableVideoBG);
        enableVideoBG = sharedPref.getBoolean("enableVideoBG", true);
        videoBGSwitch.setChecked(enableVideoBG);
        videoBGSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean("enableVideoBG", isChecked);
                editor.apply();
            }
        });
    }
}

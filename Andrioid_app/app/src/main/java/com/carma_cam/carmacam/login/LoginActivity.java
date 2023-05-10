package com.carma_cam.carmacam.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.carma_cam.carmacam.R;
import com.carma_cam.carmacam.setting.SettingActivity;
import com.carma_cam.carmacam.external.Server;
import com.carma_cam.carmacam.main.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

//    private EditText mZoomEt;
//    public static Float mZoomValue;

    private int mode = 0;
    private boolean saveLogin;
    boolean enableVideoBG = false;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    private ImageView modechange;
    private ProgressBar progressBar;
    private Button btn_login;
    private EditText phone;
    private EditText pwd;
    private CheckBox checkBox;

    VideoView videoView;
    MediaPlayer mMediaPlayer;
    int mCurrentVideoPosition;
    private SeekBar mSeekBar;
    //    private double mProgress;
    public static double mProgressValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // added by Xiaoming Niu
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        initView();
    }

    private void initView() {
        findResources();

        sharedPref = LoginActivity.this.getSharedPreferences(
                "carmacamsettings", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        mode = sharedPref.getInt("drivemode", 0);
        enableVideoBG = sharedPref.getBoolean("enableVideoBG", false);

        setVideoBackground(enableVideoBG);

        saveLogin = sharedPref.getBoolean("saveLogin", false);
        if (saveLogin) {
            phone.setText(sharedPref.getString("phone", ""));
            pwd.setText(sharedPref.getString("password", ""));
        }
        checkBox.setChecked(saveLogin);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean("saveLogin", isChecked);
                editor.apply();
            }
        });

        modechange.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SettingActivity.class);
                intent.putExtra("mode", mode);
                startActivity(intent);
            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkInput()) {
                    progressBar.setVisibility(View.VISIBLE);
                    verifyLogin(phone.getText().toString(), pwd.getText().toString());
                }
                mProgressValue = getIntent().getDoubleExtra("mProgress", 0.0);
                Log.d(TAG, "mProgressValue" + mProgressValue);
            }
        });
    }


    private void findResources() {
        mSeekBar = findViewById(R.id.seekBar);
        videoView = findViewById(R.id.videoBG);
        modechange = findViewById(R.id.modechange);
        progressBar = findViewById(R.id.progressBar_login);
        btn_login = findViewById(R.id.btn_login);
        phone = findViewById(R.id.phone);
        pwd = findViewById(R.id.pwd);
        checkBox = findViewById(R.id.checkBox);
//        mZoomEt = findViewById(R.id.zoomEt);
    }

    private boolean checkInput() {
        if (phone.getText().length() == 0) {
            phone.setError("Please input your phone number");
            phone.requestFocus();
            return false;
        }
        if (pwd.getText().length() == 0) {
            pwd.setError("Please input your password");
            phone.requestFocus();
            return false;
        }

        return true;
    }

    private void verifyLogin(final String phone, final String password) {
//        String url = Server.SERVER_URL + "/login";
        // TODO: Remove this
        String url = Server.SERVER_URL + "/Account/login";
        Log.d("login url", url);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        JSONObject json = new JSONObject();
        try {
            json.put("username", phone);
            json.put("password", password);
            Log.d("phon url", phone+ password);
        } catch (JSONException e) {
            Log.e("JSON ERROR", "putting phone & password");
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, json, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.INVISIBLE);
                        Log.d("response dummy logger", response.toString());
                        try {
                            if (response.isNull("error")) {

                                if (checkBox.isChecked()) {
                                    editor.putString("phone", phone);
                                    editor.putString("password", password);
                                    editor.apply();
                                } else {
                                    editor.remove("phone");
                                    editor.remove("password");
                                    editor.apply();
                                }

                                String jwt = response.getString("jwt");
                                Log.d("jwt code", jwt);

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("mode", mode);
                                intent.putExtra("username", phone);
                                intent.putExtra("jwt", jwt);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.d("error code", response.getString("error"));
                                System.out.println("error");

                                Toast toast = Toast.makeText(LoginActivity.this, response.getString("error"), Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                        } catch (JSONException e) {
                            Log.e("JSON ERROR", "extract response");
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.INVISIBLE);
                        System.out.println(error);
                        Toast toast = Toast.makeText(LoginActivity.this, "SERVERt ERROR", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                // TODO: Remove this
                headers.put("PaAccessToken", "aefbef04-c3e5-4195-bf59-c489ea241554");
                return headers;
            }
        };

        queue.add(jsonObjectRequest);
    }

    private void setVideoBackground(boolean enable) {

        Uri uri = Uri.parse("android.resource://"
                + getPackageName()
                + "/"
                + R.raw.road);
        videoView.setVideoURI(uri);
        if (enable) {
            videoView.setVisibility(View.VISIBLE);
            videoView.start();
        } else {
            videoView.setVisibility(View.GONE);
            ImageView logo = findViewById(R.id.slogan);
            logo.setImageResource(R.drawable.carmacam_logo);
        }

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mMediaPlayer = mediaPlayer;
                mediaPlayer.setVolume(0, 0);
                mMediaPlayer.setLooping(true);
                if (mCurrentVideoPosition != 0) {
                    mMediaPlayer.seekTo(mCurrentVideoPosition);
                    mMediaPlayer.start();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (enableVideoBG) {
            mCurrentVideoPosition = mMediaPlayer.getCurrentPosition();
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (enableVideoBG) {
            videoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (enableVideoBG) {
            mMediaPlayer.release();
        }
    }

}

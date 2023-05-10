package com.carma_cam.carmacam.main;


import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.Toast;

import com.carma_cam.carmacam.LocationServiceCall;
import com.carma_cam.carmacam.login.LoginActivity;
import com.carma_cam.carmacam.R;
import com.carma_cam.carmacam.external.Server;
import com.carma_cam.carmacam.fragments.MapFragment;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TimeZone;


public class MainActivity extends Activity {

    //NetworkStateReceiver networkStateReceiver;
    String mapTag = "MAP_FRAGMENT_TAG";
    String camTag = "CAMERA_FRAGMENT_TAG";

    static final int VIEW_STATE_MAP = 0,
            VIEW_STATE_CAMERA = 1;

    FragmentManager mFragmentManager;
    MapFragment mMapFragment;
    Camera2VideoFragment mCamera2VideoFragment;

    HashMap<Integer, RotateView> getView;
    int[] toPortrait;
    int[] toLand;
    int[] toMapLand, toMapPortrait;
    int viewState;
    private ImageView img;
    public static boolean isBackground = false;
    public static boolean mainVideoStart = false;

    public static int mSecondsOfVideo = 20;
    public static final int DAYTIMEMODE = 0,
            CLOUDYMODE = 2,
            NIGHTMODE = 1;

    public static int SCENE_MODE = DAYTIMEMODE;
    public static int mode;
    private static String phone;
    private static String jwt;

    private ImageView mIvThemeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Full);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //Shobha: DO not delete below line. Else screen closes if app is not in record mode
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);
        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mode = intent.getIntExtra("mode", 0);

        phone = intent.getStringExtra("username");
        jwt = intent.getStringExtra("jwt");

        if (mode == 0) {
            mSecondsOfVideo = 20;
        } else {
            mSecondsOfVideo = 10;
        }

        //first time to create the activity
        if (savedInstanceState == null) {
            viewState = VIEW_STATE_CAMERA;
            // pass to fragment
            Bundle bundle = new Bundle();
            bundle.putString("phone", phone);
            bundle.putString("jwt", jwt);
            mCamera2VideoFragment = new Camera2VideoFragment();
            mCamera2VideoFragment.setArguments(bundle);
            mCamera2VideoFragment.setUpGoogleApiClient(this);
            mMapFragment = new MapFragment();
            mFragmentManager = getFragmentManager();
            initViewParams();

            //set upload button click
            findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCamera2VideoFragment.getView().findViewById(R.id.texture).performClick();
                }
            });

            mFragmentManager
                    .beginTransaction()
                    .add(R.id.hold_view, mMapFragment, mapTag)
                    .add(R.id.mainFrameLayout, mCamera2VideoFragment, camTag)
                    .commit();
            (findViewById(R.id.bottomFrameLayout))
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int curMapView = ((ViewGroup) mMapFragment.getView().getParent()).getId(),
                                    curCamView = ((ViewGroup) mCamera2VideoFragment.getView().getParent()).getId();
                            mFragmentManager
                                    .beginTransaction()
                                    .remove(mMapFragment)
                                    .remove(mCamera2VideoFragment)
                                    .commit();
//							//commit is asynchronized, so we need it
                            mFragmentManager.executePendingTransactions();
                            mFragmentManager
                                    .beginTransaction()
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .replace(curMapView, mCamera2VideoFragment, camTag)
                                    .replace(curCamView, mMapFragment, mapTag)
                                    .commit();
                            mFragmentManager.executePendingTransactions();


                            if (curCamView == R.id.mainFrameLayout) {//after swap, main is map
                                viewState = VIEW_STATE_MAP;
                                findViewById(R.id.btn_upload).setVisibility(View.VISIBLE);
                                //findViewById(R.id.ll_zoom).setVisibility(View.VISIBLE);
                            } else {
                                viewState = VIEW_STATE_CAMERA;
                                findViewById(R.id.btn_upload).setVisibility(View.GONE);
                                //findViewById(R.id.ll_zoom).setVisibility(View.GONE);
                            }
                            handleRotate(getResources().getConfiguration());
                            mCamera2VideoFragment.getCameraView().viewChange(CameraViewManager.MAIN_VIEW_CHANGE);
                        }
                    });

            mIvThemeMode = (ImageView) findViewById(R.id.iv_theme_mode_activity_main);
            if (SCENE_MODE == DAYTIMEMODE) {
                mIvThemeMode.setImageResource(R.drawable.sunmode);
            } else if (SCENE_MODE == CLOUDYMODE) {
                mIvThemeMode.setImageResource(R.drawable.cloudsmode);
            } else {
                mIvThemeMode.setImageResource(R.drawable.nightmode);
            }
            mIvThemeMode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SCENE_MODE = (SCENE_MODE + 1) % 3;
                    if (SCENE_MODE == DAYTIMEMODE) {
                        mIvThemeMode.setImageResource(R.drawable.sunmode);
                    } else if (SCENE_MODE == CLOUDYMODE) {
                        mIvThemeMode.setImageResource(R.drawable.cloudsmode);
                    } else {
                        mIvThemeMode.setImageResource(R.drawable.nightmode);
                    }
                    mCamera2VideoFragment.modeChange();
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("started", true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // auto-login when resuming
        attemptLogin();

        handleRotate(getResources().getConfiguration());
    }

    // Attempt to login to check if jwt is expired
    private void attemptLogin() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    // use username instead of phone number
                    // String urlString = Server.SERVER_URL + "/account?phone=" + phone;
                    String urlString = Server.SERVER_URL + "/account?username=" + phone;
                    Log.d("URL", urlString);
                    URL url = new URL(urlString);


                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("PaAccessToken", "42c86f0c-002c-4161-8620-77769e6f6c62");
                    conn.setRequestProperty("Authorization", "Bearer " + jwt);
                    Log.d("jwt", jwt);
                    conn.setRequestProperty("Accept", "*/*");

                    conn.connect();

                    int HttpResult = conn.getResponseCode();
                    Log.d("response code", Integer.toString(HttpResult));
                    if (HttpResult == 403) {
                        Log.d("jwt status", "jwt expired");
                        // Go back to Login Activity
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }

                } catch (Exception e) {
                    Log.d("Yuwei", e.toString());
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    //Following 3 functions have been added to handle activity lifecycle
    @Override
    protected void onResume() {
		/*if (networkStateReceiver == null) {
			networkStateReceiver = new NetworkStateReceiver();
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(networkStateReceiver, filter);*/
//		RetryUpload();
        super.onResume();
    }

    @Override
    protected void onPause() {
        //unregisterReceiver(networkStateReceiver);
        Log.d("Camera2Fragment Test:: ", "onPause");
        if (mainVideoStart) {
            isBackground = true;
        }
        super.onPause();
    }


    //this is to set the launch again and no need to see splash screen
    @Override
    public void onBackPressed() {
        // super.onBackPressed(); 	not call the parent class method
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleRotate(newConfig);
    }

    void initViewParams() {
        getView = new HashMap<Integer, RotateView>();
        getView.put(R.layout.fragment_camera2_video_land_map,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.fragment_camera2_video_land_map, null),
                        new int[] {R.id.video_frame}));
        getView.put(R.layout.fragment_camera2_video_land,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.fragment_camera2_video_land, null),
                        new int[] {R.id.cameraSideMenu, R.id.video_frame}));
        getView.put(R.layout.fragment_camera2_video,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.fragment_camera2_video, null),
                        new int[] {R.id.cameraSideMenu, R.id.video_frame}));

        getView.put(R.layout.map_fragment_land_map,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.map_fragment_land_map, null),
                        new int[] {R.id.map_frame, R.id.bottom_frame}));
        getView.put(R.layout.map_fragment,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.map_fragment, null),
                        new int[] {R.id.map_frame, R.id.bottom_frame}));
        getView.put(R.layout.map_fragment_map,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.map_fragment_map, null),
                        new int[] {R.id.map_frame, R.id.bottom_frame}));


        getView.put(R.layout.activity_main_land,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.activity_main_land, null),
                        new int[] {R.id.bottomFrameLayout, R.id.btn_upload, R.id.ll_report}));
        getView.put(R.layout.activity_main,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.activity_main, null),
                        new int[] {R.id.bottomFrameLayout, R.id.btn_upload, R.id.ll_report}));
        getView.put(R.layout.activity_main_land_map,
                new RotateView(LayoutInflater.from(this).inflate(R.layout.activity_main_land_map, null),
                        new int[] {R.id.bottomFrameLayout, R.id.btn_upload, R.id.ll_report}));

        toPortrait = new int[] {R.layout.activity_main, R.layout.fragment_camera2_video, R.layout.map_fragment};
        toLand = new int[] {R.layout.activity_main_land, R.layout.fragment_camera2_video_land, R.layout.map_fragment};

        toMapLand = new int[] {R.layout.activity_main_land_map, R.layout.fragment_camera2_video_land_map,
                R.layout.map_fragment_land_map};
        toMapPortrait = new int[] {R.layout.activity_main, R.layout.fragment_camera2_video_land_map, R.layout.map_fragment_map};
    }

    void handleRotate(Configuration newConfig) {
        // Checks the orientation of the screen
        int[] transform = null;
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (viewState == VIEW_STATE_MAP) {
                transform = toMapLand;
            } else {
                transform = toLand;
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (viewState == VIEW_STATE_MAP) {
                transform = toMapPortrait;
            } else {
                transform = toPortrait;
            }
        } else {
            return;
        }

        for (Integer i : transform) {
            RotateView vto = getView.get(i);
            for (Integer vid : vto.widgetId) {
                View v = findViewById(vid);
                ViewGroup.LayoutParams vp = vto.v.findViewById(vid).getLayoutParams();
                v.setLayoutParams(vp);
            }
        }
    }

    void RetryUpload() {
        String path = this.getExternalFilesDir(null).getAbsolutePath();
        if (new File(path + "backup").exists()) {
            File fList[] = new File(path + "backup").listFiles();
            if (fList.length != 0) {
                int num = 3;
                Queue<File> heap = new PriorityQueue<>(fList.length, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        if (f1.lastModified() > f2.lastModified()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
                for (int i = 0; i < fList.length; i++) {
                    heap.offer(fList[i]);
                }

                for (int i = 0; i < Math.min(num, fList.length); i++) {
                    new MainActivity.UploadToServer(this, heap.poll().getAbsolutePath()).execute();
                }
            }
        }
    }

    class UploadToServer extends AsyncTask<String, Integer, String> {
        private String phoneNumber;
        private int isImmediateHazard = 0;
        private LocationServiceCall locationServiceCallObject = new LocationServiceCall(getApplicationContext());
        private String mLongitude;
        private String mLatitude;
        private Context mContext;
        private SoundPool mSoundPool = null;
        private HashMap<Integer, Integer> soundID = new HashMap<Integer, Integer>();
        private String mVideoFilename;

        UploadToServer(Context context, String path) {
            mContext = context;
            mVideoFilename = path;
        }

        @Override
        protected void onPreExecute() {

        }


        @Override
        protected String doInBackground(String... params) {
            locationServiceCallObject.connect();
            mLatitude = locationServiceCallObject.getLatitude();
            mLongitude = locationServiceCallObject.getLongitude();
            Log.i(Camera2VideoFragment.class.getName(), "begin uploading...");
//			TelephonyManager manager =(TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
//			if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager
//			.PERMISSION_GRANTED) {
//				phoneNumber = manager.getLine1Number();
//			}
//
//            phoneNumber = phoneNumber.replaceAll("[^\\d]", "");
            phoneNumber = phone;
            Log.d("PHONE #", phoneNumber);

            //this is the JSON for the metadata information
            JSONObject metadata = new JSONObject();

            try {

                String format = "MM/dd/yyyy HH:mm:ss";
                final SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String utcTime = sdf.format(new Date());

                metadata.put("duration", "32");
                metadata.put("framesPerSecond", "6");
                metadata.put("isImmediateHazard", isImmediateHazard);
                metadata.put("locationRecorded", mLatitude + "," + mLongitude);
                metadata.put("sizeInMB", "10");
                metadata.put("speedInMPH", "90");
                metadata.put("timeOfRecording", utcTime);
                metadata.put("phoneNumber", phoneNumber);

                isImmediateHazard = 0;
            } catch (JSONException e) {
                e.printStackTrace();
                return "JSON: " + e.getMessage();//test
            }

            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(mVideoFilename);

            if (!sourceFile.isFile()) {
                return "Source File not exist";//test
            } else {
                try {
                    // open a URL connection to the Servlet
                    FileInputStream fileInputStream = new FileInputStream(sourceFile);
                    URL url = new URL(getString(R.string.mainURL) + getString(R.string.uploadVideoURL));

                    Log.d("UPLOADING:", mVideoFilename);

                    // Open a HTTP  connection to  the URL
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    dos = new DataOutputStream(conn.getOutputStream());//break for unavailable network

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes(
                            "Content-Disposition: form-data; name=\"filefield\"; filename=\"" + mVideoFilename + "\"" + lineEnd);
                    dos.writeBytes("Content-Type: video/mp4" + lineEnd);
                    dos.writeBytes(lineEnd);

                    // create a buffer of  maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {

                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    }

                    // send multipart form data necessary after file data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"metadata\"" + lineEnd);
                    dos.writeBytes("Content-Type: application/json" + lineEnd);
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(String.valueOf(metadata));
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);   //EoF request payload... IMPORTANT
                    dos.flush();
                    dos.close();

                    // Responses from the server (code and message)
                    int serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();

                    Log.i("uploadFile", "HTTP Response is : "
                            + serverResponseMessage + "and " + serverResponseCode);

                    //close the streams
                    fileInputStream.close();
                    if (serverResponseCode >= 200 && serverResponseCode < 300) {
                        new File(mVideoFilename).delete();
                        return "1";
                    } else {
                        Log.e(Camera2VideoFragment.class.getName(), "server response code: " + serverResponseCode);
                        return "server response code: " + serverResponseCode;//test
                    }
                    //}

                } catch (Exception ex) {
                    ex.printStackTrace();
                    return "connection error: " + ex.getMessage();//test
                }
            } // End else block
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                initSP();
                if (result == "1") {
                    Log.d("SUCCESS", "uploaded to mongo");
                    mSoundPool.play(soundID.get(3), 1, 1, 0, 0, 1);
                    Toast.makeText(mContext, "UPLOAD SUCCESSFUL", Toast.LENGTH_LONG).show();
                } else {
                    mSoundPool.play(soundID.get(4), 1, 1, 0, 0, 1);
                    Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
                    Log.i(Camera2VideoFragment.class.getName(), "upload failed due to result:" + result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void initSP() throws Exception {
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(1);
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            attrBuilder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            builder.setAudioAttributes(attrBuilder.build());
            mSoundPool = builder.build();
            soundID.put(1, mSoundPool.load(mContext, R.raw.recording, 1));
            soundID.put(2, mSoundPool.load(mContext, R.raw.uploading, 1));
            soundID.put(3, mSoundPool.load(mContext, R.raw.upload_success, 1));
            soundID.put(4, mSoundPool.load(mContext, R.raw.upload_failed, 1));
            soundID.put(5, mSoundPool.load(mContext, R.raw.recording_stopped, 1));
        }
    }

    class RotateView {
        View v;
        int[] widgetId;

        public RotateView(View v, int[] widgetId) {
            this.v = v;
            this.widgetId = widgetId;
        }
    }
}

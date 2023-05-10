package com.carma_cam.carmacam.main;

/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.carma_cam.carmacam.LocationServiceCall;
import com.carma_cam.carmacam.MyApplication;
import com.carma_cam.carmacam.R;
import com.carma_cam.carmacam.external.Server;
import com.carma_cam.carmacam.fragments.AutoFitTextureView;
import com.carma_cam.carmacam.login.LoginActivity;
import com.carma_cam.carmacam.setting.SettingActivity;
import com.carma_cam.carmacam.utils.CircularEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.carma_cam.carmacam.R.id.video;


public class Camera2VideoFragment extends Fragment
        implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {

    public static final int RECORDING = 1;
    public static final int UPLOADING = 2;
    public static final int UPLOAD_SUCCESS = 3;
    public static final int UPLOAD_FAILED = 4;
    public static final int UPLOAD_STOPPED = 5;

    private Double mZoomEtValue;

    public Camera2VideoFragment() {
    }

    //##ffmpeg
    Context context = MyApplication.getInstance();
    //Context context = getActivity().getApplicationContext();
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    Context mContext;

    private static final String TAG = "Camera2VideoFragment";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final int REQUEST_PHONE_PERMISSIONS = 9;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA
    };
    private static final String[] PHONE_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE
    };


    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    //declarations for video buffers
    private boolean bool = true; //wtf, what does bool mean...
    private boolean pauseRecord = false;
    private Rect zoom;
    private String cameraId;
    CameraViewManager cameraView = new CameraViewManager(CameraViewManager.INITIALIZING, this);

    private boolean mFileSaveInProgress = false;

    //View record;
    //for Emergency ALert
    private int isImmediateHazard = 0;

    //for audio notifications
    private SoundPool mSoundPool = null;
    private HashMap<Integer, Integer> soundID = new HashMap<Integer, Integer>();
    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;
    public boolean startVideoFlag = false;

    /**
     * Button to record video
     */
    private Button mButtonVideo;

    /**
     * A refernce to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession mPreviewSession;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    //required to get locations for file-level metadata
    private LocationServiceCall locationServiceCallObject;

    // login information
    private String phoneNumber = "";
    private String jwt;
    private String videoUploadData;
    private JSONObject videoUploadDataResponse;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            Log.d(TAG, "texture available to open camera");
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };


    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link Size} of video recording.
     */
    private Size mVideoSize;


    /**
     * CircularEncoder
     */
    private CircularEncoder mCircEncoder;

    /**
     * {@link CircularEncoder Callback} is called when {@link CameraDevice} changes its status.
     */
    private CircularEncoder.Callback mCircularEncoderCallback = new CircularEncoder.Callback() {
        @Override
        public void fileSaveComplete(int status) {
            mFileSaveInProgress = false;
            //Important to message to UIthread to execute UI changes
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    updateVariables();
                    new UploadToServer().execute();
                    startPreview();
                }
            });

        }

        @Override
        public void bufferStatus(long totalTimeMsec) {
            Log.d("DEBUG", totalTimeMsec + "");
        }
    };


    /**
     * EncoderSurface
     */
//    private WindowSurface mEncoderSurface;

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo = false;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    private int recordCount = 0;


    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    //Accelerometer variables
    SensorManager sManager;
    Sensor accelerometerSensor;
    SensorEventListener acceleromererListener;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {

            mCameraDevice = cameraDevice;
            Log.d("debug", "hardware level is " + mHardwareLevel);
            if (videoMode != MainActivity.DAYTIMEMODE &&
                    mHardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
                updateParam();
            } else {
                startPreview();
            }
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private Integer mSensorOrientation;
    private CaptureRequest.Builder mPreviewBuilder;
    private Surface mRecorderSurface;
    private String mLongitude;
    private String mLatitude;

    private CameraCharacteristics characteristics;

    //required to the mode of video (0--daytime, 1--cloudy, 2--night)
    private int videoMode = MainActivity.DAYTIMEMODE;

    //required to get camera support level
    private int mHardwareLevel = 2;

    //required to get the lastest exposure time
    private Long lastExpoTime = 0l;

    //required to get the lastest frame duration time
    private Long lastFrameDurationTime = 0l;

    //required to check if above two parameters have been updated
    private boolean hasUpdateParam = false;

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {

            lastExpoTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
            lastFrameDurationTime = result.get(CaptureResult.SENSOR_FRAME_DURATION);


//            Log.d("myDebug", "Auto-exposure comp: " + result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION));
//            Log.d("myDebug", "expoTime: " + lastExpoTime);
//            Log.d("myDebug", "frameDurationTime: " + lastFrameDurationTime);
//            Log.d("myDebug", "iso: " + result.get(CaptureResult.SENSOR_SENSITIVITY));

            if (!hasUpdateParam) {
                startPreview();
            }
            hasUpdateParam = true;
        }
    };

    public static Camera2VideoFragment newInstance() {
        return new Camera2VideoFragment();
    }

    public void setUpGoogleApiClient(Context context) {
        mContext = context;
        locationServiceCallObject = new LocationServiceCall(mContext);

    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        phoneNumber = getArguments().getString("phone");
        jwt = getArguments().getString("jwt");
        if (SettingActivity.mZoomValue != null) {
            mZoomEtValue = LoginActivity.mProgressValue;
            Log.d(TAG, "mProgressValue---" + mZoomEtValue);
        } else {
            mZoomEtValue = 0.0;
        }

        Log.d(TAG, "zoom value " + mZoomEtValue);


        try {
            //added by Jingyi 2.22.2017: for Acclerometer sensor
            sManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            accelerometerSensor = sManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            acceleromererListener = new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }

                @Override
                public void onSensorChanged(SensorEvent event) {

                    if (bool) { //it needs to be true in BDR because auto-upload should be based on recording status
                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];

                        double threshold = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

                        /* threshold for collision is about 5g ＝ 49 m/s^2 */
                        if (threshold > 49)

                     /* threshold for slamming on brakes
                     From some research, acceleration for this event is said to be small,
                     we should consider this small value but eliminate a false positive
                    with a little higher threshold to trigger wrongly —
                    phone dropped down from someone’s hand with an acceleration about 2.5g,
                     so we need to test for an accuracy range for slamming on brakes. */

//                     Shobha: Commenting below line since there is a false positive on my phone test.
//                                Needs more testing for accurate threshold
//                                || (threshold > 12 && threshold < 15 && z > 0))
                        {
                            stopRecordingVideo();
                        }

                    }
                }

            };
            sManager.registerListener(acceleromererListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inflater.inflate(R.layout.fragment_camera2_video, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        // added by Xiaoming Niu
//        mZoomEt = view.findViewById(R.id.zoomEt);
//
//
//        if (mZoomEt != null) {
//            mZoomEtText = mZoomEt.getText();
//            mZoomEtValue = Float.parseFloat(String.valueOf(mZoomEtText)) * 10;
//        } else {
//            mZoomEtValue = 10.0f;
//        }

        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mTextureView.setOnClickListener(this);

        mButtonVideo = (Button) view.findViewById(video);
        mButtonVideo.setOnClickListener(this);

        cameraView.viewChange(cameraView.getCurrentState());
        try {
            initSP();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //function for start video voice command
    public void startVideo() {
        Handler timedWait = new Handler();
        timedWait.postDelayed(new Runnable() {
            public void run() {
                // Actions to do after 3 seconds
                mIsRecordingVideo = true;
                bool = true;
                mSoundPool.play(soundID.get(1), 1, 1, 0, 0, 1);
                handler.removeCallbacks(mRecordingVideoTask);
                handler.postDelayed(mRecordingVideoTask, 1000);
                Log.d("APP LAUNCH: ", "recording started");
            }
        }, 2000);
    }

    //for audio notification initialization
    private void initSP() throws Exception {
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        attrBuilder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
        builder.setAudioAttributes(attrBuilder.build());
        mSoundPool = builder.build();
        soundID.put(RECORDING, mSoundPool.load(getContext(), R.raw.recording, 1));
        soundID.put(UPLOADING, mSoundPool.load(getContext(), R.raw.uploading, 1));
        soundID.put(UPLOAD_SUCCESS, mSoundPool.load(getContext(), R.raw.upload_success, 1));
        soundID.put(UPLOAD_FAILED, mSoundPool.load(getContext(), R.raw.upload_failed, 1));
        soundID.put(UPLOAD_STOPPED, mSoundPool.load(getContext(), R.raw.recording_stopped, 1));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pauseRecord && mIsRecordingVideo) {
            pauseRecord = false;
        }

        locationServiceCallObject.connect();
        setUpVideoMode();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            Log.d(TAG, "resume to open camera");
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        if (MainActivity.isBackground) {
            startVideo();
            MainActivity.isBackground = false;
        }
    }

    @Override
    public void onPause() {
        if (mIsRecordingVideo) {
            pauseRecord = true;
        }
        closeCamera();
        locationServiceCallObject.disconnect();
        stopBackgroundThread();
        super.onPause();
    }


    @Override
    public void onDestroy() {
        Log.d("Camera2VideoFragment:: ", "onDestroy");
        Thread.interrupted();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        startVideoFlag = false;
        super.onStart();
    }

    @Override
    public void onStop() {
        if (MainActivity.isBackground) {
            stopCapture();
            releaseCircularEncoder();
        }
        super.onStop();
    }

    private long mLastClickTime = 0;

    @Override
    public void onClick(View view) {
        // Disable double click
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        switch (view.getId()) {
            case R.id.texture:
                if (mIsRecordingVideo) { // upload video
                    stopRecordingVideo();
                    //resume recording
                    recordCount = 0;

                    mIsRecordingVideo = true;
                    bool = true;
                    handler.removeCallbacks(mRecordingVideoTask);
                    handler.postDelayed(mRecordingVideoTask, 1000);
                    Log.d("RECORDING", "Resumed after upload");
                }
                break;
            case R.id.video:
                if (cameraView.getCurrentState() ==
                        CameraViewManager.RECORDING) { // stop recording video and upload as immediate alert
                    isImmediateHazard = 1;
                    stopRecordingVideo();
                    recordCount = 0;
                    mIsRecordingVideo = true;
                    bool = true;
                    handler.removeCallbacks(mRecordingVideoTask);
                    //mSoundPool.play(soundID.get(1), 1, 1, 0, 0, 1);
                    handler.postDelayed(mRecordingVideoTask, 1000);
                    Log.d("RECORDING", "Resumed after upload");
                } else { // start recoding video
                    recordCount = 0;
                    mIsRecordingVideo = true;
                    bool = true;
                    handler.removeCallbacks(mRecordingVideoTask);
                    mSoundPool.play(soundID.get(1), 1, 1, 0, 0, 1);
                    cameraView.viewChange(CameraViewManager.RECORDING);
                    handler.postDelayed(mRecordingVideoTask, 1000);
                    Log.d("RECORDING", "STARTED");
                }
                break;
        }

    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getActivity().getFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            Log.d("Permissions", permissions.toString());
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(getActivity().getFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(getActivity().getFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    private void openCamera(int width, int height) {
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestVideoPermissions();
            return;
        }

        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(25000, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            System.out.println("Video size" + mVideoSize);
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            System.out.println("Texture view" + mTextureView.getHeight() + "," + mTextureView.getWidth());
            deleteFiles();
            manager.openCamera(cameraId, mStateCallback, null);

        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getActivity().getFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        } catch (SecurityException e) {
            Log.e("Security Exception", e.toString());
        }
        if (startVideoFlag == false) {
            startVideo();
            startVideoFlag = true;
            MainActivity.mainVideoStart = true;
        }
    }

    private void setZoom() {


        final Activity activity = getActivity();


        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            Log.d(TAG, "mZoomEtValue " + mZoomEtValue);
            float maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
            int minW = (int) (m.width() / maxZoom);
            int minH = (int) (m.height() / maxZoom);
            int difW = m.width() - minW;
            int difH = m.height() - minH;
            //zoom currently set at 45% (previous)
            // changed to 50
            int zoom_level = 50 + (int) ((mZoomEtValue - 1) * 10);
            int zoom_percentage = (int) ((zoom_level * maxZoom) / (100));
            int cropW = difW / 100 * zoom_percentage;
            int cropH = difH / 100 * zoom_percentage;
            cropW -= cropW & 3;
            cropH -= cropH & 3;
            zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
            Log.d("Zoom level", "Zoom level" + String.valueOf(zoom_percentage));
//            zoom = new Rect(cropW, cropH, mTextureView.getWidth()+2*cropW,mTextureView.getHeight()+2*cropH);

            mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getActivity().getFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    private void deleteFiles() {
        File folder = new File(getActivity().getExternalFilesDir(null).getAbsolutePath());
        File fList[] = folder.listFiles();
        int count = 0;
        for (int i = 0; i < fList.length; i++) {
            String pes = fList[i].toString();
            if (pes.endsWith(".mp4") || pes.endsWith(".txt")) {
                // and deletes
                new File(pes).delete();
                count++;
            }
        }
        System.out.println("Deleted " + count + " files!");
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    setZoom();
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                             long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    if (mCircEncoder != null) {
                        mCircEncoder.frameAvailableSoon();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        // Set frame rate to highest [60, 60]
        Range<Integer> fpsRange = new Range<>(60, 60);
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
        Log.d("Frame rate", String.valueOf(builder.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)));
        if (videoMode != MainActivity.DAYTIMEMODE) {

            if (mHardwareLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                Range<Integer> rangeOfISO = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                if (videoMode == MainActivity.NIGHTMODE) {
                    // In night mode, set ISO to 1/4 of its range
                    System.out.println("nightNum: " + (int) (rangeOfISO.getUpper() * 0.25));
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, (int) (rangeOfISO.getUpper() * 0.25));
                    // TODO: Set night mode focus to a range with minimum value
                    builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.3f);
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                } else {
                    System.out.println("cloudyNum: " + (int) (rangeOfISO.getUpper() * 0.4));
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, (int) (rangeOfISO.getUpper() * 0.4));
                }
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, lastExpoTime);
                builder.set(CaptureRequest.SENSOR_FRAME_DURATION, lastFrameDurationTime);

            } else {

                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                Range<Integer> rangeOfAEComp = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                if (videoMode == MainActivity.NIGHTMODE) {
                    // Manually set focus distance to 0.3f and exposure compensation to -1
                    builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.3f);
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                    Log.d("Fixed focus distance", String.valueOf(builder.get(CaptureRequest.LENS_FOCUS_DISTANCE)));
                    builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (int) (rangeOfAEComp.getUpper() * -1));
                    Log.d("Exposure compensation", String.valueOf(builder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)));

                } else {
                    //Log.d("MyDebug", "cloudy mode");
                    System.out.println("cloudyNum: " + (int) (rangeOfAEComp.getUpper() * 0.4));
                    builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (int) (rangeOfAEComp.getUpper() * 0.4));
                }
            }
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, -1);
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    private String getVideoFilePath(Context context, int clipNumber) {
        return context.getExternalFilesDir(null).getAbsolutePath() + "/CLIP" + clipNumber + ".mp4";
    }

    private void startRecordingVideo() {
        Log.d("Camera2VideoFragment:: ", "startRecordingVideo called");
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // TODO: adjust bit rate based on frame rate?
            // TODO: adjust video width/height based on what we're getting from the camera preview?
            //       (can we guarantee that camera preview size is compatible with AVC video encoder?)

            mCircEncoder = new CircularEncoder(mVideoSize.getWidth(), mVideoSize.getHeight(), 6000000,
                    30, MainActivity.mSecondsOfVideo, mCircularEncoderCallback, locationServiceCallObject, MainActivity.mode,
                    isImmediateHazard);

            Surface mEncoderSurface = mCircEncoder.getInputSurface();
            surfaces.add(mEncoderSurface);
            mPreviewBuilder.addTarget(mEncoderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    setZoom();
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cameraView.viewChange(CameraViewManager.RECORDING);
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;

        }
    }


    private void stopCapture() {
        // UI
        mIsRecordingVideo = false;
        bool = false;
        if (mPreviewSession != null) {
            try {
                mPreviewSession.stopRepeating();
                mPreviewSession.abortCaptures();
            } catch (CameraAccessException e) {
                Log.e("Camera", e.toString());
            }
        }
        cameraView.viewChange(CameraViewManager.NOT_RECORDING);
    }

    private void releaseCircularEncoder() {
        if (mCircEncoder != null) {
            mCircEncoder.shutdown();
            mCircEncoder = null;
        }

    }

    private String mVideoFilename;

    private void doCopy(String dirName, String outPath) {
        AssetManager assets = context.getAssets();
        try {
            String[] srcFiles = assets.list(dirName);//for directory
            for (String srcFileName : srcFiles) {
                if (srcFileName.equals("Helvetica-Regular.ttf")) {
                    String outFileName = outPath + File.separator + srcFileName;
                    String inFileName = dirName + File.separator + srcFileName;
                    if (dirName.equals("")) {// for first time
                        inFileName = srcFileName;
                    }
                    try {
                        InputStream inputStream = assets.open(inFileName);
                        copyAndClose(inputStream, new FileOutputStream(outFileName));
                    } catch (IOException e) {//if directory fails exception
                        new File(outFileName).mkdir();
                        doCopy(inFileName, outFileName);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/*public static void closeQuietly(AutoCloseable autoCloseable)  {
		try {
			if(autoCloseable != null) {
				autoCloseable.close();
			}
		} catch(IOException ioe) {
			//skip
		}
	}*/

    public static void copyAndClose(InputStream input, OutputStream output) {
        copy(input, output);
        try {
            input.close();
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //closeQuietly(input);
        //closeQuietly(output);
    }

    public static void copy(InputStream input, OutputStream output) {
        byte[] buffer = new byte[1024];
        int n = 0;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingVideo() {
        doCopy("", context.getExternalFilesDir(null).getAbsolutePath());

        stopCapture();
        mVideoFilename = getActivity().getExternalFilesDir(null).getAbsolutePath() + "/UploadClip.mp4";

        if (new File(mVideoFilename).exists()) {
            new File(mVideoFilename).delete();
        }


        //notify user
        cameraView.viewChange(CameraViewManager.UPLOADING);
        mSoundPool.play(soundID.get(2), 1, 1, 0, 0, 1);


        mFileSaveInProgress = true;
        File outputFile = new File(getActivity().getExternalFilesDir(null).getAbsolutePath(), "UploadClip.mp4");
        System.out.println("PAATH" + outputFile.getAbsolutePath());
        if (mCircEncoder != null) {
            mCircEncoder.saveVideo(outputFile);
        }
        releaseCircularEncoder();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent, VIDEO_PERMISSIONS,
                                    REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.getActivity().finish();
                                }
                            })
                    .create();
        }

    }

    private Handler handler = new Handler();
    private Runnable mRecordingVideoTask = new Runnable() {
        @Override
        public void run() {
            if (bool) {
                if (!mIsRecordingVideo) {
                    mIsRecordingVideo = true;
                }
                // When user clicks Record button
                startRecordingVideo();
            }
        }
    };

    private int fileCreationDate(String file1, String file2, int count) {
        File f1 = new File(file1);
        File f2 = new File(file2);
        long val = f1.lastModified() - f2.lastModified();
        int diff = (int) val / 1000;

        if (diff > 26) // Maximum time difference between 2 videos is 26 seconds
        {
            return count - 1;
        } else {
            return count;
        }
    }


    public void updateVariables() {
        mLatitude = locationServiceCallObject.getLatitude();
        mLongitude = locationServiceCallObject.getLongitude();
    }

    private class UploadToServer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            getActivity().findViewById(R.id.video_frame).setPadding(12, 12, 12, 12);
            getActivity().findViewById(R.id.video_frame).setBackgroundResource(R.drawable.stroke_yellow);
            if (getActivity().findViewById(R.id.btn_upload).getVisibility() == View.VISIBLE) {
                getActivity().findViewById(R.id.btn_upload).setBackgroundResource(R.drawable.btn_report_yellow);
            }
        }


        @Override
        protected String doInBackground(String... params) {
            Log.d("Camera2VideoFragment", "begin uploading...");

            //this is the JSON for the metadata information
            JSONObject metadata = new JSONObject();

            try {
                metadata.put("duration", 32);
                metadata.put("frames_per_second", 6);
                metadata.put("is_immediate_hazard", isImmediateHazard != 0);
                metadata.put("location_recorded", mLatitude + "," + mLongitude);
                metadata.put("size_in_mb", 10);
                metadata.put("speed_in_mph", 90);
                metadata.put("time_of_recording", getUtcTime());
                metadata.put("phone_number", phoneNumber);
//                metadata.put("video_data", "");
//                metadata.put("video_metadata", new JSONArray());
                metadata.put("video_file", "");
                metadata.put("ml_scoring_metadata", new JSONArray());

                Log.d("Camera2VideoFragment", phoneNumber);
                Log.d("Camera2VideoFragment", "Alert" + isImmediateHazard);

                isImmediateHazard = 0;
            } catch (JSONException e) {
                e.printStackTrace();
                return "JSON: " + e.getMessage();
            }


            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "WebKitFormBoundary7MA4YWxkTrZu0gW";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(mVideoFilename); // for recorded video upload
//            File sourceFile = new File("/data/data/com.carma_cam.carmacam/files/videos/lane change 6ESS586.mp4"); // for
//            direct file upload

            String insertId;

            if (!sourceFile.isFile()) {
                return "Source File not exist";//test
            } else {
                try {
                    URL url = new URL(Server.SERVER_URL + "/video");
                    Log.d("caizhixing", Server.SERVER_URL + "/video");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("PaAccessToken", "aefbef04-c3e5-4195-bf59-c489ea241554");
                    // jwt
                    conn.setRequestProperty("Authorization", "Bearer " + jwt);
                    Log.d("jwt", "Bearer " + jwt);

                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestMethod("POST");

                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(metadata.toString());
                    wr.flush();

                    Log.d("meta_data", metadata.toString());
                    //display what returns the POST request

                    StringBuilder sb = new StringBuilder();
                    int HttpResult = conn.getResponseCode();
                    String my_error = conn.getResponseMessage();

                    if (HttpResult >= 200 && HttpResult < 300) { //result success?
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(conn.getInputStream(), "utf-8"));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        Log.d("video metadata results", sb.toString());
                        JSONObject resultJson = new JSONObject(sb.toString());
                        videoUploadData = resultJson.getJSONObject("data").toString();
                        videoUploadDataResponse = resultJson.getJSONObject("data");
//                        if (resultJson.getInt("insertedCount") == 1) {
//                            insertId = resultJson.getJSONArray("insertedIds").getString(0);
//                            Log.d("Insert ID", insertId);
//                        } else {
//                            Log.e(Camera2VideoFragment.class.getName(), "result not ok");
//                            return "server response: " + sb.toString();
//                        }
                        insertId = resultJson.getJSONObject("data").getString("id");
                    } else {
                        Log.e(Camera2VideoFragment.class.getName(), "server response code for video_metadata: " + my_error);
                        return "server response code: " + my_error;
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    RetryLogic(sourceFile);
                    return "connection 0 error: " + ex.getMessage();//test
                }
                try {

                    // open a URL connection to the Servlet
                    FileInputStream fileInputStream = new FileInputStream(sourceFile);
                    URL url = new URL(Server.VIDEO_FILE_URL + insertId);
//                    URL url = new URL(getString(R.string.mainURL)+getString(R.string.uploadVideoURL)); // production server
//                    URL url = new URL("http://18.218.120.121:9001/uploadVideo"); // test server

                    Log.d("UPLOADING:", mVideoFilename);
                    Log.d("Upload URL:", url.toString());

                    // Open a HTTP  connection to  the URL
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod("POST");

                    // TODO: remove
                    conn.setRequestProperty("PaAccessToken", "aefbef04-c3e5-4195-bf59-c489ea241554");
                    // jwt
                    conn.setRequestProperty("Authorization", "Bearer " + jwt);

                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes(
                            "Content-Disposition: form-data; name=\"file\"; filename=\"" + mVideoFilename + "\"" + lineEnd);
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
                        //test
                        InputStream inputStream = conn.getInputStream();
                        String resultData = null;
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byte[] data = new byte[1024];
                        int len = 0;
                        try {
                            while ((len = inputStream.read(data)) != -1) {
                                byteArrayOutputStream.write(data, 0, len);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        resultData = new String(byteArrayOutputStream.toByteArray());
                        return "1";
                    } else {
                        RetryLogic(sourceFile);
                        Log.e(Camera2VideoFragment.class.getName(), "server response code for videoFile: " + serverResponseCode);
                        return "server response code: " + serverResponseCode;//test
                    }


                } catch (Exception ex) {
                    ex.printStackTrace();
                    RetryLogic(sourceFile);
                    return "connection error: " + ex.getMessage();//test
                }
            } // End else block
        }

        private String getUtcTime() {
            String format = "MM/dd/yyyy HH:mm:ss";
            final SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String utcTime = sdf.format(new Date());
            return utcTime;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onPostExecute(String result) {
            try {

                if (result == "1") {

                    Log.d("SUCCESS", "uploaded to mongo");
                    mSoundPool.play(soundID.get(3), 1, 1, 0, 0, 1);
                    Toast.makeText(getActivity(), "UPLOAD SUCCESSFUL", Toast.LENGTH_LONG).show();

                    //send out confirmation email
                    new EmailConfirmation().execute();

                    getActivity().findViewById(R.id.video_frame).setBackgroundResource(R.drawable.stroke_green);
                    if (getActivity().findViewById(R.id.btn_upload).getVisibility() == View.VISIBLE) {
                        getActivity().findViewById(R.id.btn_upload).setBackgroundResource(R.drawable.btn_report_green);
                    }
                } else {
                    mSoundPool.play(soundID.get(4), 1, 1, 0, 0, 1);
                    Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
                    Log.i(Camera2VideoFragment.class.getName(), "upload failed due to result:" + result);
                    getActivity().findViewById(R.id.video_frame).setBackgroundResource(R.drawable.stroke_red);
                    if (getActivity().findViewById(R.id.btn_upload).getVisibility() == View.VISIBLE) {
                        getActivity().findViewById(R.id.btn_upload).setBackgroundResource(R.drawable.btn_report_red);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private class EmailConfirmation extends AsyncTask<String, Integer, Void> {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            protected Void doInBackground(String... strings) {
                //report email
                JSONObject metadata = new JSONObject();
                try {
                    metadata.put("license_plate_number", "to be implemented");
                    metadata.put("num_approved_reviews", 0);
                    metadata.put("num_rejected_reviews", 0);
                    metadata.put("aggregate_review_score", 0);
                    metadata.put("incident_description", "to be implemented");
                    metadata.put("license_state", "to be implemented");
                    metadata.put("severity", 0);
                    metadata.put("location", videoUploadDataResponse.get("location_recorded"));
                    metadata.put("posting_account", videoUploadDataResponse.get("id"));
                    metadata.put("vehicle_descripton", "to be implemented");
                    metadata.put("date", "");
                    metadata.put("time", videoUploadDataResponse.get("time_of_recording"));
                    metadata.put("category", "uploaded");
                    metadata.put("reporter_name", phoneNumber);
                    metadata.put("captured_image", "");
                    List<Object> list = new ArrayList<>();
                    list.add(videoUploadDataResponse.get("id"));
                    metadata.put("review", new JSONArray(list));
                    metadata.put("video", new JSONArray(list));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    //URL url = new URL("http://18.218.120.121:9001/email/");
                    URL url = new URL("https://parallelagile.net/hosted/carmacam/carmacamhostedapi/baddriverreport/");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestProperty("Authorization", "Bearer " + jwt);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(metadata.toString());
                    wr.flush();

                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        Log.i("caizhixing", response.toString());
                    }
                } catch (Exception err) {
                    Log.i("anthony", "Could not send email, there was an error");
                    err.printStackTrace();
                }
                return null;
            }
        }


        private void RetryLogic(File input) {
            int threshold = 5;
            if (!new File(getActivity().getExternalFilesDir(null).getAbsolutePath() + "backup").exists()) {
                File dir = new File(getActivity().getExternalFilesDir(null).getAbsolutePath() + "backup");
                dir.mkdirs();
            }

            String filePath = getActivity().getExternalFilesDir(null).getAbsolutePath() + "backup" + "/Retry";
            try {
                int num = new File(getActivity().getExternalFilesDir(null).getAbsolutePath() + "backup").listFiles().length;
                if (num < threshold) {
                    File video = new File(filePath + String.valueOf(input.lastModified()) + ".mp4");
                    FileInputStream fis = new FileInputStream(input);
                    FileOutputStream fos = new FileOutputStream(video);
                    int bufferSize = Math.min(fis.available(), 1024 * 1024);
                    byte[] buffer = new byte[bufferSize];
                    int count = 0;
                    while ((count = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, bufferSize);
                        bufferSize = Math.min(fis.available(), 1024 * 1024);
                    }
                    fis.close();
                    fos.close();
                } else {
                    File[] children = new File(getActivity().getExternalFilesDir(null).getAbsolutePath() + "backup").listFiles();
                    long earliest = 0;
                    int mark = 0;
                    for (int i = 0; i < threshold; i++) {
                        if (i == 0) {
                            earliest = children[i].lastModified();
                            mark = i;
                        } else {
                            if (earliest > children[i].lastModified()) {
                                earliest = children[i].lastModified();
                                mark = i;
                            }
                        }
                    }
                    children[mark].delete();
                    File video = new File(filePath + String.valueOf(input.lastModified()) + ".mp4");
                    FileInputStream fis = new FileInputStream(input);
                    FileOutputStream fos = new FileOutputStream(video);
                    int bufferSize = Math.min(fis.available(), 1024 * 1024);
                    byte[] buffer = new byte[bufferSize];
                    int count = 0;
                    while ((count = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, bufferSize);
                        bufferSize = Math.min(fis.available(), 1024 * 1024);
                    }
                    fis.close();
                    fos.close();
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }


    public CameraViewManager getCameraView() {
        return cameraView;
    }


    public void modeChange() {
        this.videoMode = MainActivity.SCENE_MODE;
        updatePreview();
    }

    private void updateParam() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        hasUpdateParam = false;


        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    setZoom();
                    try {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        ;
                        mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpVideoMode() {
        videoMode = MainActivity.SCENE_MODE;
    }
}
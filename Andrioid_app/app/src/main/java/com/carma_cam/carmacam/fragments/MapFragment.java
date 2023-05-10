package com.carma_cam.carmacam.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.carma_cam.carmacam.R;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

public class MapFragment extends Fragment implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    MapView mapView;
    //ZoomView zoomView;
    View rootView;

    public static final float MAPVIEW_ALPHA = 0.7f;
    public static final float MAPVIEW_NOTRANSPARENT = 1.f;

    static final int DEFAULT_ZOOM = 16;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    boolean initView;

    int width, height;

    //Google Map
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!shouldCheckRuntimePermissions(getActivity())||checkAndRequestLocationPermission()){
            buildGoogleApiClient();
            initView = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.map_fragment, container, false);
        //zoomView = (ZoomView) rootView.findViewById(R.id.zoomed);
        mapView = (MapView) rootView.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        if(initView){
            //setAlpha(MAPVIEW_ALPHA);
            initView = false;
        }

        return rootView;
    }

    public void setAlpha(float alpha){
        mapView.setAlpha(alpha);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (mGoogleApiClient!=null&&mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        mapView.onPause();
        if (mGoogleApiClient!=null&&mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    /**
     * set map type
     * enable the location indication (the blue circle) on the map
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        try{
            mMap.setMyLocationEnabled(true);
            mMap.animateCamera(CameraUpdateFactory.zoomTo( 17.0f ));
            //set zoom controls
            rootView.findViewById(R.id.btn_zoom_in).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMap.animateCamera(CameraUpdateFactory.zoomIn());
                }
            });

            rootView.findViewById(R.id.btn_zoom_out).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMap.animateCamera(CameraUpdateFactory.zoomOut());
                }
            });
        }catch (SecurityException e){

        }
    }

    //Google Play service
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = (new LocationRequest())
                .setInterval(1000)
                .setFastestInterval(30000)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        startLocationUpdates();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Toast.makeText(this.getActivity(),
                "Google play service connection error" + result.getErrorCode(),
                Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this.getActivity(),
                "Google play service connection suspended, Trying to reconnect",
                Toast.LENGTH_SHORT)
                .show();
    }

    GoogleMap.SnapshotReadyCallback mapSnapCallback = new GoogleMap.SnapshotReadyCallback(){
        @Override
        public void onSnapshotReady(Bitmap snapshot)
        {
            if(snapshot!=null) {
                //zoomView.zoom(snapshot);
                Log.d("MAP FRAG","SS");
            }
        }
    };

    @Override
    public void onLocationChanged(Location location) {
        mMap.snapshot(mapSnapCallback);
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        //move map camera
        setCamera(latLng);
        mMap.setTrafficEnabled(true);
    }

    void setCamera(LatLng position){
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
    }

    protected void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }catch (SecurityException e){

        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    //permission request
    public boolean checkAndRequestLocationPermission(){
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // if the app has requested this permission previously
            // and the user denied the request
            // and the user didn't choose the 'Don't ask again'
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mGoogleApiClient == null) {
                        buildGoogleApiClient();
                    }
                } else {
                    Toast.makeText(getActivity(), "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    /* device is under Android 5.1 or target SDK is under 22, we need runtime request.
     * ====================
       target SDK |<23| 23|
       ====================
              |<23| X | X |
              ------------|
       OS SDK |23 | X | V |
       ====================
     *
    */
    public static boolean shouldCheckRuntimePermissions(Context context) {
        return
                isApplicationWithMarshmallowTargetSdkVersion(context) &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isApplicationWithMarshmallowTargetSdkVersion(Context context) {
        return
                context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.M;
    }

}
package com.carma_cam.carmacam.main;

import android.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.carma_cam.carmacam.R;
import com.carma_cam.carmacam.fragments.ViewChange;

public class CameraViewManager implements ViewChange {
    //Created by YUANFAN PENG
    //view related code
    public static final int INITIALIZING = 0,
            NOT_RECORDING = 1,
            RECORDING = 2,
            UPLOADING = 3,
            MAIN_VIEW_CHANGE = 4;

    public static final int MAIN_CAMERA = 0,
            MAIN_MAP = 1;

    int currentState;
    int mainView;
    Fragment fragment;

    Button button;
    View bottomMenu;

    public int getCurrentState() {
        return currentState;
    }

    public CameraViewManager(int initState, Fragment fragment) {
        this.fragment = fragment;
        currentState = initState;
    }

    @Override
    public int viewChange(int state) {
        View v = fragment.getView();
        if (state == MAIN_VIEW_CHANGE) {
            setView(v);//after change, view is re-inflated
            switch (mainView) {
                case MAIN_CAMERA:
                    mainView = MAIN_MAP;
                    button.setVisibility(View.GONE);
                    break;
                case MAIN_MAP:
                    mainView = MAIN_CAMERA;
                    button.setVisibility(View.VISIBLE);
                    break;
            }
            viewChange(currentState);
        } else {
            switch (state) {
                case INITIALIZING:
                    setView(v);
                    state = NOT_RECORDING;
                    mainView = MAIN_CAMERA;
                case NOT_RECORDING:
                    //button.setText(R.string.record);
                    //button.setBackgroundResource(R.drawable.btn_circle);
                    button.setBackgroundResource(R.drawable.rec);
                    break;
                case RECORDING:
                    button.setText("");
                    button.setBackgroundResource(R.drawable.warning);
                    break;
                case UPLOADING:
                    Toast.makeText(fragment.getActivity(), fragment.getText(R.string.upload), Toast.LENGTH_SHORT).show();
                    //button.setText(R.string.upload);
                    button.setBackgroundResource(R.drawable.btn_circle);
                    //recordSpot.setVisibility(ImageView.INVISIBLE);
                    break;
            }
            currentState = state;
        }
        return 0;
    }

    void setView(View v) {
        button = (Button) v.findViewById(R.id.video);
        bottomMenu = v.findViewById(R.id.cameraSideMenu);
    }
}

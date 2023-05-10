package com.carma_cam.carmacam.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by pengyuanfan on 4/20/2017.
 */

public class ZoomView extends View {
    private static final int FACTOR = 2;
    Bitmap zoomed = null;
    Matrix scaleMatrix;
    void init(){
        scaleMatrix = new Matrix();
        scaleMatrix.reset();
    }
    public ZoomView(Context context){
        super(context);init();
    }
    public ZoomView(Context context, AttributeSet attrs){
        super(context, attrs);init();
    }
    public ZoomView(Context context, AttributeSet attrs,int defStyleRes){
        super(context, attrs, defStyleRes);init();
    }

    public void setCenter(int width, int height){
        scaleMatrix.reset();
        scaleMatrix.setScale(FACTOR, FACTOR);
        scaleMatrix.postTranslate(width/2*(1-FACTOR),
                height/2*(1-FACTOR));
    }

    public void zoom(Bitmap original){
        zoomed = original;
        setCenter(original.getWidth(), original.getHeight());//cannot be preset, because of rotation
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(zoomed!=null) {
            canvas.drawBitmap(zoomed, scaleMatrix, null);
        }
    }
}

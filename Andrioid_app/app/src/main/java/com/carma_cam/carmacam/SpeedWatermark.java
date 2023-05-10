package com.carma_cam.carmacam;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SpeedWatermark {
    private static final String TAG = "SpeedWatermark";
    private static final int TIME_SPLIT = 3;
    private static final int MARKED_FRAMES_PER_SPLIT = 1;

    private File mFile;
    private double[] mSpeed;
    private int mEndIndex;
    private int mFrameRate;

    FFmpeg ffmpeg = FFmpeg.getInstance(MyApplication.getInstance());

    private void loadFFmpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    System.out.println("Failed!");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            System.out.println("Failed!");
        }
    }

    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    System.out.println("FAILED with output : " + s);
                }

                @Override
                public void onSuccess(String s) {
                    System.out.println("SUCCESS with output : " + s);
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + s);
                    System.out.println("progress : " + s);
                    System.out.println("Processing\n" + s);
                }

                @Override
                public void onStart() {
                    //outputLayout.removeAllViews();

                    Log.d(TAG, "Started command : ffmpeg ");
                    System.out.println("Processing...");
                    //progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg ");
                    //progressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    public SpeedWatermark(File videoFile, double[] speed, int endSpeedIdx, int frameRate) {
        mFile = videoFile;
        mSpeed = speed;
        mEndIndex = endSpeedIdx;
        mFrameRate = frameRate;
    }

    private class Watermark implements Runnable {
        private int id;

        public Watermark(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            String[] cmd =
                    {"-i", "/storage/emulated/0/Android/data/com.carma_cam.carmacam/files/output" + String.valueOf(id) + ".mp4",
                            "-vf", "drawtext=enable='between(n,0," + String.valueOf(MARKED_FRAMES_PER_SPLIT) +
                            ")':fontsize=50:fontfile=/storage/emulated/0/Android/data/com.carma_cam" +
                            ".carmacam/files/Helvetica-Regular.ttf:text='" +
                            (int) mSpeed[id * mFrameRate] + "mph':fontcolor=white:box=1:x=10:y=10:boxcolor=black@0.5", "-y",
                            "/storage/emulated/0/Android/data/com.carma_cam.carmacam/files/out" + String.valueOf(id) + ".mp4"};
//            String[] cmd = {"-i", "/storage/emulated/0/Android/data/com.carma_cam.carmacam/files/output" + String.valueOf(id)
//            + ".mp4", "-vf", "drawtext=fontsize=50:fontfile=/storage/emulated/0/Android/data/com.carma_cam
//            .carmacam/files/Helvetica-Regular.ttf:text='" + (int)mSpeed[id * mFrameRate] +
//            "mph':fontcolor=white:box=1:x=10:y=10:boxcolor=black@0.5", "-y", "/storage/emulated/0/Android/data/com.carma_cam
//            .carmacam/files/out" + String.valueOf(id) + ".mp4"};
            execFFmpegBinary(cmd);
        }
    }

    public void start() {
        Context context = MyApplication.getInstance();

        loadFFmpegBinary();

        for (int i = 0; i < mEndIndex / mFrameRate; i++) {
            if (i % TIME_SPLIT == 0) {
                String[] cmd = {"-ss", "00:00:" + String.format("%02d", i), "-t", String.valueOf(TIME_SPLIT - .01), "-i",
                        "/storage/emulated/0/Android/data/com.carma_cam.carmacam/files/UploadClip.mp4", "-c", "copy",
                        "/storage/emulated/0/Android/data/com.carma_cam.carmacam/files/output" + String.valueOf(i) + ".mp4"};
                execFFmpegBinary(cmd);
            }
        }

        Set<Thread> threadSet = new HashSet<>();

        for (int i = 0; i < mEndIndex / mFrameRate; i++) {
            if (i % TIME_SPLIT == 0) {
                Watermark waterMark = new Watermark(i);
                Thread myThread = new Thread(waterMark);
                threadSet.add(myThread);
                myThread.start();
            }
        }
        try {
            for (Thread mThread : threadSet) {
                mThread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            File root = new File(context.getExternalFilesDir(null).getAbsolutePath() + "/mylist.txt");
            FileWriter writer = new FileWriter(root);
            writer.append("file '" + context.getExternalFilesDir(null).getAbsolutePath() + "/out0.mp4'");
            for (int i = 1; i < mEndIndex / mFrameRate; i++) {
                if (i % TIME_SPLIT == 0) {
                    writer.append(
                            "\n" + "file '" + context.getExternalFilesDir(null).getAbsolutePath() + "/out" + String.valueOf(i) +
                                    ".mp4'");
                }
            }
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] cmd =
                {"-f", "concat", "-safe", "0", "-i", context.getExternalFilesDir(null).getAbsolutePath() + "/mylist.txt", "-y",
                        mFile.getPath()};
        execFFmpegBinary(cmd);

    }
}
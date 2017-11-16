package com.example.vanda.cardgame;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2
{

    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private static final String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Mat mRgba, imgGray, imgCanny;
    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this){
        @Override
        public void onManagerConnected(int status){
            switch(status){
                case BaseLoaderCallback.SUCCESS:{
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        javaCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    };


    @Override
    protected void onPause(){
        super.onPause();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded!\n");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0,this,mLoaderCallBack);

        }
        else{
            Log.d(TAG, "OpenCV loaded!\n");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
    }

    @Override
    public void onCameraViewStarted(int width, int height){
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        imgGray = new Mat(height, width, CvType.CV_8UC1);
        imgCanny = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped(){
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba = inputFrame.rgba();
        //Imgproc.cvtColor(mRgba, imgGray, Imgproc.COLOR_RGB2GRAY);
        return mRgba;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                Log.i("Camera", "G : " + grantResults[0]);
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    javaCameraView.enableView();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    System.exit(1);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request

        }
    }

}

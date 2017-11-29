package dcc.up.pt.cardgame;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by satyan on 11/29/17.
 * Fragment handling the camera
 */

public class CameraFragment extends Fragment implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = "Camera Fragment";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private int mMaxCardsCount = 0;

    private JavaCameraView mCameraView;
    private Mat imgThresh, imgGray, imgBlur, imgHierarchy;
    private Mat imgRGBA;
    private Map<OnCardsRecognisedListener, Integer> listeners = new ArrayMap<>();

    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(getContext()){
        @Override
        public void onManagerConnected(int status){
            switch(status){
                case BaseLoaderCallback.SUCCESS:{
                    if (ContextCompat.checkSelfPermission(getContext(),
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(),
                                PERMISSIONS,
                                CAMERA_PERMISSION_REQUEST);
                    } else {
                        mCameraView.enableView();
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mCameraView = view.findViewById(R.id.camera_view);
        mCameraView.setVisibility(SurfaceView.VISIBLE);
        mCameraView.setCvCameraViewListener(this);

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded!\n");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, getContext(), mLoaderCallBack);

        } else{
            Log.d(TAG, "OpenCV loaded!\n");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraView.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height){
        imgThresh = new Mat(height, width, CvType.CV_8UC1);
        imgRGBA = new Mat(height, width, CvType.CV_8UC3);
        imgGray = new Mat(height, width, CvType.CV_8UC1);
        imgBlur = new Mat(height, width, CvType.CV_8UC1);
        imgHierarchy = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped(){
        imgThresh.release();
        imgGray.release();
        imgBlur.release();
        imgRGBA.release();
        imgHierarchy.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        imgGray = inputFrame.gray();
        imgRGBA = inputFrame.rgba();

        // Reduction or noises
        Imgproc.GaussianBlur(imgGray, imgBlur, new Size(1,1), 1000);

        // Augmentation of the contrast (card are colorful against a white background)
        Imgproc.threshold(imgBlur, imgThresh,120, 255, Imgproc.THRESH_BINARY);

        // Identifying the contours
        List<MatOfPoint> contours = new LinkedList<>();
        Imgproc.findContours(imgThresh, contours,imgHierarchy,Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        // Sort them by size (card are typically the biggest)
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return -Double.compare(Imgproc.contourArea(o1),Imgproc.contourArea(o2));
            }
        });

        // Center each card
        List<Mat> cards = new ArrayList<>(mMaxCardsCount);

        for (int i = 0; i < mMaxCardsCount && i < contours.size(); i++) {
            MatOfPoint2f contour = new MatOfPoint2f();
            contours.get(i).convertTo(contour, CvType.CV_32FC2);
            Mat destMat = new Mat(imgRGBA.size(), imgRGBA.type());
            Size size = computeCard(contour, imgThresh, destMat, imgRGBA);

            cards.add(destMat);
            double width = size.width;
            double height = size.height;

            Log.d(TAG, String.format("Objects#%2d: %3.2fx%3.2f",i,width,height));
        }

        for (Map.Entry<OnCardsRecognisedListener, Integer> entry : listeners.entrySet()) {
            int expected = entry.getValue();
            int found = cards.size();
            OnCardsRecognisedListener listener = entry.getKey();
            if (found == 0){
                listener.noCards();
            } else if (found < expected){
                listener.partiallyRecognised(cards, expected - found);
            } else {
                // return the top mCards
                listener.recognised(cards.subList(0,expected));
            }
        }

        return imgRGBA;
    }

    private Size computeCard(MatOfPoint2f contour, Mat srcMat, Mat destMat, Mat debugMat){
        MatOfPoint2f approxCard = new MatOfPoint2f();

        double arcLength = Imgproc.arcLength(contour, true);
        Imgproc.approxPolyDP(contour, approxCard, arcLength*0.2,true);
        RotatedRect rec = Imgproc.minAreaRect(contour);
        Point[] vertices = new Point[4];
        Point[] pts = new Point[4];
        rec.points(pts);

        Point c = rec.center;
        boolean[] set = {false, false, false, false};
        // We place our point so that our rectangle is p0, p1, p2, p3
        for (Point p : pts) {
            if (p != null &&
                    p.x > 0 && p.x < srcMat.width()
                    && p.y > 0 && p.y < srcMat.height()) {
                if (p.x < c.x) {
                    if (p.y < c.y) {
                        vertices[0] = p;
                        set[0] = true;
                    } else {
                        vertices[3] = p;
                        set[3] = true;
                    }
                } else {
                    if (p.y < c.y) {
                        vertices[1] = p;
                        set[1] = true;
                    } else {
                        vertices[2] = p;
                        set[2] = true;
                    }
                }
            }
        }

        if (set[0] && set[1] && set[2] && set[3]) {

            Mat src = new MatOfPoint2f(
                    vertices[0],
                    vertices[1],
                    vertices[2],
                    vertices[3]
            );

            Mat dest =
                    new MatOfPoint2f(
                            new Point(0, 0),
                            new Point(destMat.width() - 1, 0),
                            new Point(destMat.width() - 1, destMat.height() - 1),
                            new Point(0, destMat.height() - 1));
            Mat transform = Imgproc.getPerspectiveTransform(src, dest);

            Imgproc.warpPerspective(srcMat, destMat, transform, destMat.size());

            if (debugMat != null) {
                List<MatOfPoint> boxContours = new ArrayList<>();
                boxContours.add(new MatOfPoint(vertices));
                Imgproc.drawContours(
                        debugMat,
                        boxContours,
                        0, new Scalar(255),
                        2);
            }
        }
        return rec.size;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST: {
                Log.i("Camera", "G : " + grantResults[0]);
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraView.enableView();
                } else {
                    getActivity().finishAndRemoveTask();
                }
            }
        }
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    public void addOnCardsRecognisedListener(OnCardsRecognisedListener listener, int numberExpected){
        listeners.put(listener, numberExpected);
        updateMaxCard();
    }

    public void removeOnCardsRecognisedListener(OnCardsRecognisedListener listener){
        listeners.remove(listener);
        updateMaxCard();
    }

    private void updateMaxCard(){
        int max = 0;
        // recognise the maximum among listeners
        for (Integer count : listeners.values()) {
            if (count > max){
                max = count;
            }
        }
        mMaxCardsCount = max;
    }
}

package dcc.up.pt.cardgame;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dcc.up.pt.cardgame.listener.OnCardsRecognisedListener;

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

    public final static int MODE_PREVIEW = 0;
    public final static int MODE_PREVIEW_BORDER = 1;
    private static final int PREVIEW_CAMERA = 0;
    private static final int PREVIEW_IMAGE = 1;

    private int mPreviewMode = MODE_PREVIEW;
    private int mCardCount = 0;

    private JavaCameraView mCameraView;
    private ImageView mPreviewView;
    private Mat imgGray;
    private Mat imgRGBA;
    private Mat imgCards;
    private OnCardsRecognisedListener mOnCardRecognisedListener;

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

    public CameraFragment() {
    }

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


        mPreviewView = view.findViewById(R.id.image_preview);

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded!\n");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, getContext(), mLoaderCallBack);

        } else{
            Log.d(TAG, "OpenCV loaded!\n");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mPreviewMode = savedInstanceState.getInt("var_preview_mode", MODE_PREVIEW);
            mCardCount = savedInstanceState.getInt("var_card_count", 0);
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
    public void onCameraViewStarted(int width, int height){}

    @Override
    public void onCameraViewStopped(){
        imgGray.release();
        imgRGBA.release();
        imgCards.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        imgRGBA = inputFrame.rgba();
        imgGray = inputFrame.gray();
        imgCards = imgRGBA.clone();

        // if we are not supposed to recognise cards, no need to use resources
        if (mCardCount > 0) {
            List<Mat> cards = new ArrayList<>(mCardCount);
            List<Mat> previews = new ArrayList<>(mCardCount);
            long[] cardsAddrs = new long[mCardCount];
            long[] previewsAddrs = new long[mCardCount];
            float[] cardsX = new  float[mCardCount];
            for (int i = 0; i < mCardCount; i++) {
                Mat mat = new Mat(new Size(480,480), imgRGBA.type());
                Mat preview = new Mat(new Size(480,480), imgGray.type());
                cards.add(mat);
                previews.add(preview);
                previewsAddrs[i] = preview.getNativeObjAddr();
                cardsAddrs[i] = mat.getNativeObjAddr();
            }

            int found = findCards(
                    imgGray.getNativeObjAddr(),
                    imgRGBA.getNativeObjAddr(),
                    imgCards.getNativeObjAddr(),
                    cardsAddrs, previewsAddrs, cardsX);

            // Dirty dirty...
            List<MatPos> matPos = new ArrayList<>(mCardCount);
            for (int i = 0; i < found; i++) {
                Log.d(TAG, "onCameraFrame: ");
                MatPos pos = new MatPos(cards.get(i), cardsX[i]);
                matPos.add(pos);
            }
            cards.clear();

            Collections.sort(matPos, MatPos.COMPARATOR);
            for (MatPos m : matPos) {
                cards.add(m.mat);
            }

            matPos.clear();
            for (int i = 0; i < found; i++) {
                MatPos pos = new MatPos(previews.get(i), cardsX[i]);
                matPos.add(pos);
            }
            previews.clear();

            Collections.sort(matPos, MatPos.COMPARATOR);
            for (MatPos m : matPos) {
                previews.add(m.mat);
            }

            if (found != 0)
            Log.d(TAG, "onCameraFrame: " + found);

            if (mOnCardRecognisedListener != null) {
                if (found == 0){
                    mOnCardRecognisedListener.noCards();
                }else if (found == mCardCount) {
                    mOnCardRecognisedListener.recognised(cards, previews.get(0));
                } else if (found < mCardCount) {
                    mOnCardRecognisedListener.partiallyRecognised(cards, mCardCount - found);
                }
            }

            switch (mPreviewMode){
                case MODE_PREVIEW:
                    return imgRGBA;
                case MODE_PREVIEW_BORDER:
                    return imgCards;
            }
        }

        return imgRGBA;
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

    public void setOnCardsRecognisedListener(OnCardsRecognisedListener listener, int numberExpected){
        mOnCardRecognisedListener = listener;
        mCardCount = numberExpected;
        switchPreview(PREVIEW_CAMERA);
    }

    private void switchPreview(int preview) {
        switch (preview){
            case PREVIEW_CAMERA:
                mCameraView.enableView();
                mCameraView.setVisibility(View.VISIBLE);
                mPreviewView.setVisibility(View.GONE);
                break;
            case PREVIEW_IMAGE:
                mCameraView.disableView();
                mCameraView.setVisibility(View.GONE);
                mPreviewView.setVisibility(View.VISIBLE);
                break;
            default: switchPreview(PREVIEW_CAMERA);
                break;
        }
    }

    public void clearOnCardsRecognisedListener(){
        mOnCardRecognisedListener = null;
        mCardCount = 0;

    }

    public void clearOnCardsRecognisedListener(Mat preview){
        clearOnCardsRecognisedListener();
        switchPreview(PREVIEW_IMAGE);
        preview(preview, mPreviewView);
    }

    public static void preview(Mat m, ImageView imgView) {

        Log.d(TAG, "preview: " + m.width() + " " + m.height() + " " + m.size());

        Bitmap resultBitmap = Bitmap.createBitmap(m.cols(),  m.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, resultBitmap);
        imgView.setImageBitmap(resultBitmap);
    }


    public void setPreviewMode(int mode) {
        if (mode > MODE_PREVIEW_BORDER){
            mode = 0;
        }
        mPreviewMode = mode;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("var_card_count", mCardCount);
        outState.putInt("var_preview_mode",mPreviewMode);
    }

    public native int findCards(long addrGray, long addrRgba, long addrCards, 
                                long[] cardAddrs, long[] previewAddrs, float[] cardsX);

    private static class MatPos {
        Mat mat;
        float x;

        static final Comparator<MatPos> COMPARATOR = new Comparator<MatPos>() {
            @Override
            public int compare(MatPos o1, MatPos o2) {
                return Float.compare(o1.x, o2.x);
            }
        };

        public MatPos(Mat mat, float x) {
            this.mat = mat;
            this.x = x;
        }
    }
}

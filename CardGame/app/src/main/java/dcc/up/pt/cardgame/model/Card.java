package dcc.up.pt.cardgame.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

import dcc.up.pt.cardgame.R;

/**
 * Created by satyan on 11/30/17.
 * A card
 */

public class Card implements Comparable<Card> {
    /*public final static String[] CARD_VALUES = new String[]{
            "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "V", "D", "R"
    };*/
    public final static String[] CARD_VALUES = new String[]{
            "A", "2", "3", "4", "5"
    };

    /*public final static int[] CARD_SUITS = new int[] {
            R.string.card_diamonds,
            R.string.card_hearts,
            R.string.card_clubs,
            R.string.card_spades
    };*/
    public final static int[] CARD_SUITS = new int[] {
            R.string.card_diamonds
    };
    private static final String TAG = "Card";

    private final int mSuit;
    private final int mValue;
    private Mat mMatrix;

    public Card(Mat mat, int value, int suit) {
        this.mSuit = suit;
        this.mValue = value;
        this.mMatrix = mat;
    }

    public Card(int value, int suit) {
        this.mSuit = suit;
        this.mValue = value;
    }

    public void setMatrix(Mat mMatrix) {
        this.mMatrix = mMatrix;
    }

    public int getSuit() {
        return mSuit;
    }

    public int getValue() {
        return mValue;
    }

    public String toString(Context context){
        return context.getString(
                R.string.card_value,
                CARD_VALUES[mValue], context.getString(CARD_SUITS[mSuit]));
    }

    public static String card2String(Context context, int value, int suit){
        return context.getString(
                R.string.card_value,
                CARD_VALUES[value], context.getString(CARD_SUITS[suit]));
    }

    @Override
    public int compareTo(@NonNull Card o) {
        return Integer.compare(mValue, o.getValue());
    }

    /**
     * Try if the card's {@link Mat} is similar enough to a given {@link Mat}
     * @param mat {@link Mat} to compare
     * @return true if cards matches
     */
    public double matches(Mat mat){
        Mat diffMat = mMatrix.clone();
        Mat threshMat = mMatrix.clone();
        Mat m0, m1;
        m0 = new Mat(mat.size(), CvType.CV_8UC1);
        m1 = new Mat(mat.size(), CvType.CV_8UC1);

        clearMat(mat, m0);
        clearMat(mMatrix, m1);

        Log.d(TAG, "sizes: " + m0.size() + " " + m0.size());
        Log.d(TAG, "format: " + m1.type() + " " + m1.type());

        Core.subtract(m0, m1, diffMat);
        Imgproc.threshold(diffMat, threshMat, 200, 255, Imgproc.THRESH_BINARY);
        Scalar res =  Core.sumElems(threshMat);

        diffMat.release();
        threshMat.release();
        m0.release();
        m1.release();

        return res.val[0];
    }

    private void clearMat(Mat src, Mat dest){
        Mat gray = new Mat(src.size(), CvType.CV_8UC1);
        Mat blur = new Mat(src.size(), CvType.CV_8UC1);
        Mat thresh = new Mat(src.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(gray, blur,new Size(5, 5), 2);
        Imgproc.adaptiveThreshold(
                blur, thresh, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,
                11, 1
        );
        Imgproc.GaussianBlur(thresh, dest,new Size(5, 5), 2);

        blur.release();
        thresh.release();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Card
                && ((Card) obj).getSuit() == mSuit
                && ((Card) obj).getValue() == getValue();
    }

    public boolean hasMat() {
        return mMatrix != null;
    }
}

package dcc.up.pt.cardgame;

import org.opencv.core.Mat;

import java.util.List;

/**
 * Created by satyan on 11/29/17.
 *
 */

public interface OnCardsRecognisedListener {
    void recognised(List<Mat> cards);
    void partiallyRecognised(List<Mat> cards, int missing);
    void noCards();
}

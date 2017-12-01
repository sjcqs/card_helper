#include "native-lib.h"

using namespace cv;
using namespace std;

extern "C" {

Mat mat_blur, mat_thresh, mat_hierarchy;

// comparison function object
bool compareAreasReverse(vector<Point> contour1, vector<Point> contour2) {
    double i = fabs( contourArea(Mat(contour1)) );
    double j = fabs( contourArea(Mat(contour2)) );
    
    return ( i > j );
}

JNIEXPORT jint JNICALL Java_dcc_up_pt_cardgame_CameraFragment_findCards(
        JNIEnv* env, jobject, jlong addrGray, jlong addrRgba, jlong addrCards,
        jlongArray cardsAddrs, jfloatArray cardsX){
    Mat& mat_gray = *(Mat*)addrGray;
    Mat& mat_rgba = *(Mat*)addrRgba;
    Mat& mat_cards = *(Mat*)addrCards;

    jsize cards_count = env->GetArrayLength(cardsAddrs);
    jlong* cards_addresses = env->GetLongArrayElements(cardsAddrs, 0);
    jfloat* cards_x = env->GetFloatArrayElements(cardsX, 0);

    GaussianBlur(mat_gray,mat_blur, Size(1, 1), 1000);
    threshold(mat_blur, mat_thresh, 120, 255, THRESH_BINARY);

    vector< Mat > contours;
    findContours(mat_thresh, contours, mat_hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE, Point(0, 0));

    sort(contours.begin(), contours.end(), compareAreasReverse);

    double averageArea = fabs(contourArea(contours[0]));
    vector< Mat > contours_rect (contours.size());
    int count = 0;
    for (int i = 0; i < cards_count && i < contours.size(); ++i) {
        Mat contour = contours[i];
        double  area = fabs(contourArea(contour));
        if (area >= 0.90*averageArea) {
            averageArea = (averageArea + area) / 2.0;
            Mat &card = *(Mat *) cards_addresses[i];

            double arc_length = arcLength(contour, true);
            approxPolyDP(contour, contours_rect[i], arc_length * 0.1, true);
            RotatedRect rect = minAreaRect(contour);

            Point2f pts[4];
            rect.points(pts);

            Point2f src[4];
            bool set[4] = {false};
            Point2f c = rect.center;
            for (int j = 0; j < 4; ++j) {
                Point2f p = pts[j];
                if (p.x > 0 && p.x < mat_rgba.size[0]
                    && p.y > 0 && p.y < mat_rgba.size[1]) {
                    if (p.x < c.x) {
                        if (p.y < c.y) {
                            src[0] = p;
                            set[0] = true;
                        } else {
                            src[3] = p;
                            set[3] = true;
                        }
                    } else {
                        if (p.y < c.y) {
                            src[1] = p;
                            set[1] = true;
                        } else {
                            src[2] = p;
                            set[2] = true;
                        }
                    }
                }
            }

            if (set[0] && set[1] && set[2] && set[3]) {

                Point2f dest[4];
                dest[0] = Point2f(0, 0);
                dest[1] = Point2f(card.size[0] - 1, 0);
                dest[2] = Point2f(card.size[0] - 1, card.size[1] - 1);
                dest[3] = Point2f(0, card.size[1] - 1);

                Mat trans = getPerspectiveTransform(src, dest);
                Mat warp;

                warpPerspective(mat_rgba, warp, trans, Size(card.size[0], card.size[1]));

                if (rect.boundingRect().width > rect.boundingRect().height) {
                    rotate(warp, card, ROTATE_90_CLOCKWISE);
                } else {
                    warp.copyTo(card);
                }
                CvScalar color(255);
                line(mat_cards, src[0], src[1], color, 2);
                line(mat_cards, src[1], src[2], color, 2);
                line(mat_cards, src[2], src[3], color, 2);
                line(mat_cards, src[3], src[0], color, 2);
                cards_x[i] = src[0].x;
                count++;
            }
        }
    }
    return count;
}

}

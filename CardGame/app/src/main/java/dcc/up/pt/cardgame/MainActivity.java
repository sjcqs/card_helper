package dcc.up.pt.cardgame;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.opencv.core.Mat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private int mCards = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CameraFragment cameraFragment = CameraFragment.newInstance();
        final TextView statusText = findViewById(R.id.text_status);



        final OnCardsRecognisedListener listener = new OnCardsRecognisedListener() {
            @Override
            public void recognised(List<Mat> cards) {
                Log.d(TAG, "recognised");
            }

            @Override
            public void partiallyRecognised(List<Mat> cards, final int missing) {
                Log.d(TAG, "partiallyRecognised");
            }

            @Override
            public void noCards() {
                Log.d(TAG, "noCards");
            }
        };

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, cameraFragment)
                .commit();
    }

}

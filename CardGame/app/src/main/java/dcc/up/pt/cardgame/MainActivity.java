package dcc.up.pt.cardgame;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.core.Mat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private int mCards = 0;
    private Status status = Status.NOT_RECOGNISE;

    enum Status{
        RECOGNISE,
        PARTIAL,
        NOT_RECOGNISE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CameraFragment cameraFragment = CameraFragment.newInstance();
        final TextView statusText = findViewById(R.id.status);



        final OnCardsRecognisedListener listener = new OnCardsRecognisedListener() {
            @Override
            public void recognised(List<Mat> cards) {
                Log.d(TAG, "recognised");
                if (status != Status.RECOGNISE) {
                    status = Status.RECOGNISE;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("Recognised");
                        }
                    });
                }
            }

            @Override
            public void partiallyRecognised(List<Mat> cards, final int missing) {
                Log.d(TAG, "partiallyRecognised");
                if (status != Status.PARTIAL) {
                    status = Status.PARTIAL;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("Partially recognised, missing: " + missing);
                        }
                    });
                }
            }

            @Override
            public void noCards() {
                Log.d(TAG, "noCards");
                if (status != Status.NOT_RECOGNISE) {
                    status = Status.NOT_RECOGNISE;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("No cards");
                        }
                    });
                }
            }
        };

        SeekBar seekBar = findViewById(R.id.spin_bar);
        seekBar.setMax(8);
        final TextView cardNumber = findViewById(R.id.card_number);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                mCards = progress;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cardNumber.setText(String.format("%2d",progress));
                    }
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mCards == 0){
                    cameraFragment.removeOnCardsRecognisedListener(listener);
                } else {
                    cameraFragment.addOnCardsRecognisedListener(listener, mCards);
                }
            }
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, cameraFragment)
                .commit();
    }

}

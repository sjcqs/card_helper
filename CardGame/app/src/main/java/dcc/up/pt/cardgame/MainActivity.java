package dcc.up.pt.cardgame;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.Mat;

import java.util.List;
import java.util.Locale;

import dcc.up.pt.cardgame.listener.OnCardsRecognisedListener;
import dcc.up.pt.cardgame.model.Card;
import dcc.up.pt.cardgame.model.Deck;

public class MainActivity extends AppCompatActivity implements CardDialog.CardDialogListener {
    private static final String TAG = "MainActivity";
    private final static int STATE_START = 0;
    private final static int STATE_REGISTER = 1;
    private final static int STATE_PLAY = 2;

    private int mState = STATE_START;
    private StartFragment mStartFragment;
    private RegisterFragment mRegisterFragment;
    private PlayFragment mPlayFragment;
    private CameraFragment mCameraFragment;
    private Fragment mCurrentFragment;
    private List<Mat> mRecognizedCards;
    private Deck mDeck = new Deck();
    private OnCardsRecognisedListener mRegisterCardsRecognisedListener = new OnCardsRecognisedListener() {
        @Override
        public void recognised(List<Mat> cards) {
            mRecognizedCards = cards;
        }

        @Override
        public void partiallyRecognised(List<Mat> cards, int missing) {
            // technically impossible
            mRecognizedCards = cards;
        }

        @Override
        public void noCards() {
            mRecognizedCards = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.loadLibrary("native-lib");
        setContentView(R.layout.activity_main);

        mCameraFragment = CameraFragment.newInstance();
        mStartFragment = StartFragment.newInstance();
        mRegisterFragment = RegisterFragment.newInstance();
        mPlayFragment = PlayFragment.newInstance();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.layout_container, mCameraFragment)
                .commit();

        changeState(STATE_START);
    }

    private void changeState(int state) {
        mState = state;
        switch (state){
            case STATE_START:
                Log.d(TAG, "Start state");
                switchFragment(mStartFragment);
                displayStart();
                break;
            case STATE_REGISTER:
                Log.d(TAG, "Register state");
                switchFragment(mRegisterFragment);
                displayRegister();
                break;
            case STATE_PLAY:
                Log.d(TAG, "Play state");
                switchFragment(mPlayFragment);
                displayPlay();
                break;
        }
    }

    private void displayStart() {
        mCameraFragment.setPreviewMode(CameraFragment.MODE_PREVIEW);
        mStartFragment.setOnStartListener(new StartFragment.OnStartListener() {
            @Override
            public void onStart() {
                changeState(STATE_REGISTER);
            }
        });
    }

    private void displayRegister() {
        mCameraFragment.setPreviewMode(CameraFragment.MODE_PREVIEW_BORDER);
        mRegisterFragment.setOnRegistrationListener(new RegisterFragment.OnRegistrationListener() {
            @Override
            public void onCardRegistered(int value, int suit) {
                if (mRecognizedCards != null && mRecognizedCards.size() > 0) {
                    Mat preview = mRecognizedCards.get(0);
                    mCameraFragment.clearOnCardsRecognisedListener(preview);
                    Log.d(TAG, "onCardRegistered: "
                            + String.format(Locale.ENGLISH, "Card: %2d %d", value, suit));
                    DialogFragment fragment = new CardDialog();
                    Bundle args = new Bundle();
                    args.putInt("suit", suit);
                    args.putInt("value", value);
                    fragment.setArguments(args);
                    fragment.show(getSupportFragmentManager(), "card_recognized");
                }
            }

            @Override
            public void onReset() {
                Log.d(TAG, "onReset: ");
                mRegisterFragment.reset();
                mDeck.clear();
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish:");
                if (!mDeck.completed()){
                    Toast.makeText(getApplication(),"Missing cards.", Toast.LENGTH_SHORT)
                            .show();
                    mDeck.clear();
                    mRegisterFragment.reset();
                    mCameraFragment.clearOnCardsRecognisedListener();
                }
                changeState(STATE_PLAY);
            }
        });
        mCameraFragment.setOnCardsRecognisedListener(
                mRegisterCardsRecognisedListener,
                1);
    }
    private void displayPlay() {
        mCameraFragment.setPreviewMode(CameraFragment.MODE_PREVIEW_BORDER);
        OnCardsRecognisedListener listener = new OnCardsRecognisedListener() {
            @Override
            public void recognised(final List<Mat> cards) {
                mCameraFragment.clearOnCardsRecognisedListener();
                Thread bgThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int index = 0;
                        for (Mat cardMat : cards) {
                            Log.d(TAG, "recognised: " + cardMat.total());
                            Card card = mDeck.matches(cardMat);
                            if (card != null){
                                String str = card.toString(getApplication());
                                Log.d(TAG, "recognised: "+ str);
                                mPlayFragment.setResult(str, index);
                                index++;
                            }
                        }
                    }
                });
                bgThread.run();
                try {
                    bgThread.join();
                } catch (InterruptedException ignored) {
                }
                mCameraFragment.setOnCardsRecognisedListener(this, 2);

            }

            @Override
            public void partiallyRecognised(List<Mat> cards, int missing) {
            }

            @Override
            public void noCards() {
            }
        };
        mCameraFragment.setOnCardsRecognisedListener(listener, 2);
    }

    private void switchFragment(Fragment fragment) {
        mCurrentFragment = fragment;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_action, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("var_state",mState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mState = savedInstanceState.getInt("var_state", STATE_START);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mCurrentFragment.equals(mStartFragment)){
            displayStart();
            mCurrentFragment = mStartFragment;
        }else if (mCurrentFragment.equals(mPlayFragment)){
            displayRegister();
            mCurrentFragment = mRegisterFragment;
        } else if(mCurrentFragment.equals(mRegisterFragment)){
            displayStart();
            mCurrentFragment = mStartFragment;
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int value, int suit) {
        Card card = new Card(mRecognizedCards.get(0),value, suit);
        if (!mDeck.add(card)){
            Toast.makeText(
                    getApplication(),
                    getString(R.string.not_registered,
                            Card.card2String(getApplication(),value, suit)),
                    Toast.LENGTH_SHORT).show();
        } else {
            mRegisterFragment.next();
        }
        mCameraFragment.setOnCardsRecognisedListener(mRegisterCardsRecognisedListener, 1);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        mCameraFragment.setOnCardsRecognisedListener(mRegisterCardsRecognisedListener, 1);
    }
}

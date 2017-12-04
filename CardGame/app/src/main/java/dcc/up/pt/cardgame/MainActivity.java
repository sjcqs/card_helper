package dcc.up.pt.cardgame;

import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import dcc.up.pt.cardgame.listener.OnCardsRecognisedListener;
import dcc.up.pt.cardgame.model.Card;
import dcc.up.pt.cardgame.model.Deck;

public class MainActivity extends AppCompatActivity implements CardDialog.CardDialogListener,
        PlayFragment.PlayModeListener{
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
    private TextView mInstruction;
    private List<Mat> mRecognizedCards;
    private Deck mDeck = null;
    private TextToSpeech toSpeech;
    private Mat mPreview;
    private OnCardsRecognisedListener mRegisterCardsRecognisedListener = new OnCardsRecognisedListener() {
        @Override
        public void recognised(List<Mat> cards, Mat preview) {
            mRecognizedCards = cards;
            mPreview = preview;
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

    private final static int CARDS_TO_TREAT = 50;

    private int mAddedCards = 0;
    private BlockingQueue<List<Mat>> mCardsQueue = new ArrayBlockingQueue<>(CARDS_TO_TREAT);
    private List<Card[]> mResults = new ArrayList<>(CARDS_TO_TREAT);
    private boolean mAutoPlay = false;

    private Thread mPlayWorker = new Thread(new Runnable() {
        private boolean running = true;

        @Override
        public void run() {
            List<Mat> cardMats;
            while (running) {
                try {
                    cardMats = mCardsQueue.poll(1000L, TimeUnit.MILLISECONDS);
                    if (cardMats != null) {
                        int index = 0;
                        Card cards[] = new Card[cardMats.size()];
                        for (Mat cardMat : cardMats) {
                            Log.d(TAG, "matches: " + index);
                            Card card = mDeck.matches(cardMat);
                            if (card != null) {
                                cards[index] = card;
                                index++;
                            }
                        }

                        if (cards.length == 2) {
                            StringBuilder str = new StringBuilder("Results: ");
                            for (Card card : cards) {
                                str.append(card.toString(getApplication())).append(", ");
                            }
                            long curr = System.currentTimeMillis();
                            Log.d(TAG, mIndex + " " + str.toString() + " " + (curr - mLast));
                            mIndex++;
                            mLast = curr;
                            mResults.add(cards);
                        } else {
                            Log.d(TAG, "run: No enough cards");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    });

    private OnCardsRecognisedListener mPlayCardsListener = new OnCardsRecognisedListener() {
        @Override
        public void recognised(final List<Mat> cardMats, Mat preview) {
            if (mAddedCards < CARDS_TO_TREAT) {
                mAddedCards++;
                mCardsQueue.offer(cardMats);
            } else {
                mCameraFragment.clearOnCardsRecognisedListener();

                new  Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (mResults.size() < CARDS_TO_TREAT){
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        if (mResults.size() >= CARDS_TO_TREAT) {
                            Log.d(TAG, "recognised: " + mResults.size());
                            int values[] = new int[mDeck.getValueCount()];
                            int values1[] = new int[mDeck.getValueCount()];
                            int suits[] = new int[mDeck.getSuitCount()];
                            int suits1[] = new int[mDeck.getSuitCount()];

                            for (Card[] result : mResults) {
                                Card card = result[0];
                                Card card1 = result[1];

                                Log.d(TAG, "recognised: "
                                        + card.toString(getApplication())
                                        + " "
                                        + card1.toString(getApplication()));

                                values[card.getValue()]++;
                                suits[card.getSuit()]++;

                                values1[card1.getValue()]++;
                                suits1[card1.getSuit()]++;
                            }
                            int value = 0, value1 = 0;
                            int max, max1;
                            max = 0;
                            max1 = 0;
                            for (int i = 0; i < mDeck.getValueCount(); i++) {
                                if (max <= values[i]) {
                                    max = values[i];
                                    value = i;
                                }
                                if (max1 <= values1[i]) {
                                    max1 = values1[i];
                                    value1 = i;
                                }
                            }
                            int suit = 0, suit1 = 0;
                            max = 0;
                            max1 = 0;
                            for (int i = 0; i < mDeck.getSuitCount(); i++) {
                                if (max <= suits[i]) {
                                    max = suits[i];
                                    suit = i;
                                }
                                if (max1 <= suits1[i]) {
                                    max1 = suits1[i];
                                    suit1 = i;
                                }
                            }

                            mPlayFragment.setResult(Card.card2String(getApplication(), value, suit), 0);
                            mPlayFragment.setResult(Card.card2String(getApplication(), value1, suit1), 1);
                            mPlayFragment.enableButton();

                            if (value > value1){
                                toSpeech.speak("Left player wins",TextToSpeech.QUEUE_FLUSH,null, Thread.currentThread().getName());
                            } else if(value < value1){
                                toSpeech.speak("Right player wins",TextToSpeech.QUEUE_FLUSH,null, Thread.currentThread().getName());
                            } else {
                                toSpeech.speak("It's a draw",TextToSpeech.QUEUE_FLUSH,null, Thread.currentThread().getName());
                            }

                            mResults.clear();
                            setInstruction(R.string.done);
                            mAddedCards = 0;

                            if (mAutoPlay){
                                // Wait three second before next turn
                                try {
                                    Thread.sleep(3000);
                                    mCameraFragment.setOnCardsRecognisedListener(mPlayCardsListener, 2);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                }).start();
            }
        }

        @Override
        public void partiallyRecognised(List<Mat> cards, int missing) {
        }

        @Override
        public void noCards() {
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
        mInstruction = findViewById(R.id.text_instruction);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.layout_container, mCameraFragment)
                .commit();

        toSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    toSpeech.setLanguage(Locale.UK);
                }
            }
        });

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
            public void onStart(int valueCount, int suitCount) {
                mDeck = new Deck(valueCount, suitCount);
                Bundle args = new Bundle();
                args.putInt("value_count", valueCount);
                args.putInt("suit_count", suitCount);
                mRegisterFragment.setArguments(args);
                changeState(STATE_REGISTER);
            }
        });
    }

    private void displayRegister() {
        mDeck.clear();
        mCameraFragment.setPreviewMode(CameraFragment.MODE_PREVIEW_BORDER);
        mRegisterFragment.setOnRegistrationListener(new RegisterFragment.OnRegistrationListener() {
            @Override
            public void onCardRegistered(int value, int suit) {
                if (mRecognizedCards != null && mRecognizedCards.size() > 0) {
                    Mat preview = mPreview;
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
                } else {
                    mCameraFragment.clearOnCardsRecognisedListener();
                    changeState(STATE_PLAY);
                }
            }
        });
        mCameraFragment.setOnCardsRecognisedListener(
                mRegisterCardsRecognisedListener,
                1);
    }

    private int mIndex = 0;
    private long mLast = System.currentTimeMillis();
    private void displayPlay() {
        mCameraFragment.setPreviewMode(CameraFragment.MODE_PREVIEW_BORDER);
        if (!mPlayWorker.isAlive()){
            mPlayWorker.start();
        }
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
            mRegisterFragment.reset();
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

    @Override
    public void onSwitchMode(boolean isAuto) {
        mAutoPlay = isAuto;
        if (isAuto){
            mCameraFragment.setOnCardsRecognisedListener(mPlayCardsListener, 2);
        } else {
            mCameraFragment.clearOnCardsRecognisedListener();
        }
    }

    @Override
    public void onNextTurn() {
        mCameraFragment.setOnCardsRecognisedListener(mPlayCardsListener, 2);
        mPlayFragment.disableButton();
        setInstruction(R.string.wait);
    }

    private void setInstruction(@StringRes final int res, final Object... args){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInstruction.setText(getString(res, args));
            }
        });
    }
}

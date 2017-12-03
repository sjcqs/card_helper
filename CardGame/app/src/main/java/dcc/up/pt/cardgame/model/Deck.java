package dcc.up.pt.cardgame.model;

import android.util.ArraySet;
import android.util.Log;

import org.opencv.core.Mat;

import java.util.Set;

import dcc.up.pt.cardgame.R;

/**
 * Created by satyan on 11/30/17.
 * Contains the {@link Card} and allows to look for one.
 */

public class Deck {
    private static final String TAG = "Deck";
    public final static int[] CARD_SUITS = new int[] {
            R.string.card_diamonds,
            R.string.card_spades,
            R.string.card_hearts,
            R.string.card_clubs
    };
    public final static String[] CARD_VALUES = new String[]{
            "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "V", "D", "R"
    };

    private boolean[][] mCardRegistered;

    private Set<Card> mCards;

    private final int mValueCount;
    private final int mSuitCount;

    public Deck(int valueCount, int suitCount) {
        if (valueCount < 0 || valueCount > 13 || suitCount < 0 || suitCount > 4){
            throw new IndexOutOfBoundsException("Suits: 0-4; Values: 0-13");
        }
        this.mValueCount = valueCount;
        this.mSuitCount = suitCount;

        mCardRegistered = new boolean[suitCount][valueCount];
        mCards = new ArraySet<>(suitCount* valueCount);
    }

    public Card matches(Mat mat){
        double max = Double.MAX_VALUE;
        Card bestCard = null;
        for (Card card : mCards) {
            double value = card.matches(mat);
            Log.d(TAG, "matches: " + card.getSuit() + ";" + card.getValue() + ": " + value);
            if (value < max){
                max = value;
                bestCard = card;
            }
            if (value == 0){
                return card;
            }
        }
        Log.d(TAG,
                String.format(
                        "%d %d -> matches: %f",
                        bestCard.getValue(), bestCard.getSuit(), max));
        return bestCard;
    }

    public boolean completed(){
        return mCards.size() == mSuitCount * mValueCount;
    }

    public Set<Card> missingCards(){
        Set<Card> missing = new ArraySet<>();
        for (int i = 0; i < mSuitCount; i++) {
            for (int j = 0; j < mValueCount; j++) {
                if (!mCardRegistered[i][j]){
                    missing.add(new Card(i,j));
                }
            }
        }
        return missing;
    }

    public int getValueCount() {
        return mValueCount;
    }

    public int getSuitCount() {
        return mSuitCount;
    }

    public void clear(){
        for (int i = 0; i < mSuitCount; i++) {
            for (int j = 0; j < mValueCount; j++) {
                mCardRegistered[i][j] = false;
            }
        }
        mCards.clear();
    }

    public boolean add(Card card){
        if (!card.hasMat()){
            return false;
        }
        mCardRegistered[card.getSuit()][card.getValue()] = true;
        mCards.add(card);
        return true;
    }
}

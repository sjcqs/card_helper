package dcc.up.pt.cardgame.model;

import android.util.ArraySet;
import android.util.Log;

import org.opencv.core.Mat;

import java.util.Set;

/**
 * Created by satyan on 11/30/17.
 */

public class Deck {
    private static final String TAG = "Deck";
    private boolean[][] mCardRegistered =
            new boolean[Card.CARD_SUITS.length][Card.CARD_VALUES.length];
    private Set<Card> mCards =
            new ArraySet<>(Card.CARD_SUITS.length*Card.CARD_VALUES.length);

    public Card matches(Mat mat){
        double max = Double.MAX_VALUE;
        Card maxCard = null;
        for (Card card : mCards) {
            double value = card.matches(mat);
            if (value < max){
                max = value;
                maxCard = card;
            }
            if (value == 0){
                return card;
            }
        }
        if (maxCard != null) {
            Log.d(TAG,
                    String.format(
                            "%d %d -> matches: %f / %f",
                            maxCard.getValue(), maxCard.getSuit(), max, mat.size().area()));
        }
        return maxCard;
    }

    public boolean completed(){
        return mCards.size() == Card.CARD_SUITS.length * Card.CARD_VALUES.length;
    }

    public Set<Card> missingCards(){
        Set<Card> missing = new ArraySet<>();
        for (int i = 0; i < Card.CARD_SUITS.length; i++) {
            for (int j = 0; j < Card.CARD_VALUES.length; j++) {
                if (!mCardRegistered[i][j]){
                    missing.add(new Card(i,j));
                }
            }
        }
        return missing;
    }

    public void clear(){
        for (int i = 0; i < Card.CARD_SUITS.length; i++) {
            for (int j = 0; j < Card.CARD_VALUES.length; j++) {
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

package dcc.up.pt.cardgame;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import dcc.up.pt.cardgame.model.Card;

import static dcc.up.pt.cardgame.model.Card.CARD_SUITS;
import static dcc.up.pt.cardgame.model.Card.CARD_VALUES;

/**
 * Created by satyan on 11/30/17.
 *
 */

public class RegisterFragment extends Fragment{

    private int suitIndex = 0;
    private int valueIndex = 0;
    private OnRegistrationListener onRegistrationListener;
    private Button mOkButton;
    private Button mResetButton;
    private TextView mCurrentCardText;

    public static RegisterFragment newInstance() {
        return new RegisterFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCurrentCardText = view.findViewById(R.id.card_value);
        mOkButton = view.findViewById(R.id.button_ok);
        mResetButton = view.findViewById(R.id.button_reset);

    }

    private void updateCurrentCard(){
        if (suitIndex < CARD_SUITS.length && valueIndex < CARD_VALUES.length){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCurrentCardText.setVisibility(View.VISIBLE);
                    mCurrentCardText.setText(Card.card2String(getContext(),valueIndex, suitIndex));
                }
            });
        } else {
            mCurrentCardText.setVisibility(View.GONE);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOkButton.setText(R.string.finish);
                }
            });
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            suitIndex = savedInstanceState.getInt("var_suit", 0);
            valueIndex =savedInstanceState.getInt("var_value", 0);
        }
        final TextView instructionText = getActivity().findViewById(R.id.text_instruction);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instructionText.setText(R.string.register_instructions);
            }
        });

        updateCurrentCard();
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onRegistrationListener != null) {
                    if (mOkButton.getText().equals(getString(R.string.ok))) {
                        onRegistrationListener.onCardRegistered(valueIndex, suitIndex);
                    } else {
                        onRegistrationListener.onFinish();
                    }
                }
            }
        });
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onRegistrationListener != null){
                    onRegistrationListener.onReset();
                }
            }
        });
    }

    public void next(){
        valueIndex = valueIndex + 1;
        if (valueIndex >= CARD_VALUES.length) {
            suitIndex = suitIndex + 1;
            valueIndex = 0;
        }
        updateCurrentCard();
    }

    public void reset(){
        suitIndex = 0;
        valueIndex = 0;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOkButton.setText(R.string.ok);
            }
        });
        updateCurrentCard();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("var_suit", suitIndex);
        outState.putInt("var_value", valueIndex);
    }

    public void setOnRegistrationListener(OnRegistrationListener onRegistrationListener) {
        this.onRegistrationListener = onRegistrationListener;
    }

    interface OnRegistrationListener{
        void onCardRegistered(int value, int suit);
        void onReset();
        void onFinish();
    }
}

package dcc.up.pt.cardgame;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import dcc.up.pt.cardgame.model.Deck;

/**
 * Created by satyan on 11/30/17.
 *
 */

public class StartFragment extends Fragment {
    private OnStartListener onStartListener;
    private int mSuitCount = 4;
    private int mValueCount = 13;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start, container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button startButton = view.findViewById(R.id.button_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onStartListener != null){
                    onStartListener.onStart(mValueCount, mSuitCount);
                }
            }
        });
        final TextView instructionView = getActivity().findViewById(R.id.text_instruction);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instructionView.setText(R.string.start_instructions);
            }
        });

        final SeekBar valueBar = view.findViewById(R.id.bar_values);
        final TextView valueText = view.findViewById(R.id.text_values);
        valueBar.setMax(12);
        valueBar.setProgress(12);
        setValueText(valueText);
        valueBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mValueCount = progress + 1;
                setValueText(valueText);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setValueText(valueText);
            }
        });

        final SeekBar suitBar = view.findViewById(R.id.bar_suits);
        final TextView suitText = view.findViewById(R.id.text_suits);
        suitBar.setMax(3);
        suitBar.setProgress(3);
        setSuitText(suitText);
        suitBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSuitCount = progress+1;
                setSuitText(suitText);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setSuitText(suitText);
            }
        });
    }

    private void setSuitText(final TextView view) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < mSuitCount; i++) {
                    builder.append(getString(Deck.CARD_SUITS[i])).append(" ");
                }
                view.setText(builder.toString());
            }
        });
    }

    private void setValueText(final TextView view) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < mValueCount; i++) {
                    builder.append(Deck.CARD_VALUES[i]).append(" ");
                }
                view.setText(builder.toString());
            }
        });
    }

    public void setOnStartListener(OnStartListener listener){
        this.onStartListener = listener;
    }

    public static StartFragment newInstance() {
        return new StartFragment();
    }

    interface OnStartListener{
        void onStart(int valueCount, int suitCount);
    }
}

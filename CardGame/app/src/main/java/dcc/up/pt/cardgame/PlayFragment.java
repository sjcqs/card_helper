package dcc.up.pt.cardgame;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Created by satyan on 11/30/17.
 */

public class PlayFragment extends Fragment {
    private TextView[] mResults = new TextView[2];
    private Switch mModeSwitch;
    private Button mNextButton;
    private PlayModeListener mModeListener;

    public static PlayFragment newInstance() {
        return new PlayFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_play, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mModeListener = (PlayModeListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement PlayModeListener");
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mResults[0] = view.findViewById(R.id.result);
        mResults[1] = view.findViewById(R.id.result2);
        mNextButton = view.findViewById(R.id.next_button);
        mModeSwitch = view.findViewById(R.id.mode_switch);

        final TextView instruction = getActivity().findViewById(R.id.text_instruction);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instruction.setText(R.string.play_instruction);
            }
        });
        updateMode();

        mModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateMode();
                if (mModeListener != null){
                    mModeListener.onSwitchMode(isChecked);
                }
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mModeListener != null){
                    mModeListener.onNextTurn();
                }
            }
        });

    }

    private void updateMode(){
        if (mModeSwitch.isChecked()){
            mModeSwitch.setText(R.string.auto);
            mNextButton.setVisibility(View.INVISIBLE);
        } else {
            mModeSwitch.setText(R.string.manual);
            mNextButton.setVisibility(View.VISIBLE);
        }
    }


    public void setResult(final String text, final int index){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mResults[index].setText(text);
            }
        });
    }

    public void disableButton() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNextButton.setEnabled(false);
            }
        });
    }

    public void enableButton() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNextButton.setEnabled(true);
            }
        });
    }

    public interface PlayModeListener{
        void onSwitchMode(boolean isAuto);
        void onNextTurn();
    }
}

package dcc.up.pt.cardgame;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by satyan on 11/30/17.
 */

public class PlayFragment extends Fragment {
    public TextView[] result = new TextView[2];
    public static PlayFragment newInstance() {
        return new PlayFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_play, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        result[0] = view.findViewById(R.id.result);
        result[1] = view.findViewById(R.id.result2);
        final TextView instruction = getActivity().findViewById(R.id.text_instruction);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instruction.setText(R.string.play_instruction);
            }
        });
    }


    public void setResult(final String text, final int index){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[index].setText(text);
            }
        });
    }
}

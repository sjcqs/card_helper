package dcc.up.pt.cardgame;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by satyan on 11/30/17.
 */

public class StartFragment extends Fragment {
    private OnStartListener onStartListener;

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
                    onStartListener.onStart();
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
    }

    public void setOnStartListener(OnStartListener listener){
        this.onStartListener = listener;
    }

    public static StartFragment newInstance() {
        return new StartFragment();
    }

    interface OnStartListener{
        void onStart();
    }
}

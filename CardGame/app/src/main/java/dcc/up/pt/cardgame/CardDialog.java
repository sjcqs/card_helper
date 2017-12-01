package dcc.up.pt.cardgame;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by satyan on 11/30/17.
 *
 */

public class CardDialog extends DialogFragment {
    private CardDialogListener mListener;

    interface CardDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, int value, int suit);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (CardDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        final int suit = args.getInt("suit");
        final int value = args.getInt("value");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_card_registered)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(CardDialog.this, value, suit);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(CardDialog.this);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mListener.onDialogNegativeClick(CardDialog.this);
                    }
                });
        return builder.create();
    }
}

package pt.ulisboa.tecnico.cmu.locmess.features.configuration.fragments;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.fragments.BaseDialogFragment;

public class MessageHopsDialogFragment extends BaseDialogFragment {

    NumberPicker mNumberPicker;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), getTheme());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_number_picker, null);

        mNumberPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        mNumberPicker.setMinValue(0);
        mNumberPicker.setMaxValue(10);

        builder.setView(view)
                .setTitle("Message Hops")
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(MessageHopsDialogFragment.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getDialog().cancel();
                    }
                });

        return builder.create();
    }
}

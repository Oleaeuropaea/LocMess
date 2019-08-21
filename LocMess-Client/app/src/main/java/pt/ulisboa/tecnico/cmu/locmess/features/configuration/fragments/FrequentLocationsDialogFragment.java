package pt.ulisboa.tecnico.cmu.locmess.features.configuration.fragments;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.fragments.BaseDialogFragment;

public class FrequentLocationsDialogFragment extends BaseDialogFragment {

    ListView mListView;
    HashSet<LocationLocMess> mSelectedLocations = new HashSet<>();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), getTheme());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_multiple_selection, null);


        List<LocationLocMess> locations = mLocationManager.getValidLocations();
        ArrayAdapter<LocationLocMess> adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_multiple_choice,
                locations
        );

        mListView = (ListView) view.findViewById(R.id.lv_multi_selection_list);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SparseBooleanArray clickedItemsPositions = mListView.getCheckedItemPositions();

                for (int index = 0; index < clickedItemsPositions.size(); index++) {
                    int key = clickedItemsPositions.keyAt(index);
                    LocationLocMess location = (LocationLocMess) mListView.getItemAtPosition(key);

                    boolean checked = clickedItemsPositions.valueAt(index);
                    if (checked) {
                        mSelectedLocations.add(location);
                    } else {
                        mSelectedLocations.remove(location);
                    }
                }
            }
        });


        builder.setView(view)
                .setTitle("Frequent Locations")
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(FrequentLocationsDialogFragment.this);
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

    public HashSet<LocationLocMess> getSelectedLocation() {
        return mSelectedLocations;
    }
}

package pt.ulisboa.tecnico.cmu.locmess.features.configuration.activites;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.features.configuration.adapters.FrequentLocationsAdapter;
import pt.ulisboa.tecnico.cmu.locmess.features.configuration.fragments.FrequentLocationsDialogFragment;
import pt.ulisboa.tecnico.cmu.locmess.features.configuration.fragments.MessageHopsDialogFragment;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.fragments.BaseDialogFragment;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;

public class SettingsActivity extends BaseActivity
        implements BaseDialogFragment.NoticeDialogListener, FrequentLocationsAdapter.NoticeDeleteFrequentLocationListener {
    private final String MESSAGE_HOPS_DIALOG_TAG = "MESSAGE_HOPS_DIALOG_TAG";
    private final String FREQUENT_LOCATIONS_DIALOG_TAG = "FREQUENT_LOCATIONS_DIALOG_TAG";

    private LocMessPreferences mLocMessPreferences;

    private List<LocationLocMess> mFrequentLocationsList = new ArrayList<>();
    private FrequentLocationsAdapter mFrequentLocationsAdapter;

    private int mDrawerItemId;

    private Switch mWifiDirectSwitch;
    private ConstraintLayout mMessageHopsConstraintLayout;
    private TextView mMessageHopsVal;
    private ListView mFrequentLocationListView;
    private ImageButton mAddFrequentLocationsImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        super.onCreateDrawer(toolbar);
        mDrawerItemId = BaseActivity.sCurrDrawerItemId;

        mLocMessPreferences = LocMessPreferences.getInstance();

        mWifiDirectSwitch = (Switch) findViewById(R.id.switch_wifi_direct);
        boolean isWifiDirectEnable = mLocMessPreferences.getWifiDirectEnableSetting();
        if (isWifiDirectEnable) {
            mWifiDirectSwitch.toggle();
        }

        mMessageHopsConstraintLayout = (ConstraintLayout) findViewById(R.id.cl_message_hops);
        mMessageHopsConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageHopsDialogFragment fragment = new MessageHopsDialogFragment();
                fragment.show(getFragmentManager(), MESSAGE_HOPS_DIALOG_TAG);
            }
        });
        mMessageHopsVal = (TextView) mMessageHopsConstraintLayout.findViewById(R.id.tv_message_hops_val);
        int messageHops = mLocMessPreferences.getMessageHopsSetting();
        mMessageHopsVal.setText(String.valueOf(messageHops));

        mAddFrequentLocationsImageButton = (ImageButton) findViewById(R.id.ib_frequent_locations);
        mAddFrequentLocationsImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FrequentLocationsDialogFragment fragment = new FrequentLocationsDialogFragment();
                fragment.show(getFragmentManager(), FREQUENT_LOCATIONS_DIALOG_TAG);
            }
        });

        List<LocationLocMess> frequentLocations = mLocMessPreferences.getFrequentLocationsSetting();
        if (frequentLocations != null) {
            mFrequentLocationsList.addAll(mLocMessPreferences.getFrequentLocationsSetting());
        }

        mFrequentLocationsAdapter = new FrequentLocationsAdapter(this, 0, mFrequentLocationsList);
        mFrequentLocationsAdapter.setListener(this);
        mFrequentLocationListView = (ListView) findViewById(R.id.lv_frequent_locations);
        mFrequentLocationListView.setAdapter(mFrequentLocationsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BaseActivity.sCurrDrawerItemId = mDrawerItemId;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocMessPreferences.setWifiDirectEnableSetting(mWifiDirectSwitch.isChecked());

        mLocMessPreferences.setMessageHopsSetting(Integer.valueOf(mMessageHopsVal.getText().toString()));

        String locationsJson = LocMessJsonUtils.toLocationJson(mFrequentLocationsList);
        mLocMessPreferences.setFrequentLocationsSetting(locationsJson);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialogFragment) {
        String tag = dialogFragment.getTag();
        Dialog dialog = dialogFragment.getDialog();

        if (tag.equals(MESSAGE_HOPS_DIALOG_TAG)) {
            NumberPicker numberPicker = (NumberPicker) dialog.findViewById(R.id.numberPicker);
            mMessageHopsVal.setText(String.valueOf(numberPicker.getValue()));
        } else {
            FrequentLocationsDialogFragment fragment = (FrequentLocationsDialogFragment) dialogFragment;
            mFrequentLocationsList.clear();
            mFrequentLocationsList.addAll(fragment.getSelectedLocation());
            mFrequentLocationsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDeleteFrequentLocationClick(LocationLocMess locationLocMess) {
        mFrequentLocationsList.remove(locationLocMess);
        mFrequentLocationsAdapter.notifyDataSetChanged();
    }
}

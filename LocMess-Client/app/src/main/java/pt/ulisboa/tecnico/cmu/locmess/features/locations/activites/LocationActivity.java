package pt.ulisboa.tecnico.cmu.locmess.features.locations.activites;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.adapters.LocationsAdapter;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

public class LocationActivity extends BaseActivity {
    public static final String SEND_LOCATION = "pt.ulisboa.tecnico.cmu.locmess.ui.main.SEND_LOCATION";
    public static final int REQUEST_LOCATION_ACTIVITY = 11;
    public static final int REQUEST_LOCATION_DETAIL = 22;
    public static final String SYNCHRONIZE_LOCATIONS = "pt.ulisboa.tecnico.cmu.locmess.features.locations.activites.NEW_LOCATIONS";

    private int mDrawerItemId;
    private ArrayList<LocationLocMess> locations;
    ArrayAdapter<LocationLocMess> locationAdapter;
    private ListView listView;

    LocMessApplication application;


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("[LocationActivity]","INTENT RECEIVED");

            locations.clear();
            locations.addAll(application.locMessBackgroundService.locationsManager.getValidLocations());
            locationAdapter.notifyDataSetChanged();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        super.onCreateDrawer(toolbar);
        mDrawerItemId = BaseActivity.sCurrDrawerItemId;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (NetworkUtils.isNetworkAvailable()) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), LocationCreationActivity.class);
                    startActivityForResult(intent, REQUEST_LOCATION_ACTIVITY);
                }
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        application = (LocMessApplication) getApplicationContext();
        locations = new ArrayList<>();
        locations.addAll(application.locMessBackgroundService.locationsManager.getValidLocations());

        listView = (ListView) findViewById(R.id.locations_list);

        // No content
        TextView emptyText = (TextView) findViewById(R.id.tv_empty);
        listView.setEmptyView(emptyText);

        locationAdapter = new LocationsAdapter(this, 0, locations);
        listView.setAdapter(locationAdapter);

        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LocationLocMess locationToSend = locations.get(position);

                Intent intendToSend = new Intent();
                intendToSend.setClass(LocationActivity.this, LocationDetailActivity.class);
                Bundle bundleToSend = new Bundle();
                bundleToSend.putSerializable(SEND_LOCATION, locationToSend);
                intendToSend.putExtras(bundleToSend);
                startActivityForResult(intendToSend, REQUEST_LOCATION_DETAIL);

            }
        });


        //Responsible for refreshing List if inserted on Server
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SYNCHRONIZE_LOCATIONS);
        registerReceiver(receiver, intentFilter);


    }

    @Override
    protected void onResume() {
        super.onResume();
        BaseActivity.sCurrDrawerItemId = mDrawerItemId;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOCATION_ACTIVITY){
            if(resultCode == Activity.RESULT_OK){
                //The task of LocationCreationActivity is responsible to notifyAdapter


            }
        }

        if(requestCode == REQUEST_LOCATION_DETAIL){
            if(resultCode == Activity.RESULT_OK){

                //The task of LocationDetailActivity is responsible to notifyAdapter

//                Bundle receivedBundle = data.getExtras();
//                LocationLocMess locationToRemove = (LocationLocMess) receivedBundle.getSerializable(SEND_LOCATION);
//                application.locMessBackgroundService.locationsManager.removeLocation(locationToRemove);
//                locations.clear();
//                locations.addAll(application.locMessBackgroundService.locationsManager.getValidLocations());
//                locationAdapter.notifyDataSetChanged();
            }
        }

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);

        super.onDestroy();

    }
}

package pt.ulisboa.tecnico.cmu.locmess.features.locations.activites;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.GPSManager;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

public class LocationCreationActivity extends BaseActivity {
    public static final String TAG = LocationCreationActivity.class.getCanonicalName();

    private LocationLocMess.LocationType currentType;

    private ArrayList<String> connections = new ArrayList();
    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_location);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RadioGroup rg = (RadioGroup) findViewById(R.id.coordinate_types);
        int checked = rg.getCheckedRadioButtonId();
        View selectedButton = rg.findViewById(checked);
        int selected = rg.indexOfChild(selectedButton);
        showView(selected);

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int pos = radioGroup.indexOfChild(findViewById(i));
                RadioButton r = (RadioButton) radioGroup.getChildAt(pos);
                showView(pos);
            }
        });

        Button btn = (Button) findViewById(R.id.available_connections);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                setupAvailableConnections();
            }
        });

        arrayAdapter = new ArrayAdapter(this, R.layout.item_available_connection, connections);
        ListView listView = (ListView) findViewById(R.id.list_available_hosts);
        listView.setAdapter(arrayAdapter);
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView ssidName = (TextView) findViewById(R.id.new_ssid);
                ssidName.setText(((TextView) view).getText());
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_activity_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save_location) {
            saveLocation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void saveLocation() {
        String locationName = checkTextEdit(R.id.location_name, getString(R.string.error_msg_empty_name));
        if (locationName.isEmpty()) {
            return;
        }

        LocationLocMess newLocation;

        if (currentType == LocationLocMess.LocationType.GPS) {
            String locationLatitude = checkTextEdit(R.id.new_latitude, getString(R.string.error_msg_empty_latitude));
            if (locationLatitude.isEmpty()) {
                return;
            }

            String locationLongitude = checkTextEdit(R.id.new_longitude, getString(R.string.error_msg_empty_longitude));
            if (locationLongitude.isEmpty()) {
                return;
            }

            String locationRadius = checkTextEdit(R.id.new_radius, getString(R.string.error_msg_empty_radius));
            if (locationRadius.isEmpty()) {
                return;
            }

            newLocation = new LocationLocMess("gpsURL", locationName, Float.parseFloat(locationLatitude),
                    Float.parseFloat(locationLongitude), Float.parseFloat(locationRadius));

        } else {
            String locationSsid = checkTextEdit(R.id.new_ssid, getString(R.string.error_msg_empty_ssid));
            if (locationSsid.isEmpty()) {
                return;
            }

            newLocation = new LocationLocMess("ssidURL", currentType, locationName, locationSsid);
        }

        new CreateLocationTask(newLocation).execute();

        Intent resultIntent = new Intent();
        Bundle resultBundle = new Bundle();
        resultBundle.putSerializable(LocationActivity.SEND_LOCATION, newLocation);
        resultIntent.putExtras(resultBundle);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private String checkTextEdit(int id, String errorMessage) {
        EditText editText = (EditText) findViewById(id);
        String editTextStr = editText.getText().toString();
        if (TextUtils.isEmpty(editTextStr)) {
            editText.setError(errorMessage);
        }
        return editTextStr;
    }

    private void showView(int selectedPos) {
        ConstraintLayout constrLayout = (ConstraintLayout) findViewById(R.id.location_coordinates);
        View gpsView = constrLayout.getChildAt(0);
        View ssidView = constrLayout.getChildAt(1);
        gpsView.setVisibility(View.VISIBLE);
        ssidView.setVisibility(View.VISIBLE);
        if (selectedPos == 0) {       // gps
            ssidView.setVisibility(View.GONE);
            currentType = LocationLocMess.LocationType.GPS;
        }
        if (selectedPos == 1) {          // wifi
            gpsView.setVisibility(View.GONE);
            currentType = LocationLocMess.LocationType.WIFI;

            connections.clear();
            Button btn = (Button) findViewById(R.id.available_connections);
            btn.setEnabled(true);
        }
        if (selectedPos == 2) {          // ble
            gpsView.setVisibility(View.GONE);
            currentType = LocationLocMess.LocationType.BLE;

            connections.clear();
            Button btn = (Button) findViewById(R.id.available_connections);
            btn.setEnabled(true);
        }
    }

    private void setupAvailableConnections() {
        LocMessApplication locMessApplication = (LocMessApplication) getApplicationContext();
        ArrayList<String> devices = locMessApplication.locMessBackgroundService.wiFiDirectManager.getAvailableDevices();

        if (currentType == LocationLocMess.LocationType.WIFI) {
            for (String s : devices) {
                if (s.startsWith("WIFI_") || s.startsWith("DEV_")) {
                    connections.add(s);
                }
            }
        }
        if (currentType == LocationLocMess.LocationType.BLE) {
            for (String s : devices) {
                if (s.startsWith("BLE_")) {
                    connections.add(s);
                }
            }
        }
        arrayAdapter.notifyDataSetChanged();
    }

    public void onSetCurrentCoordinatesClick(View view) {
        EditText latitudeText = (EditText) findViewById(R.id.new_latitude);
        EditText longitudeText = (EditText) findViewById(R.id.new_longitude);

        latitudeText.setText("");
        longitudeText.setText("");

        LocMessApplication locMessApplication = (LocMessApplication) getApplicationContext();
        GPSManager gpsManager = locMessApplication.locMessBackgroundService.gpsManager;

        if (gpsManager.checkPermission()) {
            Location lastLocation =
                    locMessApplication.locMessBackgroundService.gpsManager.getCurrentCoordinates();
            latitudeText.setText(String.valueOf(lastLocation.getLatitude()));
            longitudeText.setText(String.valueOf(lastLocation.getLongitude()));
        } else {
            Toast.makeText(this, "GPS Permission are not granted", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Represents an asynchronous task used to save location on server
     */
    private class CreateLocationTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        LocationLocMess mLocation;

        CreateLocationTask(LocationLocMess location) {
            mLocation = location;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String locationJson = LocMessJsonUtils.toLocationJson(mLocation);
            try {
                return NetworkUtils.postJsonToUrl(NetworkUtils.LOCATIONS_URL, locationJson);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            if (result != null) {
                int httpStatus = result.getHttpStatus();
                if (httpStatus == HttpStatus.SC_CREATED) {

                    LocationLocMess locationLocMess =
                            LocMessJsonUtils.toLocationLocMessObj(result.getHttpResult());
                    Log.d(TAG, locationLocMess.toString());

                    LocMessApplication locMessApplication = (LocMessApplication) getApplicationContext();
                    locMessApplication.locMessBackgroundService.locationsManager.addNewLocation(locationLocMess);

                    sendBroadcast(new Intent().setAction(LocationActivity.SYNCHRONIZE_LOCATIONS));

                    Toast.makeText(
                            LocationCreationActivity.this, "Location created.", Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
            }
            Toast.makeText(
                    LocationCreationActivity.this, "Network error, something went wrong", Toast.LENGTH_LONG
            ).show();
        }
    }
}

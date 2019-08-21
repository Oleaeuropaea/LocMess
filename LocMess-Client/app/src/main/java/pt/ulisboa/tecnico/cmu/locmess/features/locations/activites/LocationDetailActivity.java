package pt.ulisboa.tecnico.cmu.locmess.features.locations.activites;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;

import java.io.IOException;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

public class LocationDetailActivity extends BaseActivity {
    private LocationLocMess location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();

        Bundle receivedBundle = intent.getExtras();
        location = (LocationLocMess) receivedBundle.getSerializable(LocationActivity.SEND_LOCATION);

        TextView name = (TextView) findViewById(R.id.detail_loc_name);
        name.setText(location.getName());

        TextView type = (TextView) findViewById(R.id.detail_loc_type);
        type.setText(location.getType());

        ConstraintLayout infoLayout = (ConstraintLayout) findViewById(R.id.location_type_details);
        if(location.getType().equals(LocationLocMess.LocationType.GPS.getType())) {
            View.inflate(this, R.layout.item_location_detail_gps, infoLayout);
            TextView latitude = (TextView) findViewById(R.id.detail_loc_latitude);
            latitude.setText(String.valueOf(location.getLatitude()));
            TextView longitude = (TextView) findViewById(R.id.detail_loc_longitude);
            longitude.setText(String.valueOf(location.getLongitude()));
            TextView radius = (TextView) findViewById(R.id.detail_loc_radius);
            radius.setText(String.valueOf(location.getRadius()));
        } else {
            View.inflate(this, R.layout.item_location_detail_ssid, infoLayout);
            TextView ssid = (TextView) findViewById(R.id.detail_loc_ssid);
            ssid.setText(location.getSsid());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (NetworkUtils.isNetworkAvailable()) {
            getMenuInflater().inflate(R.menu.activity_detail_remove, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete_location) {
            new DeleteLocation(location).execute();

            Intent resultIntent = new Intent();
            Bundle bundleToSend = new Bundle();
            bundleToSend.putSerializable(LocationActivity.SEND_LOCATION, location);
            resultIntent.putExtras(bundleToSend);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Represents an asynchronous task used to delete InterestLocMess from Server
     */
    public class DeleteLocation extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        private static final String TAG = "+DeleteLocation";
        LocationLocMess mLocation;

        public DeleteLocation(LocationLocMess location) {
            mLocation = location;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            try {
                return NetworkUtils.deleteObjFromUrl(mLocation.getUrl());
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            if (result != null) {
                int httpStatus = result.getHttpStatus();
                if (httpStatus == HttpStatus.SC_NO_CONTENT) {
                    LocMessApplication locMessApplication = (LocMessApplication) getApplicationContext();
                    locMessApplication.locMessBackgroundService.locationsManager.removeLocation(mLocation);
                    sendBroadcast(new Intent().setAction(LocationActivity.SYNCHRONIZE_LOCATIONS));

                    Toast.makeText(LocationDetailActivity.this, "Location deleted.", Toast.LENGTH_LONG).show();
                } else {
                    JsonObject jsonObject = LocMessJsonUtils.toJsonObj(result.getHttpResult());
                    Toast.makeText(
                            LocationDetailActivity.this,
                            "Network error, " + jsonObject.get("detail").toString(),
                            Toast.LENGTH_LONG
                    ).show();
                }

            } else {
                Toast.makeText(
                        LocationDetailActivity.this, "Cannot deleted location.", Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}

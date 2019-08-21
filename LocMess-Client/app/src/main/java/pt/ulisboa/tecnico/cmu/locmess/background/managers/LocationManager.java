package pt.ulisboa.tecnico.cmu.locmess.background.managers;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.JsonObject;

import org.apache.commons.collections4.map.SingletonMap;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.activites.LocationActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessLinkedHashSet;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.database.DatabaseHandler;

public class LocationManager {
    public static final String TAG = LocationManager.class.getCanonicalName();
    private Context serviceContext;
    private WiFiDirectManager wiFiDirectManager;
    DatabaseHandler database;

    private String mTimestamp;
    private final LocMessLinkedHashSet<LocationLocMess> validLocations;


    public LocationManager(Context context) {
        Log.d("+LocationManager", "Locations Manager created!");
        this.serviceContext = context;
        LocMessBackgroundService backgroundService = (LocMessBackgroundService) context;
        wiFiDirectManager = backgroundService.wiFiDirectManager;

        validLocations = new LocMessLinkedHashSet<>();
        database = new DatabaseHandler(serviceContext);
        mTimestamp = LocMessPreferences.getInstance().getLocationManagerTimestamp();

        getAllLocations();
    }

    private void getAllLocations() {
        validLocations.addAll(database.getAllLocations());
    }

    private void updateLocationsList(List<LocationLocMess> locations) {
        for (LocationLocMess location : locations) {
            validLocations.add(location);
            database.insertLocation(location);
        }
        wiFiDirectManager.notifyLocationListChanged();
    }

    public void addNewLocation(LocationLocMess newLocation) {
        validLocations.add(newLocation);
        database.insertLocation(newLocation);
        wiFiDirectManager.notifyLocationListChanged();
    }

    public void removeLocation(LocationLocMess location) {
        validLocations.remove(location);
        database.removeLocation(location);
        wiFiDirectManager.notifyLocationListChanged();
    }

    public List<LocationLocMess> getValidLocations() {
        ArrayList<LocationLocMess> locationsToReturn = new ArrayList<>();
        synchronized (validLocations) {
            for (LocationLocMess l : validLocations) {
                locationsToReturn.add(l);
            }
        }
        return locationsToReturn;
    }

    public Thread runLocationsRequester() {
        Thread thread = new Thread(new LocationsRequester(), "LocationsRequester");
        thread.start();
        return thread;
    }

    private class LocationsRequester implements Runnable {
        @Override
        public void run() {
            try {
                Log.d("+LocationRequester", "Fetch Locations from server...");
                NetworkUtils.NetworkResult result;
                if (mTimestamp != null) {
                    SingletonMap<String, String> queryParam =
                            new SingletonMap<>(NetworkUtils.TIMESTAMP, mTimestamp);
                    result = NetworkUtils.getJsonFromUrl(NetworkUtils.LOCATIONS_URL, queryParam);
                } else {
                    result = NetworkUtils.getJsonFromUrl(NetworkUtils.LOCATIONS_URL);
                }

                int httpStatus = result.getHttpStatus();
                if (httpStatus == HttpStatus.SC_OK) {
                    mTimestamp = LocMessJsonUtils.getTimestampJson(result.getHttpResult());
                    LocMessPreferences.getInstance().setLocationManagerTimestamp(mTimestamp);
                    String resultJson = LocMessJsonUtils.getResultJson(result.getHttpResult());

                    List<LocationLocMess> locationsList = LocMessJsonUtils.toLocationsList(resultJson);
                    updateLocationsList(locationsList);

                    serviceContext.sendBroadcast(new Intent().setAction(LocationActivity.SYNCHRONIZE_LOCATIONS));
                } else if (httpStatus != HttpStatus.SC_NO_CONTENT) {
                    JsonObject jsonObject = LocMessJsonUtils.toJsonObj(result.getHttpResult());
                    Log.e(TAG, jsonObject.get("detail").toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

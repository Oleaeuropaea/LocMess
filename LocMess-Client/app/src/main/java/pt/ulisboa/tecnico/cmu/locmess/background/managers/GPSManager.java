package pt.ulisboa.tecnico.cmu.locmess.background.managers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.PostFragment;

public class GPSManager implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private String TAG = "GPSMANAGER_GOOGLE";
    private static final long UPDATES_INTERVAL = 5000;
    private Context serviceContext;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean isConnected;

    // current locations
    private Location mCurrentLocation;
    private final Object mCurrentLocationLock = new Object();
    private boolean updateCurrentLocation = true;

    ArrayList<LocationLocMess> currLocations;

    public GPSManager(Context serviceContext) {
        currLocations = new ArrayList<>();
        this.serviceContext = serviceContext;
        isConnected = false;
        mGoogleApiClient = new GoogleApiClient.Builder(serviceContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();

        if (checkPermission()) {
            connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        int permission = ContextCompat.checkSelfPermission(serviceContext, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mCurrentLocation = getCurrentCoordinates();
            updateCurrentLocation = true;
        } else
            Log.d(TAG, "NO PERMISSION GRANTED");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "LocationChanged to: " + location.getLatitude() + "," + location.getLongitude());
        mCurrentLocation = location;
        updateCurrentLocation = true;
    }

    public boolean isNewCurrentLocations() {
        return updateCurrentLocation;
    }

    public boolean checkPermission() {
        int accessFinePerm = ContextCompat.checkSelfPermission(
                serviceContext, Manifest.permission.ACCESS_FINE_LOCATION);
        int accessCoarsePerm = ContextCompat.checkSelfPermission(
                serviceContext, Manifest.permission.ACCESS_COARSE_LOCATION);
        return (accessFinePerm + accessCoarsePerm == PackageManager.PERMISSION_GRANTED);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATES_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATES_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void connect() {
        if (!isConnected) {
            Log.d(TAG, "GoogleApiClient Connected");
            isConnected = true;
            mGoogleApiClient.connect();
        }
    }

    public void disconnect() {
        if (isConnected) {
            Log.d(TAG, "GoogleApiClient Disconnect");
            isConnected = false;
            mGoogleApiClient.disconnect();
        }
    }

    public Location getCurrentCoordinates() {

        int permission = ContextCompat.checkSelfPermission(serviceContext,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            updateCurrentLocation = true;
            return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            Log.d(TAG, "No Permission Granted to access Gps location");
            return null;
        }
    }

    public List<LocationLocMess> getCurrentLocations() {
        if (updateCurrentLocation) {
            currLocations = new ArrayList<>();
            if (mCurrentLocation != null) {
                synchronized (mCurrentLocationLock) {
                    LocMessBackgroundService backgroundService = (LocMessBackgroundService) serviceContext;
                    ArrayList<LocationLocMess> validLocations =
                            (ArrayList<LocationLocMess>) backgroundService.locationsManager.getValidLocations();
                    double currLat = mCurrentLocation.getLatitude();
                    double currLong = mCurrentLocation.getLongitude();

                    for (LocationLocMess locationX : validLocations) {
                        if (locationX.getType().equals(LocationLocMess.LocationType.GPS.getType())) {
                            float[] distance = new float[2];
                            double xLat = locationX.getLatitude();
                            double xLong = locationX.getLongitude();
                            double xRadius = locationX.getRadius();
                            Location.distanceBetween(currLat, currLong, xLat, xLong, distance);
                            if (distance[0] <= xRadius) {
                                currLocations.add(locationX);
                            }

                        }
                    }
                }
            }
            updateCurrentLocation = false;
        }
        return currLocations;
    }

}

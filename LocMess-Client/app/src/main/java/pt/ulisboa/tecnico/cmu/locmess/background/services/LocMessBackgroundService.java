package pt.ulisboa.tecnico.cmu.locmess.background.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import pt.ulisboa.tecnico.cmu.locmess.background.managers.GPSManager;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.InterestManager;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.LocationManager;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.NetworkManager;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.PostManager;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.WiFiDirectManager;
import pt.ulisboa.tecnico.cmu.locmess.utils.database.DatabaseHandler;

import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.CENTRALIZED_NEARBY_POSTS;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.CENTRALIZED_POSTED_POSTS;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.CENTRALIZED_SAVED_POSTS;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.DECENTRALIZED_NEARBY_POSTS;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.DECENTRALIZED_POSTED_POSTS;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.DECENTRALIZED_SAVED_POSTS;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.THIRD_PARTY_POSTS;


public class LocMessBackgroundService extends Service {
    IBinder mBinder = new LocMessBackgroundService.LocalBinder();

    public GPSManager gpsManager;
    public NetworkManager networkManager;
    public WiFiDirectManager wiFiDirectManager;
    public PostManager postsManager;
    public LocationManager locationsManager;
    public InterestManager interestManager;

    // binder to allow access to Service from client
    public class LocalBinder extends Binder {
        public LocMessBackgroundService getLocMessBackgroundServiceInstance() {
            return LocMessBackgroundService.this;
        }
    }

    // binder returned to client
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d("++BackgroundService", "Starting Background Service in " + Thread.currentThread().getName());
        this.gpsManager = new GPSManager(this);
        this.networkManager = new NetworkManager(this);
        this.wiFiDirectManager = new WiFiDirectManager(this);
        this.locationsManager = new LocationManager(this);
        this.interestManager = new InterestManager(this);
        this.postsManager = new PostManager(this);
    }

    @Override
    public void onDestroy() {
        Log.d("++BackgroundService", "Destroying Background Service in " + Thread.currentThread().getName());
        wiFiDirectManager.disableSimWifiP2pService();
        super.onDestroy();
        deleteDatabase(DatabaseHandler.DB_NAME);
        deleteFiles();
        super.onDestroy();
    }

    private void deleteFiles() {
        this.deleteFile(CENTRALIZED_SAVED_POSTS);
        this.deleteFile(DECENTRALIZED_SAVED_POSTS);
        this.deleteFile(CENTRALIZED_POSTED_POSTS);
        this.deleteFile(DECENTRALIZED_POSTED_POSTS);
        this.deleteFile(CENTRALIZED_NEARBY_POSTS);
        this.deleteFile(DECENTRALIZED_NEARBY_POSTS);
        this.deleteFile(THIRD_PARTY_POSTS);
    }
}

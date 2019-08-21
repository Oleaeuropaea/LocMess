package pt.ulisboa.tecnico.cmu.locmess.background.managers;


import android.content.Context;

import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

public class NetworkManager {
    private Context mContext;
    private LocMessBackgroundService backgroundService;

    private InterestManager mInterestManager;
    private GPSManager mGPSManager;
    private LocationManager mLocationManager;
    private PostManager mPostManager;
    private WiFiDirectManager mWiFiDirectManager;

    public NetworkManager(Context context) {
        mContext = context;
        backgroundService = (LocMessBackgroundService) context;

        new Thread(new CentralizeThread()).start();
        new Thread(new DecentralizeThread()).start();
    }

    private class CentralizeThread implements Runnable {
        @Override
        public void run() {
            waitForManagers();

            // only once
            mInterestManager.fetchGlobalInterestsTask(mContext);

            Thread locationsRequesterThread = mLocationManager.runLocationsRequester();
            Thread centralizedPostRequesterThread = mPostManager.runCentralizedPostRequester();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (NetworkUtils.testAndSetNetworkConnectivity(mContext)) {
                        if (!locationsRequesterThread.isAlive()) {
                            locationsRequesterThread = mLocationManager.runLocationsRequester();
                        }
                        if (!centralizedPostRequesterThread.isAlive()) {
                            centralizedPostRequesterThread = mPostManager.runCentralizedPostRequester();
                        }
                    }

                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private class DecentralizeThread implements Runnable {
        @Override
        public void run() {
            waitForManagers();

            Thread decentralizedPostSenderThread = mPostManager.runDecentralizedPostSender();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!decentralizedPostSenderThread.isAlive()) {
                        decentralizedPostSenderThread = mPostManager.runDecentralizedPostSender();
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private synchronized void waitForManagers() {
        boolean managersDown = true;
        while (managersDown) {
            if (backgroundService.gpsManager != null
                    && backgroundService.interestManager != null
                    && backgroundService.locationsManager != null
                    && backgroundService.postsManager != null
                    && backgroundService.wiFiDirectManager != null) {
                mInterestManager = backgroundService.interestManager;
                mGPSManager = backgroundService.gpsManager;
                mLocationManager = backgroundService.locationsManager;
                mPostManager = backgroundService.postsManager;
                mWiFiDirectManager = backgroundService.wiFiDirectManager;

                managersDown = false;
            }
        }
    }
}

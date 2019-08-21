package pt.ulisboa.tecnico.cmu.locmess.background.managers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.PostFragment;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NotificationSender;

import static android.content.Context.MODE_PRIVATE;

public class WiFiDirectManager implements SimWifiP2pManager.PeerListListener, SimWifiP2pManager.GroupInfoListener {
    private Context serviceContext;

    private WiFiDirectManager.SimWifiP2pBroadcastReceiver mReceiver;
    private SimWifiP2pSocketServer socketServer = null;

    // attributes regarding to wifiP2pService
    private Messenger wifiP2pService = null;
    private SimWifiP2pManager wifiP2pManager = null;
    private SimWifiP2pManager.Channel channel = null;
    private boolean wifiP2pServiceBounded = false;

    // current connections
    private final SimWifiP2pDeviceList visibleDevices;
    private boolean updateVisibleleDevArray = true;     // update array before return to client
    private ArrayList<String> visibleDevicesArray;

    // only devices of other users in network
    private final SimWifiP2pDeviceList devicesInNetwork;

    // current locations
    private boolean updateCurrentLocation;
    private long currentLocationsWriteTimestamp = 0;
    private ArrayList<LocationLocMess> currentLocations;

    private String myDeviceName = null;


    private ServiceConnection connectionWifiP2p = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            wifiP2pService = new Messenger(service);
            wifiP2pManager = new SimWifiP2pManager(wifiP2pService);
            channel = wifiP2pManager.initialize(serviceContext, serviceContext.getMainLooper(), null);
            wifiP2pServiceBounded = true;

            requestPeerList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            wifiP2pService = null;
            wifiP2pManager = null;
            channel = null;
            wifiP2pServiceBounded = false;
        }
    };


    public void enableSimWifiP2pService() {
        Log.d("++enableWiFiDirServ", "WiFiP2pService started");
        Intent intent = new Intent(serviceContext, SimWifiP2pService.class);
        serviceContext.bindService(intent, connectionWifiP2p, Context.BIND_AUTO_CREATE);
        wifiP2pServiceBounded = true;

        new WiFiDirectManager.IncommingCommTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public WiFiDirectManager(Context serviceContext) {
        Log.d("+WiFiDirectManager", "WiFi Direct Manager created!");
        this.serviceContext = serviceContext;

        this.updateCurrentLocation = true;

        this.visibleDevices = new SimWifiP2pDeviceList();
        this.devicesInNetwork = new SimWifiP2pDeviceList();
        this.visibleDevicesArray = new ArrayList<>();
        this.currentLocations = new ArrayList<>();

        // termite initialization
        SimWifiP2pSocketManager.Init(serviceContext);
        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        mReceiver = new WiFiDirectManager.SimWifiP2pBroadcastReceiver();
        serviceContext.registerReceiver(mReceiver, filter);

        enableSimWifiP2pService();

        // FIXME remove after adding support to automatic addition of posts
        SharedPreferences preferences = serviceContext.getSharedPreferences("devicesReceivedPost", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }


    public void disableSimWifiP2pService() {
        Log.d("++disableWiFiDirServ", "WiFiP2pService finished");
        if (wifiP2pServiceBounded) {
            serviceContext.unbindService(connectionWifiP2p);
            wifiP2pServiceBounded = false;
            try {
                socketServer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // request to termite
    public void requestPeerList() {
        if (wifiP2pServiceBounded) {
            wifiP2pManager.requestPeers(channel, WiFiDirectManager.this);
        }
    }

    // request to termite
    public void requestGroupInfoList() {
        if (wifiP2pServiceBounded) {
            wifiP2pManager.requestGroupInfo(channel, WiFiDirectManager.this);
        }
    }

    // listener to receive the list of available peers
    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList simWifiP2pDeviceList) {
        updatePeerList(simWifiP2pDeviceList);
    }


    // listener to receive the list of device list
    @Override
    public void onGroupInfoAvailable(SimWifiP2pDeviceList simWifiP2pDeviceList, SimWifiP2pInfo simWifiP2pInfo) {
        updateDevicesInNetworkList(simWifiP2pInfo.getDevicesInNetwork());
    }


    //------------------------------- WiFiDirectManager----------------------------

    public ArrayList<String> getAvailableDevices() {
        if (updateVisibleleDevArray && this.visibleDevices != null) {
            visibleDevicesArray = new ArrayList<>();
            synchronized (this.visibleDevices) {
                for (SimWifiP2pDevice device : this.visibleDevices.getDeviceList()) {
                    visibleDevicesArray.add(device.deviceName);
                }
            }
            updateVisibleleDevArray = false;
        }
        return visibleDevicesArray;
    }

    public void notifyLocationListChanged() {
        updateCurrentLocation = true;
    }

    public long currentLocationsTimestamp() {
        return currentLocationsWriteTimestamp;
    }

    public ArrayList<LocationLocMess> getCurrentLocations() {
        if (updateCurrentLocation) {
            currentLocations = new ArrayList<>();
            synchronized (this.visibleDevices) {
                LocMessBackgroundService backgroundService = (LocMessBackgroundService) serviceContext;
                for (SimWifiP2pDevice device : this.visibleDevices.getDeviceList()) {
                    ArrayList<LocationLocMess> validLocations =
                            (ArrayList<LocationLocMess>) backgroundService.locationsManager.getValidLocations();
                    for (LocationLocMess location : validLocations) {
                        if (location.getType().equals(LocationLocMess.LocationType.GPS.getType())) {
                            continue;
                        }
                        if (device.deviceName.equals(location.getSsid())) {
                            currentLocations.add(location);
                        }
                    }
                }
            }
            updateCurrentLocation = false;
            currentLocationsWriteTimestamp += 1;
        }
        return currentLocations;
    }

    public boolean isNewCurrentLocations() {
        synchronized (this) {
            return updateCurrentLocation;
        }
    }


    private void updatePeerList(SimWifiP2pDeviceList listWithUpdates) {
        updateCurrentLocation = true;
        this.updateVisibleleDevArray = true;

        synchronized (this.visibleDevices) {
            this.visibleDevices.clear();
            this.visibleDevices.mergeUpdate(listWithUpdates);
        }
    }

    private void updateDevicesInNetworkList(Set<String> devicesName) {
        updateCurrentLocation = true;

        // filter to obtain only devices in network
        SimWifiP2pDeviceList devicesInUpdatedNetwork = new SimWifiP2pDeviceList();
        for (String devName : devicesName) {
            if (devName.startsWith("DEV_")) {
                SimWifiP2pDevice device = visibleDevices.getByName(devName);
                devicesInUpdatedNetwork.update(device);
            }
        }

        // remove the history of sent posts for devices that have abandoned the network
        // remove the nearby posts of devices that have abandoned the network
        LocMessBackgroundService backgroundService = (LocMessBackgroundService) serviceContext;
        synchronized (this.devicesInNetwork) {
            for (SimWifiP2pDevice device : this.devicesInNetwork.getDeviceList()) {
                if (devicesInUpdatedNetwork.getByName(device.deviceName) == null) {
                    LocMessPreferences.removeSentMessageByDevice(serviceContext, device.deviceName);
                    backgroundService.postsManager.removeDecentrNearbyPostsByDevName(device.deviceName);
                    serviceContext.sendBroadcast(new Intent().setAction(PostFragment.SYNCHRONIZE_POSTS));
                }
            }
        }
        // update the network list
        synchronized (this.devicesInNetwork) {
            this.devicesInNetwork.clear();
            this.devicesInNetwork.mergeUpdate(devicesInUpdatedNetwork);
        }
    }


    public enum MessageType {
        CHECK_INTERESSES("CheckInteresses"),
        ACCEPT_MESSAGE("AcceptMessage"),
        REJECT_MESSAGE("RejectMessage"),
        UNPOST("Unpost"),
        CONFIRM_MULE("ConfirmMule");
        private String operationId;

        MessageType(String type) {
            this.operationId = type;
        }

        public String getOperationId() {
            return this.operationId;
        }
    }

    public class WiFIDirectMessage {
        public String operationId;
        public String interessesInJson;
        public String locationUrl;
        public String policy;
        public String post;

        public WiFIDirectMessage(MessageType type, String interessesInJson, String locationUrl, String policy) {
            this.operationId = type.getOperationId();
            this.interessesInJson = interessesInJson;
            this.locationUrl = locationUrl;
            this.policy = policy;
            this.post = null;
        }

        public WiFIDirectMessage(MessageType type) {
            this.operationId = type.getOperationId();
            this.interessesInJson = null;
            this.locationUrl = null;
            this.policy = null;
            this.post = null;
        }

        public WiFIDirectMessage(MessageType type, String post) {
            this.operationId = type.getOperationId();
            this.interessesInJson = null;
            this.locationUrl = null;
            this.policy = null;
            this.post = post;
        }
    }

    public void sendToMule(PostLocMess post){
        if (devicesInNetwork.getDeviceList().size() == 0) {
            Log.d("+sendToMule", "the network has 0 devices");
            return;
        }
        post.setSenderDeviceName(myDeviceName);

        String postStr = LocMessJsonUtils.toDecentralizePostJson(post);
        String digestOfMessage = NetworkUtils.hashMessage(postStr);

        WiFIDirectMessage conditionsMessage = new WiFIDirectMessage(MessageType.CONFIRM_MULE,
                null, post.getLocation().getUrl(), null);
        String conditionsMessageStr = LocMessJsonUtils.toWiFiDirectMsgJson(conditionsMessage);

        HashSet<String> mulesOfMessage =
                LocMessPreferences.getMulesByMessage(serviceContext, digestOfMessage);
        for (SimWifiP2pDevice device : devicesInNetwork.getDeviceList()) {
            if (mulesOfMessage.contains(device.deviceName)){
                Log.d("+sendToMule", device.deviceName + " already received the Post");
                continue;
            }
            else{
                Log.d("+sendToMule", "sending POST to " + device.deviceName);
                SendPostTask sendToMuleTask = new SendPostTask(device.getVirtIp(), device.getVirtPort());
                sendToMuleTask.execute(conditionsMessageStr, postStr);
                mulesOfMessage.add(device.deviceName);
            }
        }
        LocMessPreferences.addMulesByMessage(serviceContext, digestOfMessage, mulesOfMessage);
    }


    public void sendPostOfMule(PostLocMess post) {
        if (devicesInNetwork.getDeviceList().size() == 0) {
            Log.d("+sendPostOfMule", "the network has 0 devices");
            return;
        }
        String postStr = LocMessJsonUtils.toDecentralizePostJson(post);

        String interesses = LocMessJsonUtils.toInterestsJson(post.getInterests());
        WiFIDirectMessage conditionsMessage = new WiFIDirectMessage(MessageType.CHECK_INTERESSES,
                interesses, post.getLocation().getUrl(), post.getPolicy());
        String conditionsMessageStr = LocMessJsonUtils.toWiFiDirectMsgJson(conditionsMessage);

        for (SimWifiP2pDevice device : devicesInNetwork.getDeviceList()) {
            Log.d("+sendPostOfMule", "sending POST to " + device.deviceName);
            SendPostTask outgoingCommTask = new SendPostTask(device.getVirtIp(), device.getVirtPort());
            outgoingCommTask.execute(conditionsMessageStr, postStr);
        }
    }


    public void sendPost(PostLocMess post) {
        if (devicesInNetwork.getDeviceList().size() == 0) {
            Log.d("+broadcastPost", "the network has 0 devices");
            return;
        }
        post.setSenderDeviceName(myDeviceName);

        String postStr = LocMessJsonUtils.toDecentralizePostJson(post);
        String digestOfMessage = NetworkUtils.hashMessage(postStr);

        String interesses = LocMessJsonUtils.toInterestsJson(post.getInterests());
        WiFIDirectMessage conditionsMessage = new WiFIDirectMessage(MessageType.CHECK_INTERESSES,
                interesses, post.getLocation().getUrl(), post.getPolicy());
        String conditionsMessageStr = LocMessJsonUtils.toWiFiDirectMsgJson(conditionsMessage);

        for (SimWifiP2pDevice device : devicesInNetwork.getDeviceList()) {
            HashSet<String> deviceSentMessages = LocMessPreferences.getSentMessagesByDevice(serviceContext, device.deviceName);

            if (!deviceSentMessages.contains(digestOfMessage)) {
                Log.d("+broadcastPost", "sending POST to " + device.deviceName);
                SendPostTask outgoingCommTask = new SendPostTask(device.getVirtIp(), device.getVirtPort());
                outgoingCommTask.execute(conditionsMessageStr, postStr);
                deviceSentMessages.add(digestOfMessage);
                LocMessPreferences.addSentMessagesByDevice(serviceContext, device.deviceName, deviceSentMessages);
            } else {
                Log.d("+broadcastPost", device.deviceName + " already received the Post");
            }
        }
    }


    public void unpostPost(PostLocMess post) {
        if (devicesInNetwork.getDeviceList().size() == 0) {
            Log.d("+broadcastUnpost", "the network has 0 devices");
            return;
        }
        List<LocationLocMess> currLocations = getCurrentLocations();
        if (!currLocations.contains(post.getLocation())) {
            Log.d("+broadcastUnpost", "not in target location");
            return;
        }
        String postStr = LocMessJsonUtils.toDecentralizePostJson(post);
        String digestOfMessage = NetworkUtils.hashMessage(postStr);

        WiFIDirectMessage messageToSend = new WiFIDirectMessage(MessageType.UNPOST, postStr);
        String messageToSendJson = LocMessJsonUtils.toWiFiDirectMsgJson(messageToSend);

        for (SimWifiP2pDevice device : devicesInNetwork.getDeviceList()) {
            HashSet<String> deviceSentMessages = LocMessPreferences.getSentMessagesByDevice(serviceContext, device.deviceName);
            if (deviceSentMessages.contains(digestOfMessage)) {
                Log.d("+broadcastUnpost", "Unpost POST to " + device.deviceName);
                WiFiDirectManager.UnpostTask unpostTask = new WiFiDirectManager.UnpostTask(device.getVirtIp(), device.getVirtPort());
                unpostTask.execute(messageToSendJson);
                deviceSentMessages.remove(digestOfMessage);
                LocMessPreferences.addSentMessagesByDevice(serviceContext, device.deviceName, deviceSentMessages);
            }
        }

    }


    //---------------------------------- Tasks ----------------------------------------------

    public class IncommingCommTask extends AsyncTask<Void, SimWifiP2pSocket, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Log.d("+IncommTask", "IncommingCommTask started (" + this.hashCode() + ").");

            try {
                socketServer = new SimWifiP2pSocketServer(10001);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SimWifiP2pSocket sock = socketServer.accept();
                    publishProgress(sock);
                } catch (IOException e) {
                    Log.d("Error socket:", e.getMessage());
                    break;

                } catch (NullPointerException e) {
                    Log.d("Error socket:", e.getMessage());
                }

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(SimWifiP2pSocket... sockets) {
            super.onProgressUpdate(sockets);
            Log.d("+IncommingCommTask:", "Attending new client");
            WiFiDirectManager.AttendClientTask t = new WiFiDirectManager.AttendClientTask();
            t.execute(sockets[0]);
        }
    }

    private boolean checkConditions(WiFIDirectMessage receivedConditions) {
        LocMessBackgroundService backgroundService = (LocMessBackgroundService) serviceContext;
        GPSManager gpsManager = backgroundService.gpsManager;
        ArrayList<LocationLocMess> clientLocations = getCurrentLocations();
        clientLocations.addAll(gpsManager.getCurrentLocations());

        Log.d("+AttentClient: rcvCond:", "|---->");
        Log.d("+AttentClient: rcvCond:", "   Location = " + receivedConditions.locationUrl);
        boolean respectConditions = false;
        for (LocationLocMess location : clientLocations) {
            if (location.getUrl().equals(receivedConditions.locationUrl)) {
                respectConditions = true;
                break;
            }
        }
        if (!respectConditions) {
            return respectConditions;
        }
        Log.d("+AttentClient: rcvCond:", "   Policy = " + receivedConditions.policy);
        List<InterestLocMess> receivedInteresses = LocMessJsonUtils.toInterestsList(receivedConditions.interessesInJson);
        List<InterestLocMess> userInteresses = LocMessPreferences.getInstance().getUserInterestsList();
        for (InterestLocMess i : receivedInteresses) {
            Log.d("+AttentClient: rcvIntr:", "     " + i.getName() + " = " + i.getValue());
            if (receivedConditions.policy.equals(PostLocMess.WHITE_LIST) && userInteresses.contains(i)) {
                Log.d("+AttentClient:", "    WL match: " + i.getName() + " = " + i.getValue());
                return true;
            }
            if (receivedConditions.policy.equals(PostLocMess.BLACK_LIST) && userInteresses.contains(i)) {
                Log.d("+AttentClient:", "    BL match: " + i.getName() + " = " + i.getValue());
                return false;
            }
        }
        Log.d("+AttentClient: rcvCond:", "<----|");
        return true;
    }

    private boolean checkConditionsMule(WiFIDirectMessage receivedConditions){
        LocMessBackgroundService backgroundService = (LocMessBackgroundService) serviceContext;

        int maxMessages = LocMessPreferences.getInstance().getMessageHopsSetting();
        int currMessages = backgroundService.postsManager.numberOfThirdPartyPosts();

        boolean accept = false;
        if(currMessages < maxMessages){
            List<LocationLocMess> freqLocations = LocMessPreferences.getInstance().getFrequentLocationsSetting();
            for (LocationLocMess loc : freqLocations){
                if(loc.getUrl().equals(receivedConditions.locationUrl)){
                    accept = true;
                }
            }
        }
        return accept;
    }

    public class AttendClientTask extends AsyncTask<SimWifiP2pSocket, Void, Void> {

        @Override
        protected Void doInBackground(SimWifiP2pSocket... params) {
            SimWifiP2pSocket clientSocket = params[0];

            LocMessBackgroundService backgroundService = (LocMessBackgroundService) serviceContext;
            try {
                BufferedReader sockIn = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                String rcvMessage = sockIn.readLine();
                WiFIDirectMessage receivedWiFiDirectMsg = LocMessJsonUtils.toWiFiDirectMsgObject(rcvMessage);
                Log.d("+AttentClient: ", "(1) WiFiDirectMessage received");
                // normal message receive
                if (receivedWiFiDirectMsg.operationId.equals(MessageType.CHECK_INTERESSES.getOperationId())) {
                    if (checkConditions(receivedWiFiDirectMsg)) {
                        WiFIDirectMessage reply = new WiFIDirectMessage(MessageType.ACCEPT_MESSAGE);
                        String replyStr = LocMessJsonUtils.toWiFiDirectMsgJson(reply);
                        Log.d("+AttentClient: Reply:", "(2) accept");
                        clientSocket.getOutputStream().write((replyStr + "\n").getBytes());
                        String postJson = sockIn.readLine();
                        PostLocMess receivedPost = LocMessJsonUtils.toPostObj(postJson);
                        Log.d("+AttentClient: R:", "(received Post json) = " + postJson);
                        backgroundService.postsManager.addNearbyPost(receivedPost);
                        serviceContext.sendBroadcast(new Intent().setAction(PostFragment.SYNCHRONIZE_POSTS));
                        NotificationSender.createNotification(serviceContext);
                    } else {
                        WiFIDirectMessage reply = new WiFIDirectMessage(MessageType.REJECT_MESSAGE);
                        String replyStr = LocMessJsonUtils.toWiFiDirectMsgJson(reply);
                        Log.d("+AttentClient: Reply:", "(2) reject");
                        clientSocket.getOutputStream().write((replyStr + "\n").getBytes());
                    }
                }
                // unpost of message
                if (receivedWiFiDirectMsg.operationId.equals(MessageType.UNPOST.getOperationId())) {
                    Log.d("+AttentClient: Unpost:", "(2) accept unpost");
                    PostLocMess receivedPost = LocMessJsonUtils.toPostObj(receivedWiFiDirectMsg.post);
                    backgroundService.postsManager.removeNearbyPost(receivedPost);
                    serviceContext.sendBroadcast(new Intent().setAction(PostFragment.SYNCHRONIZE_POSTS));
                }
                // message to be mule
                if(receivedWiFiDirectMsg.operationId.equals(MessageType.CONFIRM_MULE.getOperationId())){
                    if(checkConditionsMule(receivedWiFiDirectMsg)){
                        Log.d("+AttentClient: Mule:", "(2) accept");
                        WiFIDirectMessage reply = new WiFIDirectMessage(MessageType.ACCEPT_MESSAGE);
                        String replyStr = LocMessJsonUtils.toWiFiDirectMsgJson(reply);
                        clientSocket.getOutputStream().write((replyStr + "\n").getBytes());

                        String postJson = sockIn.readLine();
                        PostLocMess receivedPost = LocMessJsonUtils.toPostObj(postJson);
                        Log.d("+AttentClient: Mule R:", "(received Post json) = " + postJson);
                        backgroundService.postsManager.addThirdPartyPost(receivedPost);

                    } else {
                        Log.d("+AttentClient: Mule:", "(2) reject");
                        WiFIDirectMessage reply = new WiFIDirectMessage(MessageType.REJECT_MESSAGE);
                        String replyStr = LocMessJsonUtils.toWiFiDirectMsgJson(reply);
                        clientSocket.getOutputStream().write((replyStr + "\n").getBytes());
                    }
                }

            } catch (IOException e) {
                Log.d("Error reading socket:", e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    Log.d("Error socket:", e.getMessage());
                    return null;
                }
            }

            return null;
        }
    }


    public class SendPostTask extends AsyncTask<String, Void, Void> {
        private String ip;
        private int port;

        public SendPostTask(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        protected Void doInBackground(String... params) {
            String wiFiDirMessageJson = params[0];
            String postJson = params[1];
            try {
                Log.d("+SendPostTask, sending:", "(1) Sending conditions");
                SimWifiP2pSocket clientSocket = new SimWifiP2pSocket(ip, port);
                clientSocket.getOutputStream().write((wiFiDirMessageJson + "\n").getBytes());

                BufferedReader sockIn = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                String response = sockIn.readLine();
                WiFIDirectMessage respMessage = LocMessJsonUtils.toWiFiDirectMsgObject(response);
                Log.d("+SendPostTask, reply:", "(2) " + respMessage.operationId);
                if (respMessage.operationId.equals(MessageType.ACCEPT_MESSAGE.getOperationId())) {
                    Log.d("+SendPostTask, sending:", "(3) post");
                    clientSocket.getOutputStream().write((postJson + "\n").getBytes());
                }
                clientSocket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    public class UnpostTask extends AsyncTask<String, Void, Void> {
        private String ip;
        private int port;

        public UnpostTask(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        protected Void doInBackground(String... params) {
            String wiFiDirMessageJson = params[0];
            try {
                Log.d("+UnpostTask:", "Unpost");
                SimWifiP2pSocket clientSocket = new SimWifiP2pSocket(ip, port);
                clientSocket.getOutputStream().write((wiFiDirMessageJson + "\n").getBytes());
                clientSocket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    //---------------------- Broadcast receiver to receive Termite events-----------------------
    private class SimWifiP2pBroadcastReceiver extends BroadcastReceiver {
        public SimWifiP2pBroadcastReceiver() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

                // This action is triggered when the Termite service changes state:
                // - creating the service generates the WIFI_P2P_STATE_ENABLED event
                // - destroying the service generates the WIFI_P2P_STATE_DISABLED event

                int state = intent.getIntExtra(SimWifiP2pBroadcast.EXTRA_WIFI_STATE, -1);
                if (state == SimWifiP2pBroadcast.WIFI_P2P_STATE_ENABLED) {
                    Log.d("+++++", "wifiDir enabled");
                    Toast.makeText(serviceContext, "WiFi Direct enabled",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(serviceContext, "WiFi Direct disabled",
                            Toast.LENGTH_SHORT).show();
                }

            } else if (SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()

                SimWifiP2pDeviceList devices = (SimWifiP2pDeviceList) intent.getSerializableExtra(
                        SimWifiP2pBroadcast.EXTRA_DEVICE_LIST);
                updatePeerList(devices);
                Log.d("+++", "Peers list changed (" + visibleDevices.getDeviceList().size() + ")");

                Toast.makeText(serviceContext, "Peer list changed",
                        Toast.LENGTH_SHORT).show();

            } else if (SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION.equals(action)) {

                SimWifiP2pInfo ginfo = (SimWifiP2pInfo) intent.getSerializableExtra(
                        SimWifiP2pBroadcast.EXTRA_GROUP_INFO);
                myDeviceName = ginfo.getDeviceName();
                Log.d("+++", "my device name = " + myDeviceName);

                Set<String> devicesName = ginfo.getDevicesInNetwork();
                updateDevicesInNetworkList(devicesName);

                Log.d("+++", "Network list actual (" + devicesInNetwork.getDeviceList().size() + ")");
                for (SimWifiP2pDevice d : devicesInNetwork.getDeviceList()) {
                    Log.d("   +++", " device in actual network = " + d.deviceName);
                }
                for (String devName : ginfo.getDevicesInNetwork()) {
                    Log.d("   +++", " device in received network = " + devName);
                }

                Toast.makeText(serviceContext, "Network membership changed",
                        Toast.LENGTH_SHORT).show();

            } else if (SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION.equals(action)) {

                SimWifiP2pInfo ginfo = (SimWifiP2pInfo) intent.getSerializableExtra(
                        SimWifiP2pBroadcast.EXTRA_GROUP_INFO);
                ginfo.print();
                Toast.makeText(serviceContext, "Group ownership changed",
                        Toast.LENGTH_SHORT).show();
            }

        }
    }


}

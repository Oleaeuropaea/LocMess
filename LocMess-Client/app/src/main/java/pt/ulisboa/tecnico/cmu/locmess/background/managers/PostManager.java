package pt.ulisboa.tecnico.cmu.locmess.background.managers;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.JsonObject;

import org.apache.commons.collections4.map.SingletonMap;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.PostFragment;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessLinkedHashSet;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NotificationSender;

import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.readObjectFromFile;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessFilesUtils.writeObjectToFile;

public class PostManager {
    public static final String TAG = PostManager.class.getCanonicalName();

    private Context serviceContext;
    WiFiDirectManager wiFiDirectManager;
    GPSManager gpsManager;
    LocationManager locationManager;

    private String mTimestamp;

    // nearby posts to show
    private final ArrayList<PostLocMess> nearbyPostsFromServer;
    private final ArrayList<PostLocMess> nearbyPostsFromDevices;

    private final ArrayList<PostLocMess> savedPostsCentralized;
    private final ArrayList<PostLocMess> savedPostsDecentralized;

    private final ArrayList<PostLocMess> postedPostsCentralized;
    private final ArrayList<PostLocMess> postedPostsDecentralized;

    private final LocMessLinkedHashSet<PostLocMess> nearbyPosts;
    private final LocMessLinkedHashSet<PostLocMess> postedPosts;
    private final LocMessLinkedHashSet<PostLocMess> savedPosts;

    private final HashSet<PostLocMess> thirdPartyPosts;

    private boolean postedPostsChanged;
    private long lastCurrentWifiTimestampDecentr = 0;
    private long lastCurrentWifiTimestampCentr = 0;

    public PostManager(Context serviceContext) {
        Log.d("+PostManager", "Posts Manager created!");
        this.serviceContext = serviceContext;
        LocMessBackgroundService backgroundService = (LocMessBackgroundService) serviceContext;
        wiFiDirectManager = backgroundService.wiFiDirectManager;
        gpsManager = backgroundService.gpsManager;
        locationManager = backgroundService.locationsManager;

        nearbyPosts = new LocMessLinkedHashSet<>();
        postedPosts = new LocMessLinkedHashSet<>();
        savedPosts = new LocMessLinkedHashSet<>();

        nearbyPostsFromServer = new ArrayList<>();
        nearbyPostsFromDevices = new ArrayList<>();
        loadNearbyPosts();

        postedPostsCentralized = new ArrayList<>();
        postedPostsDecentralized = new ArrayList<>();
        loadPostedPosts();

        savedPostsCentralized = new ArrayList<>();
        savedPostsDecentralized = new ArrayList<>();
        loadSavedPosts();

        thirdPartyPosts = new HashSet<>();
        loadThirdPartyPosts();

        postedPostsChanged = true;
    }

    private void loadNearbyPosts(){
        ArrayList<PostLocMess> decentrPosts = (ArrayList<PostLocMess>) readObjectFromFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_NEARBY_POSTS);
        if(decentrPosts != null){
            for(PostLocMess post : decentrPosts){
                addNearbyPost(post);
            }
        }
        ArrayList<PostLocMess> centrPosts = (ArrayList<PostLocMess>) readObjectFromFile(serviceContext, LocMessFilesUtils.CENTRALIZED_NEARBY_POSTS);
        if (centrPosts != null){
            for(PostLocMess post : centrPosts){
                addNearbyPost(post);
            }
        }
    }

    private void loadPostedPosts(){
        ArrayList<PostLocMess> decentrPosts = (ArrayList<PostLocMess>) readObjectFromFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_POSTED_POSTS);
        if(decentrPosts != null){
            for(PostLocMess post : decentrPosts){
                addPostedPost(post);
            }
        }
        ArrayList<PostLocMess> centrPosts = (ArrayList<PostLocMess>) readObjectFromFile(serviceContext, LocMessFilesUtils.CENTRALIZED_POSTED_POSTS);
        if (centrPosts != null){
            for(PostLocMess post : centrPosts){
                addPostedPost(post);
            }
        }
    }

    private void loadSavedPosts(){
        ArrayList<PostLocMess> decentrPosts = (ArrayList<PostLocMess>) readObjectFromFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_SAVED_POSTS);
        if(decentrPosts != null){
            for(PostLocMess post : decentrPosts){
                addSavedPost(post);
            }
        }
        ArrayList<PostLocMess> centrPosts = (ArrayList<PostLocMess>) readObjectFromFile(serviceContext, LocMessFilesUtils.CENTRALIZED_SAVED_POSTS);
        if (centrPosts != null){
            for(PostLocMess post : centrPosts){
                addSavedPost(post);
            }
        }
    }

    private void loadThirdPartyPosts(){
        HashSet<PostLocMess> thirdPartyPosts = (HashSet<PostLocMess>) readObjectFromFile(serviceContext, LocMessFilesUtils.THIRD_PARTY_POSTS);
        if(thirdPartyPosts != null){
            for(PostLocMess post : thirdPartyPosts){
                thirdPartyPosts.add(post);
            }
        }
    }

    public void addThirdPartyPost(PostLocMess post){
        if(thirdPartyPosts.add(post)){
            writeObjectToFile(serviceContext, LocMessFilesUtils.THIRD_PARTY_POSTS, Context.MODE_PRIVATE, thirdPartyPosts);
        }
    }

    public void removeThirdPartyPost(PostLocMess post){
        if(thirdPartyPosts.remove(post)){
            writeObjectToFile(serviceContext, LocMessFilesUtils.THIRD_PARTY_POSTS, Context.MODE_PRIVATE, thirdPartyPosts);
        }
    }

    public int numberOfThirdPartyPosts(){
        return thirdPartyPosts.size();
    }

    public ArrayList<PostLocMess> getThirdPartyPostsByLocation(LocationLocMess location) {
        ArrayList<PostLocMess> result = new ArrayList<>();
        synchronized (thirdPartyPosts) {
            for (PostLocMess p : this.thirdPartyPosts) {
                if (p.getLocation().equals(location)) {
                    result.add(p);
                }
            }
        }
        return result;
    }


    public void addNearbyPost(PostLocMess post) {
        if (nearbyPosts.add(post)) {
            if (post.isCentralizedMode()) {
                this.nearbyPostsFromServer.add(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.CENTRALIZED_NEARBY_POSTS, Context.MODE_PRIVATE, nearbyPostsFromServer);

            } else {
                this.nearbyPostsFromDevices.add(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_NEARBY_POSTS, Context.MODE_PRIVATE, nearbyPostsFromDevices);
            }
        }
    }

    public void removeNearbyPost(PostLocMess post) {
        if (nearbyPosts.remove(post)) {
            if (post.isCentralizedMode()) {
                this.nearbyPostsFromServer.remove(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.CENTRALIZED_NEARBY_POSTS, Context.MODE_PRIVATE, nearbyPostsFromServer);

            } else {
                this.nearbyPostsFromDevices.remove(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_NEARBY_POSTS, Context.MODE_PRIVATE, nearbyPostsFromDevices);

            }
        }
    }

    public void removeDecentrNearbyPostsByDevName(String deviceName) {
        ArrayList<PostLocMess> postsToRemove = new ArrayList<>();
        for (PostLocMess post : this.nearbyPostsFromDevices) {
            if (!post.isCentralizedMode() && deviceName.equals(post.getSenderDeviceName())) {
                postsToRemove.add(post);
            }
        }
        for (PostLocMess postToRemove : postsToRemove) {
            removeNearbyPost(postToRemove);
        }
        writeObjectToFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_NEARBY_POSTS, Context.MODE_PRIVATE, nearbyPostsFromDevices);
    }


    public void addPostedPost(PostLocMess post) {
        if (postedPosts.add(post)) {
            if (post.isCentralizedMode()) {
                this.postedPostsCentralized.add(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.CENTRALIZED_POSTED_POSTS, Context.MODE_PRIVATE, postedPostsCentralized);

            } else {
                this.postedPostsDecentralized.add(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_POSTED_POSTS, Context.MODE_PRIVATE, postedPostsDecentralized);

            }
            postedPostsChanged = true;
        }
    }

    public void removePostedPost(PostLocMess post) {
        if (postedPosts.remove(post)) {
            if (post.isCentralizedMode()) {
                this.postedPostsCentralized.remove(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.CENTRALIZED_POSTED_POSTS, Context.MODE_PRIVATE, postedPostsCentralized);

            } else {
                this.postedPostsDecentralized.remove(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_POSTED_POSTS, Context.MODE_PRIVATE, postedPostsDecentralized);

            }
            postedPostsChanged = true;
        }
    }

    public ArrayList<PostLocMess> getPostedDecentrPostsByLocation(LocationLocMess location) {
        ArrayList<PostLocMess> result = new ArrayList<>();
        synchronized (postedPostsDecentralized) {
            for (PostLocMess p : this.postedPostsDecentralized) {
                if (p.getLocation().equals(location)) {
                    result.add(p);
                }
            }
        }
        return result;
    }


    public void addSavedPost(PostLocMess post) {
        if(savedPosts.add(post)){
            if(post.isCentralizedMode()){
                this.savedPostsCentralized.add(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.CENTRALIZED_SAVED_POSTS, Context.MODE_PRIVATE, postedPostsCentralized);
            } else{
                this.savedPostsDecentralized.add(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_SAVED_POSTS, Context.MODE_PRIVATE, postedPostsCentralized);
            }
        }
    }

    public void removeSavedPost(PostLocMess post) {
        if(savedPosts.remove(post)){
            if(post.isCentralizedMode()){
                this.savedPostsCentralized.remove(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.CENTRALIZED_SAVED_POSTS, Context.MODE_PRIVATE, postedPostsCentralized);
            } else{
                this.savedPostsDecentralized.remove(post);
                writeObjectToFile(serviceContext, LocMessFilesUtils.DECENTRALIZED_SAVED_POSTS, Context.MODE_PRIVATE, postedPostsCentralized);
            }
        }

    }

    public LocMessLinkedHashSet<PostLocMess> getNearbyPosts() {
        return nearbyPosts;
    }

    public LocMessLinkedHashSet<PostLocMess> getPostedPosts() {
        return postedPosts;
    }

    public LocMessLinkedHashSet<PostLocMess> getSavedPosts() {
        return savedPosts;
    }

    public Thread runCentralizedPostRequester() {
        Thread thread = new Thread(new CentralizedPostRequester());
        thread.start();
        return thread;
    }

    public Thread runDecentralizedPostSender() {
        Thread thread = new Thread(new DecentralizedPostSender(), "DecentralizedPostSender");
        thread.start();
        return thread;
    }

    private class CentralizedPostRequester implements Runnable {
        @Override
        public void run() {
            try {
                Log.d("++CentralPostRequester", "Request posts for locations...");
                ArrayList<LocationLocMess> locations = new ArrayList<>();
                locations.addAll(gpsManager.getCurrentLocations());
                locations.addAll(wiFiDirectManager.getCurrentLocations());

                ArrayList<SingletonMap<String, String>> queryParams = new ArrayList<>();
                for (LocationLocMess location : locations) {
                    queryParams.add(new SingletonMap<>(
                            NetworkUtils.LOCATION, String.valueOf(location.getId())
                    ));
                }
                if (!queryParams.isEmpty()) {
                    if (mTimestamp != null) {
                        queryParams.add(new SingletonMap<>(NetworkUtils.TIMESTAMP, mTimestamp));
                    }
                    NetworkUtils.NetworkResult result =
                            NetworkUtils.getJsonFromUrl(NetworkUtils.POSTS_URL, queryParams);
                    int httpStatus = result.getHttpStatus();
                    if (httpStatus == HttpStatus.SC_OK) {
                        mTimestamp = LocMessJsonUtils.getTimestampJson(result.getHttpResult());
                        String resultJson = LocMessJsonUtils.getResultJson(result.getHttpResult());

                        List<PostLocMess> postsList = LocMessJsonUtils.toPostsList(resultJson);
                        String currUser = LocMessPreferences.getInstance().getUsername();

                        boolean notify = false;
                        for (PostLocMess post : postsList) {
                            if (!post.getSender().equals(currUser) &&
                                    !getSavedPosts().contains(post)) {
                                addNearbyPost(post);
                                notify = true;
                            }
                        }
                        if (notify) { NotificationSender.createNotification(serviceContext); }
                        serviceContext.sendBroadcast(new Intent().setAction(PostFragment.SYNCHRONIZE_POSTS));
                    } else if (httpStatus != HttpStatus.SC_NO_CONTENT) {
                        JsonObject jsonObject = LocMessJsonUtils.toJsonObj(result.getHttpResult());
                        Log.e(TAG, jsonObject.get("detail").toString());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class DecentralizedPostSender implements Runnable {
        @Override
        public void run() {
            Log.d("++DecentralizedSender", "CheckPosts");
            if (wiFiDirectManager.currentLocationsTimestamp() == lastCurrentWifiTimestampDecentr
                    && !postedPostsChanged && !gpsManager.isNewCurrentLocations()) {
                // same peer list and 0 new Posts to post
                Log.d("++DecentralizedSender", "---------->> Same Loc and 0 new Posts <<-------------");

            } else {
                lastCurrentWifiTimestampDecentr = wiFiDirectManager.currentLocationsTimestamp();
                postedPostsChanged = false;

                ArrayList<LocationLocMess> currLocations = wiFiDirectManager.getCurrentLocations();
                currLocations.addAll(gpsManager.getCurrentLocations());

                List<LocationLocMess> validLocations = locationManager.getValidLocations();

                Log.d("++DecentralizedSender", "----------------------->");
                for (LocationLocMess location : validLocations){
                    if(currLocations.contains(location)){
                        ArrayList<PostLocMess> postsToLoc = getPostedDecentrPostsByLocation(location);
                        Log.d("++DecentralizedSender", "    my posts to send: " + location.getName() + ": " + postsToLoc.size() + " posts");
                        for (PostLocMess post : postsToLoc){
                            wiFiDirectManager.sendPost(post);
                        }
                        postsToLoc.clear();
                        postsToLoc = getThirdPartyPostsByLocation(location);
                        Log.d("++DecentralizedSender", "    3th posts to send: " + location.getName() + ": " + postsToLoc.size() + " posts");
                        for (PostLocMess post : postsToLoc){
                            wiFiDirectManager.sendPostOfMule(post);
                            removeThirdPartyPost(post);
                        }
                    }
                    else{
                        ArrayList<PostLocMess> postsToMules = getPostedDecentrPostsByLocation(location);
                        Log.d("++DecentralizedSender", "     to mules: " + location.getName() + ": " + postsToMules.size() + " posts");
                        for (PostLocMess post : postsToMules){
                            wiFiDirectManager.sendToMule(post);
                        }
                    }
                }
                Log.d("++DecentralizedSender", "-------------------<<<<<");
            }
        }
    }
}

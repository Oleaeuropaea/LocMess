package pt.ulisboa.tecnico.cmu.locmess.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashSet;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

import static android.content.Context.MODE_PRIVATE;

public class LocMessPreferences {
    private final String LOGIN_STATE = "pt.ulisboa.tecnico.cmu.locmess.app.LOGIN_STATE";
    private final String SESSION_TOKEN = "pt.ulisboa.tecnico.cmu.locmess.app.SESSION_TOKEN";
    private final String USERNAME = "pt.ulisboa.tecnico.cmu.locmess.app.USERNAME";
    private final String USER_EMAIL = "pt.ulisboa.tecnico.cmu.locmess.app.USER_EMAIL";
    private final String USER_INTERESTS = "pt.ulisboa.tecnico.cmu.locmess.app.USER_INTERESTS";
    private final String USER_POSTS = "pt.ulisboa.tecnico.cmu.locmess.app.USER_POSTS";

    private final String LOCATION_MANAGER_TIMESTAMP = "pt.ulisboa.tecnico.cmu.locmess.app.LOCATION_TIMESTAMP";
    private final String INTEREST_MANAGER_TIMESTAMP = "pt.ulisboa.tecnico.cmu.locmess.app.INTEREST_TIMESTAMP";

    private final String WIFI_DIRECT_ENABLE_SETTING = "pt.ulisboa.tecnico.cmu.locmess.app.WIFI_DIRECT_ENABLE_SETTING";
    private final String MESSAGE_HOPS_SETTING = "pt.ulisboa.tecnico.cmu.locmess.app.MESSAGE_HOPS";
    private final String FREQUENT_LOCATIONS_SETTING = "pt.ulisboa.tecnico.cmu.locmess.app.FREQUENT_LOCATIONS_SETTING";


    private LocMessApplication mAppContext;
    private static LocMessPreferences mInstance;


    private LocMessPreferences() {
        mAppContext = LocMessApplication.getInstance();
    }

    public static synchronized LocMessPreferences getInstance() {
        if(mInstance == null) { mInstance = new LocMessPreferences(); }
        return mInstance;
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mAppContext);
    }

    public void setUserAfterLogIn(String sessionToken, String username, String userEmail) {
        Editor editor = getSharedPreferences().edit();
        editor.putBoolean(LOGIN_STATE, true)
                .putString(SESSION_TOKEN, sessionToken)
                .putString(USERNAME, username)
                .putString(USER_EMAIL, userEmail)
                .apply();
    }

    public boolean isLoggedIn() {
        return getSharedPreferences().getBoolean(LOGIN_STATE, false);
    }


    public String getSessionToken() {
        return getSharedPreferences().getString(SESSION_TOKEN, null);
    }

    public void setSessionToken(String sessionToken) {
        Editor editor = getSharedPreferences().edit();
        editor.putString(SESSION_TOKEN, sessionToken).apply();
    }

    public String getLocationManagerTimestamp() {
        return getSharedPreferences().getString(LOCATION_MANAGER_TIMESTAMP, null);
    }
    public void setLocationManagerTimestamp(String timestamp) {
        Editor editor = getSharedPreferences().edit();
        editor.putString(LOCATION_MANAGER_TIMESTAMP, timestamp).apply();
    }
    public String getInterestManagerTimestamp() {
        return getSharedPreferences().getString(INTEREST_MANAGER_TIMESTAMP, null);
    }
    public void setInterestManagerTimestamp(String timestamp) {
        Editor editor = getSharedPreferences().edit();
        editor.putString(INTEREST_MANAGER_TIMESTAMP, timestamp).apply();
    }

    public String getUserInterestsUrl() {
        return NetworkUtils.USERS_URL + getUsername() + "/" + "interests" + "/";
    }

    public String getUserPostsUrl() {
        return NetworkUtils.USERS_URL + getUsername() + "/" + "posts" + "/";
    }

    public String getUsername() {
        return getSharedPreferences().getString(USERNAME, null);
    }

    public String getEmail() {
        return getSharedPreferences().getString(USER_EMAIL, null);
    }

    public void setUserInterestsJson(String userInterestsJson) {
        Editor editor = getSharedPreferences().edit();
        editor.putString(USER_INTERESTS, userInterestsJson).apply();
    }

    public List<InterestLocMess> getUserInterestsList() {
        String userInterests = getSharedPreferences().getString(USER_INTERESTS, null);
        return LocMessJsonUtils.toInterestsList(userInterests);
    }

    public void setUserPostsJson(String userPostsJson) {
        Editor editor = getSharedPreferences().edit();
        editor.putString(USER_POSTS, userPostsJson).apply();
    }

    public List<PostLocMess> getUserPostsList() {
        String userPosts = getSharedPreferences().getString(USER_POSTS, null);
        return LocMessJsonUtils.toPostsList(userPosts);
    }

    public boolean getWifiDirectEnableSetting() {
        return getSharedPreferences().getBoolean(WIFI_DIRECT_ENABLE_SETTING, false);
    }

    public void setWifiDirectEnableSetting(boolean wifiDirectEnableSetting) {
        Editor editor = getSharedPreferences().edit();
        editor.putBoolean(WIFI_DIRECT_ENABLE_SETTING, wifiDirectEnableSetting).apply();
    }

    public int getMessageHopsSetting() {
        return getSharedPreferences().getInt(MESSAGE_HOPS_SETTING, 0);
    }

    public void setMessageHopsSetting(int messageHopsSetting) {
        Editor editor = getSharedPreferences().edit();
        editor.putInt(MESSAGE_HOPS_SETTING, messageHopsSetting).apply();
    }

    public List<LocationLocMess> getFrequentLocationsSetting() {
        String json = getSharedPreferences().getString(FREQUENT_LOCATIONS_SETTING, null);
        if (json == null) { return null; }

        return LocMessJsonUtils.toLocationsList(json);
    }

    public void setFrequentLocationsSetting(String frequentLocationsSetting) {
        Editor editor = getSharedPreferences().edit();
        editor.putString(FREQUENT_LOCATIONS_SETTING, frequentLocationsSetting).apply();
    }

    public static HashSet<String> getSentMessagesByDevice(Context context, String deviceName){
        Gson gson = new Gson();
        HashSet<String> messagesSentToDevice;
        SharedPreferences preferences = context.getSharedPreferences("devicesReceivedPost", MODE_PRIVATE);
        String deviceJson = preferences.getString(deviceName, "");

        if(deviceJson.equals("")){
            messagesSentToDevice = new HashSet<>();
        } else{
            messagesSentToDevice = gson.fromJson(deviceJson, new TypeToken<HashSet<String>>(){}.getType());
        }
        return messagesSentToDevice;
    }

    public static void addSentMessagesByDevice(Context context, String deviceName, HashSet<String> messages){
        // update list of devices that already received the post
        Gson gson = new Gson();
        String messagesToJson = gson.toJson(messages, new TypeToken<HashSet<String>>(){}.getType());
        SharedPreferences preferences = context.getSharedPreferences("devicesReceivedPost", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(deviceName, messagesToJson);
        editor.apply();
    }

    public static void removeSentMessageByDevice(Context context, String deviceName){
        SharedPreferences preferences = context.getSharedPreferences("devicesReceivedPost", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(deviceName);
        editor.apply();
    }


    public static HashSet<String> getMulesByMessage(Context context, String message){
        Gson gson = new Gson();
        HashSet<String> mules;
        SharedPreferences preferences = context.getSharedPreferences("mulesReceivedPost", MODE_PRIVATE);
        String mulesStored = preferences.getString(message, "");

        if(mulesStored.equals("")){
            mules = new HashSet<>();
        } else{
            mules = gson.fromJson(mulesStored, new TypeToken<HashSet<String>>(){}.getType());
        }
        return mules;
    }

    public static void addMulesByMessage(Context context, String message, HashSet<String> mules){
        Gson gson = new Gson();
        String mulesToJson = gson.toJson(mules, new TypeToken<HashSet<String>>(){}.getType());
        SharedPreferences preferences = context.getSharedPreferences("mulesReceivedPost", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(message, mulesToJson);
        editor.apply();
    }

    public static void removeMuleByMessage(Context context, String message){
        SharedPreferences preferences = context.getSharedPreferences("mulesReceivedPost", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(message);
        editor.apply();
    }


    public void clearPreferences() {
        Editor editor = getSharedPreferences().edit();
        editor.clear().apply();
    }

}

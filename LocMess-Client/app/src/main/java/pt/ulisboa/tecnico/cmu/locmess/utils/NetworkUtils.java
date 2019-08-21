package pt.ulisboa.tecnico.cmu.locmess.utils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.http.HttpStatus;
import org.joda.time.LocalDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.PostManager;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.PostFragment;

public class NetworkUtils {
    private static final String TAG = NetworkUtils.class.getSimpleName();

    private static final int CONNECTIVITY_TIMEOUT = 30000;

    private static final String API_ROOT = "https://cmu-locmess.appspot.com/api/";

    public static final String REFRESH_TOKEN_URL = "https://cmu-locmess.appspot.com/api-token-refresh/";
    public static final String LOGIN_URL = "https://cmu-locmess.appspot.com/api/login/";
    public static final String LOGOUT_URL = "https://cmu-locmess.appspot.com/api/logout/";
    public static final String REGISTER_URL = "https://cmu-locmess.appspot.com/api/registration/";
    public static final String USERS_URL = "https://cmu-locmess.appspot.com/api/users/";
    public static final String INTERESTS_URL = "https://cmu-locmess.appspot.com/api/interests/";
    public static final String POSTS_URL = "https://cmu-locmess.appspot.com/api/posts/";
    public static final String LOCATIONS_URL = "https://cmu-locmess.appspot.com/api/locations/";

    public static final String TIMESTAMP = "timestamp";
    public static final String LOCATION = "location_id";

    private static boolean isNetworkAvailable = true;

    //-- -------------------------------------------------------------------------------------------
    public static boolean testAndSetNetworkConnectivity(Context context) {
        return isNetworkAvailable = testNetworkAccess(context);
    }

    private static boolean testNetworkAccess(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && pingServer();
    }

    private static boolean pingServer() {

        HttpURLConnection urlConnection = null;
        try {
            URL url = buildUrl(API_ROOT, null);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECTIVITY_TIMEOUT);
            urlConnection.connect();
        } catch (IOException e) {
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return true;
    }

    public static boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    public static void setIsNetworkAvailable(boolean isNetworkAvailable) {
        NetworkUtils.isNetworkAvailable = isNetworkAvailable;
    }

    //-- -------------------------------------------------------------------------------------------

    public static NetworkResult getJsonFromUrl(String url) throws IOException {
        URL urlToGet = buildUrl(url, null);
        HttpURLConnection urlConnection = (HttpURLConnection) urlToGet.openConnection();
        urlConnection.setRequestMethod("GET");

        return executeHttpMRequest(urlConnection);
    }

    public static NetworkResult getJsonFromUrl(String url, List<SingletonMap<String, String>> queryParams)
            throws IOException {
        URL urlToGet = buildUrl(url, queryParams);
        HttpURLConnection urlConnection = (HttpURLConnection) urlToGet.openConnection();
        urlConnection.setRequestMethod("GET");

        return executeHttpMRequest(urlConnection);
    }

    public static NetworkResult getJsonFromUrl(String url, SingletonMap<String, String> queryParams)
            throws IOException {
        List<SingletonMap<String, String>> query = Collections.singletonList(queryParams);
        return getJsonFromUrl(url, query);
    }

    public static NetworkResult postJsonToUrl(String url, String json) throws IOException {
        URL urlToPost = buildUrl(url, null);
        HttpURLConnection urlConnection = (HttpURLConnection) urlToPost.openConnection();
        urlConnection.setRequestMethod("POST");

        return executeHttpMRequest(urlConnection, json);
    }

    public static NetworkResult putJsonToUrl(String url, String json) throws IOException {
        URL urlToPut = buildUrl(url, null);
        HttpURLConnection urlConnection = (HttpURLConnection) urlToPut.openConnection();
        urlConnection.setRequestMethod("PUT");

        return executeHttpMRequest(urlConnection, json);
    }

    public static NetworkResult deleteObjFromUrl(String url) throws IOException {
        URL urlToDel = buildUrl(url, null);
        HttpURLConnection urlConnection = (HttpURLConnection) urlToDel.openConnection();
        urlConnection.setRequestMethod("DELETE");

        return executeHttpMRequest(urlConnection);
    }

    private static URL buildUrl(String baseUrl, List<SingletonMap<String, String>> queryParams)
            throws MalformedURLException {
        Uri.Builder builtUri = Uri.parse(baseUrl).buildUpon();

        if (queryParams != null) {
            for (KeyValue<String, String> queryParam : queryParams) {
                builtUri.appendQueryParameter(
                        queryParam.getKey(), queryParam.getValue()
                );
            }
        }
        builtUri.build();
        return new URL(builtUri.toString());
    }

    private static NetworkResult executeHttpMRequest(HttpURLConnection urlConnection)
            throws IOException {

        setConnectionParams(urlConnection);

        String jsonResult = getUrlConnectionResponse(urlConnection);
        int httpStatus = urlConnection.getResponseCode();

        return new NetworkResult(jsonResult, httpStatus);
    }

    private static NetworkResult executeHttpMRequest(HttpURLConnection urlConnection, String data)
            throws IOException {

        setConnectionParams(urlConnection);
        writeToUrlConnection(urlConnection, data);
        String result = getUrlConnectionResponse(urlConnection);
        int httpStatus = urlConnection.getResponseCode();

        return new NetworkResult(result, httpStatus);
    }

    private static void setConnectionParams(HttpURLConnection urlConnection) {
        String token = LocMessPreferences.getInstance().getSessionToken();
        if (token != null) {
            urlConnection.setRequestProperty("Authorization", "JWT " + token);
        }
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setRequestProperty("Content-type", "application/json");

        urlConnection.setConnectTimeout(CONNECTIVITY_TIMEOUT);
    }

    private static void writeToUrlConnection(HttpURLConnection urlConnection, String data)
            throws IOException {
        urlConnection.connect();

        OutputStream outputStream = urlConnection.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
        outputStreamWriter.write(data);

        outputStreamWriter.flush();
        outputStreamWriter.close();
    }

    private static String getUrlConnectionResponse(HttpURLConnection urlConnection)
            throws IOException {
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return "";
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public static class NetworkResult {
        String httpResult;
        int httpStatus;

        private NetworkResult(String httpResult, int httpStatus) {
            this.httpResult = httpResult;
            this.httpStatus = httpStatus;
        }

        public String getHttpResult() {
            return httpResult;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }


    public static String hashMessage(String messageStr) {
        byte[] message = messageStr.getBytes(StandardCharsets.UTF_8);
        byte[] result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(message);
            result = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(result, Base64.DEFAULT);
    }

    // ----------------------------- Tasks ---------------------------------------------
    public static void fetchUserInterestsTask(Context context) {
        new FetchUserInterestsTask(context).execute();
    }

    public static void fetchUserPostsTask(Context context) {
        new FetchUserPostsTask(context).execute();
    }

    public static void logoutUserTask(Context context) {
        new LogoutUserTask(context).execute();
    }


    /**
     * Represents an asynchronous task used to fetch user interests
     */
    private static class FetchUserInterestsTask extends AsyncTask<Void, Void, NetworkResult> {
        Context mContext;

        FetchUserInterestsTask(Context context) {
            mContext = context;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String url = LocMessPreferences.getInstance().getUserInterestsUrl();

            try {
                return NetworkUtils.getJsonFromUrl(url);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            if (result != null) {
                if (result.getHttpStatus() == HttpStatus.SC_OK) {
                    LocMessPreferences.getInstance().setUserInterestsJson(result.getHttpResult());
                    return;
                }
            }
            Toast.makeText(
                    mContext,
                    "Network error, Something went wrong, cannot update interests from server.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Represents an asynchronous task used to fetch user posts
     */
    private static class FetchUserPostsTask extends AsyncTask<Void, Void, NetworkResult> {
        Context mContext;

        FetchUserPostsTask(Context context) {
            mContext = context;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String url = LocMessPreferences.getInstance().getUserPostsUrl();
            try {
                return NetworkUtils.getJsonFromUrl(url);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            if (result != null) {
                if (result.getHttpStatus() == HttpStatus.SC_OK) {
                    LocMessApplication application = (LocMessApplication) mContext.getApplicationContext();
                    PostManager postsManager = application.locMessBackgroundService.postsManager;

                    LocMessPreferences.getInstance().setUserPostsJson(result.getHttpResult());
                    List<PostLocMess> userPosts = LocMessJsonUtils.toPostsList(result.getHttpResult());
                    for (PostLocMess userPost : userPosts) {
                        postsManager.addPostedPost(userPost);
                    }
                    application.locMessBackgroundService.sendBroadcast(new Intent().setAction(PostFragment.SYNCHRONIZE_POSTS));
                    return;
                }
            }
            Toast.makeText(
                    mContext,
                    "Network error, Something went wrong, cannot update posts from server.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Represents an asynchronous logout task used to logout user
     */
    private static class LogoutUserTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        Context mContext;

        LogoutUserTask(Context context) {
            mContext = context;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            JsonObject json = new JsonObject();
            json.addProperty("date_time", LocalDateTime.now().toString());

            try {
                return NetworkUtils.postJsonToUrl(NetworkUtils.LOGOUT_URL, json.toString());
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            if (result == null) {
                Toast.makeText(
                        mContext, "Network error, cannot logout with server", Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}

package pt.ulisboa.tecnico.cmu.locmess.background.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonObject;

import org.apache.commons.collections4.map.SingletonMap;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessLinkedHashSet;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.database.DatabaseHandler;

public class InterestManager {
    public static final String TAG = InterestManager.class.getCanonicalName();

    private String mTimestamp;
    private Context mContext;
    private final LocMessLinkedHashSet<InterestLocMess> mInterests = new LocMessLinkedHashSet<>();
    private final DatabaseHandler database;

    public InterestManager(Context context) {
        mContext = context;
        database = new DatabaseHandler(mContext);
        getAllInterests();
        mTimestamp = LocMessPreferences.getInstance().getInterestManagerTimestamp();
    }

    public LocMessLinkedHashSet<InterestLocMess> getInterests() {
        return mInterests;
    }

    public void fetchGlobalInterestsTask(Context context) {
        new FetchGlobalInterestsTask(context).execute();
    }

    private void getAllInterests() {
        mInterests.addAll(database.getAllInterestLocMess());
    }

    private void updateInterestList(List<InterestLocMess> listInterests) {
        for (InterestLocMess interest : listInterests) {
            mInterests.add(interest);
            database.insertInterest(interest);
        }
    }


    private class FetchGlobalInterestsTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        Context mContext;

        FetchGlobalInterestsTask(Context context) {
            mContext = context;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            try {
                if (mTimestamp != null) {
                    SingletonMap<String, String> queryParam = new SingletonMap<>(NetworkUtils.TIMESTAMP, mTimestamp);
                    return NetworkUtils.getJsonFromUrl(NetworkUtils.INTERESTS_URL, queryParam);
                } else {
                    return NetworkUtils.getJsonFromUrl(NetworkUtils.INTERESTS_URL);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            if (result != null) {
                int httpStatus = result.getHttpStatus();
                if (httpStatus == HttpStatus.SC_OK) {
                    mTimestamp = LocMessJsonUtils.getTimestampJson(result.getHttpResult());
                    LocMessPreferences.getInstance().setInterestManagerTimestamp(mTimestamp);

                    String resultJson = LocMessJsonUtils.getResultJson(result.getHttpResult());

                    List<InterestLocMess> interestList =
                            LocMessJsonUtils.toInterestsList(resultJson);
                    updateInterestList(interestList);
                } else if (httpStatus != HttpStatus.SC_NO_CONTENT) {
                    JsonObject jsonObject = LocMessJsonUtils.toJsonObj(result.getHttpResult());
                    Log.e(TAG, jsonObject.get("detail").toString());
                }
            }
        }
    }
}

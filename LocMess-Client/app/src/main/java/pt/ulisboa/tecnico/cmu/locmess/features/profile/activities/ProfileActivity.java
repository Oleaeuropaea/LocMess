package pt.ulisboa.tecnico.cmu.locmess.features.profile.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.InterestManager;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.adapters.InterestAdapter;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

public class ProfileActivity extends BaseActivity
        implements InterestAdapter.NoticeDeleteInterestListener {
    public static final String TAG = ProfileActivity.class.getCanonicalName();

    private int mDrawerItemId;
    private InterestAdapter mInterestAdapter;
    private ArrayList<InterestLocMess> mUserInterestsList;
    private ListView mUserInterestsListView;
    private TextView mUsernameTextView;
    private TextView mEmailTextView;

    private View mAddInterestInclude;
    private AutoCompleteTextView mInterestName;
    private EditText mInterestValue;
    private ImageButton mInterestCancelBtn;
    private ImageButton mInterestSaveBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        super.onCreateDrawer(toolbar);
        mDrawerItemId = BaseActivity.sCurrDrawerItemId;

        mInterestManager.fetchGlobalInterestsTask(this);

        mUsernameTextView = (TextView) findViewById(R.id.profile_username);
        mEmailTextView = (TextView) findViewById(R.id.profile_email);
        mUsernameTextView.setText(LocMessPreferences.getInstance().getUsername());
        mEmailTextView.setText(LocMessPreferences.getInstance().getEmail());

        mAddInterestInclude = findViewById(R.id.add_interest_include);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.buttonAddKeyPair);
        if (NetworkUtils.isNetworkAvailable()) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAddInterestInclude.setVisibility(View.VISIBLE);
                }
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        mInterestCancelBtn = (ImageButton) findViewById(R.id.ib_cancel);
        mInterestCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInterestName.setError(null);
                mInterestValue.setError(null);
                mInterestName.setText("");
                mInterestValue.setText("");
                mAddInterestInclude.setVisibility(View.GONE);
            }
        });

        mInterestSaveBtn = (ImageButton) findViewById(R.id.ib_save);
        mInterestSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInterest();
            }
        });

        LocMessApplication application = (LocMessApplication) getApplicationContext();
        InterestManager interestManager = application.locMessBackgroundService.interestManager;

        mInterestName = (AutoCompleteTextView) findViewById(R.id.actv_name);
        ArrayList<InterestLocMess> interests = new ArrayList<>();
        interests.addAll(interestManager.getInterests());
        ArrayAdapter<InterestLocMess> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                interests);
        mInterestName.setAdapter(adapter);
        mInterestName.setThreshold(1);
        mInterestName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mInterestName.getError() == null) {
                    mInterestName.showDropDown();
                }
            }
        });

        mInterestValue = (EditText) findViewById(R.id.et_value);

        mUserInterestsList = (ArrayList<InterestLocMess>) LocMessPreferences.getInstance().getUserInterestsList();
        mInterestAdapter = new InterestAdapter(this, 0, mUserInterestsList);
        mInterestAdapter.setListener(this);

        mUserInterestsListView = (ListView) findViewById(R.id.list_interests);
        TextView empty = (TextView) findViewById(R.id.tv_empty);
        mUserInterestsListView.setEmptyView(empty);
        mUserInterestsListView.setAdapter(mInterestAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BaseActivity.sCurrDrawerItemId = mDrawerItemId;
    }

    @Override
    protected void onPause() {
        super.onPause();
        String interestsJson = LocMessJsonUtils.toInterestsJson(mUserInterestsList);
        LocMessPreferences.getInstance().setUserInterestsJson(interestsJson);
    }

    private void saveInterest() {
        if (TextUtils.isEmpty(mInterestName.getText())) {
            mInterestName.setError(getString(R.string.error_field_required));
            mInterestName.requestFocus();
            return;
        } else if (TextUtils.isEmpty(mInterestValue.getText())) {
            mInterestValue.setError(getString(R.string.error_field_required));
            mInterestValue.requestFocus();
            return;
        }

        String name = mInterestName.getText().toString();
        String value = mInterestValue.getText().toString();
        InterestLocMess interest = new InterestLocMess(name, value);

        if(!mUserInterestsList.contains(interest)) {
            new CreateUserInterestTask(interest).execute();
        }

        // Hide Keyboard
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        mInterestName.setText("");
        mInterestValue.setText("");
        mAddInterestInclude.setVisibility(View.GONE);
    }

    @Override
    public void onDeleteInterestClick(final InterestLocMess interest) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Delete Confirmation");
        alert.setMessage("If you proceed you may lose access to some Messages, do you want to continue? ");
        alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new ProfileActivity.DeleteUserInterestTask(ProfileActivity.this, mInterestAdapter, interest).execute();
                dialog.dismiss();
            }
        });
        alert.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    /**
     * Represents an asynchronous task used to save interest in server
     */
    private class CreateUserInterestTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        InterestLocMess mInterest;

        CreateUserInterestTask(InterestLocMess interest) {
            mInterest = interest;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String url = LocMessPreferences.getInstance().getUserInterestsUrl();
            String interestJson = LocMessJsonUtils.toInterestsJson(mInterest);
            try {
                return NetworkUtils.postJsonToUrl(url, interestJson);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            if (result != null) {
                int httpStatus = result.getHttpStatus();
                if (httpStatus == HttpStatus.SC_CREATED) {

                    InterestLocMess interest = LocMessJsonUtils.toInterestsObj(result.getHttpResult());

                    mUserInterestsList.add(interest);
                    mInterestAdapter.notifyDataSetChanged();

                    Toast.makeText(ProfileActivity.this, "InterestLocMess created.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(
                    ProfileActivity.this, "Network error, something went wrong", Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Represents an asynchronous task used to delete InterestLocMess from Server
     */
    public static class DeleteUserInterestTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        Context mContext;
        InterestAdapter mAdapter;
        InterestLocMess mInterest;

        public DeleteUserInterestTask(Context context, InterestAdapter adapter, InterestLocMess interest) {
            mContext = context;
            mAdapter = adapter;
            mInterest = interest;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            try {
                return NetworkUtils.deleteObjFromUrl(mInterest.getUrl());
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
                    mAdapter.getInterestList().remove(mInterest);
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(mContext, "InterestLocMess deleted.", Toast.LENGTH_LONG).show();
                } else {
                    JsonObject jsonObject = LocMessJsonUtils.toJsonObj(result.getHttpResult());
                    Toast.makeText(
                            mContext,
                            "Network error, " + jsonObject.get("detail").toString(),
                            Toast.LENGTH_LONG
                    ).show();
                }

            } else {
                Toast.makeText(
                        mContext, "Network error, something went wrong", Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}

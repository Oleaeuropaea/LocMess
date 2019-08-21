package pt.ulisboa.tecnico.cmu.locmess.features.posts.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.managers.LocationManager;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.DateTimePickerFragment;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.PostFragment;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.PostSendDialogFragment;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

public class PostCreationActivity extends BaseActivity
        implements PostSendDialogFragment.NoticeDialogListener,
        DateTimePickerFragment.NoticeDateTimePickerListener {

    public static final String TAG = PostCreationActivity.class.getCanonicalName();

    private Spinner mLocations;
    private TextView mWindowStartDate;
    private TextView mWindowEndDate;
    private EditText mSubject;
    private EditText mContent;

    private DateTimeFormatter mDateTimeFormatterClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LocMessApplication application = (LocMessApplication) getApplicationContext();
        LocMessBackgroundService locMessBackgroundService;
        while(true) {
            if (!((locMessBackgroundService = application.locMessBackgroundService) == null)) break;
        }

        LocationManager locationManager = locMessBackgroundService.locationsManager;
        ArrayList<LocationLocMess> locationsList = (ArrayList<LocationLocMess>) locationManager.getValidLocations();
        locationsList.add(0, LocationLocMess.newDummyInstance());

        ArrayAdapter<LocationLocMess> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mLocations = (Spinner) findViewById(R.id.sp_npost_location);
        mLocations.setAdapter(adapter);

        mWindowStartDate = (TextView) findViewById(R.id.et_npost_start_date);
        mWindowEndDate = (TextView) findViewById(R.id.et_npost_end_date);

        mSubject = (EditText) findViewById(R.id.et_npost_subject);
        mContent = (EditText) findViewById(R.id.et_npost_content);

        mDateTimeFormatterClient = DateTimeFormat.forPattern(getString(R.string.date_time_format_client));
    }

    public void onDateTimeClick(View view) {
        TextView textView = (TextView) view;
        textView.setError(null);

        Bundle bundle = new Bundle();
        bundle.putInt(DateTimePickerFragment.ARGUMENT_VIEW_ID, textView.getId());

        DateTimePickerFragment dateTimePicker = new DateTimePickerFragment();
        dateTimePicker.setArguments(bundle);
        dateTimePicker.show(getFragmentManager(), "datePicker");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_send_post) {
            if (verifyPost()) {
                PostSendDialogFragment postSendDialog = new PostSendDialogFragment();
                postSendDialog.show(getFragmentManager(), "postSendDialog");
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean verifyPost() {
        LocationLocMess location = (LocationLocMess) mLocations.getSelectedItem();
        String windowStartDate = mWindowStartDate.getText().toString();
        String windowEndDate = mWindowEndDate.getText().toString();
        String subject = mSubject.getText().toString();
        String content = mContent.getText().toString();


        if (location.isDummy()) {
            TextView spinnerError = (TextView) mLocations.getSelectedView();
            spinnerError.setError("");
            spinnerError.setTextColor(Color.RED);
            spinnerError.setText("Choose a location");
        } else if (StringUtils.isBlank(windowStartDate)) {
            mWindowStartDate.requestFocus();
            mWindowStartDate.setError("Choose a start date");
        } else if (StringUtils.isBlank(windowEndDate)) {
            mWindowEndDate.requestFocus();
            mWindowEndDate.setError("Choose a end date");
        } else if (StringUtils.isBlank(subject)) {
            mSubject.requestFocus();
            mSubject.setError("Empty subject");
        } else if (StringUtils.isBlank(content)) {
            mContent.requestFocus();
            mContent.setError("Empty content");
        } else {
            return true;
        }

        return false;
    }

    @Override
    public void onDateTimeSet(View view, LocalDateTime dateTime) {
        TextView textView = (TextView) view;

        if (textView.equals(mWindowStartDate) &&
                StringUtils.isNotBlank(mWindowEndDate.getText().toString())) {
            LocalDateTime endDate =
                    mDateTimeFormatterClient.parseLocalDateTime(mWindowEndDate.getText().toString());

            if (dateTime.isAfter(endDate)) {
                showDateTimeError(textView, "Stat date must be lesser than end date");
                return;
            }
        } else if (textView.equals(mWindowEndDate) &&
                StringUtils.isNotBlank(mWindowStartDate.getText().toString())) {
            LocalDateTime startDate =
                    mDateTimeFormatterClient.parseLocalDateTime(mWindowStartDate.getText().toString());
            if (dateTime.isBefore(startDate)) {
                showDateTimeError(textView, "End date must be greater than start date");
                return;
            }
        }

        textView.setText(dateTime.toString(getString(R.string.date_time_format_client)));
    }

    private void showDateTimeError(TextView textView, String errorMsg) {
        textView.setText("");
        textView.requestFocus();
        textView.setError(errorMsg);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialogFragment) {
        Dialog dialog = dialogFragment.getDialog();

        LocationLocMess location = (LocationLocMess) mLocations.getSelectedItem();

        RadioButton whiteListPolicy = (RadioButton) dialog.findViewById(R.id.rb_spost_white_list);
        String policy = whiteListPolicy.isChecked() ? PostLocMess.WHITE_LIST : PostLocMess.BLACK_LIST;
        List<InterestLocMess> restrictions = ((PostSendDialogFragment) dialogFragment).getRestrictionsList();
        RadioButton centralizeDeliveryMode = (RadioButton) dialog.findViewById(R.id.rb_spost_centralize);

        PostLocMess postLocMess = new PostLocMess(
                mSubject.getText().toString(),
                DateTime.now(),
                location,
                policy,
                mDateTimeFormatterClient.parseDateTime(mWindowStartDate.getText().toString()),
                mDateTimeFormatterClient.parseDateTime(mWindowEndDate.getText().toString()),
                restrictions,
                mContent.getText().toString(),
                centralizeDeliveryMode.isChecked()
        );
        postLocMess.setSender(LocMessPreferences.getInstance().getUsername());

        if (postLocMess.isCentralizedMode()) {
            new CreatePostTask(postLocMess).execute();
        } else {
            LocMessApplication application = (LocMessApplication) getApplicationContext();
            application.locMessBackgroundService.postsManager.addPostedPost(postLocMess);
        }

        setResult(Activity.RESULT_OK);
        finish();
    }

    /**
     * Represents an asynchronous task used to POST PostLocMess to server
     */
    private class CreatePostTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        PostLocMess mPost;

        CreatePostTask(PostLocMess post) {
            mPost = post;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String postJson = LocMessJsonUtils.toCentralizePostJson(mPost);

            try {
                return NetworkUtils.postJsonToUrl(NetworkUtils.POSTS_URL, postJson);
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

                    PostLocMess postLocMess = LocMessJsonUtils.toPostObj(result.getHttpResult());
                    LocMessApplication application = (LocMessApplication) getApplicationContext();
                    application.locMessBackgroundService.postsManager.addPostedPost(postLocMess);
                    application.locMessBackgroundService.sendBroadcast(new Intent().setAction(PostFragment.SYNCHRONIZE_POSTS));
                    Toast.makeText(PostCreationActivity.this, "Post created.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(
                    PostCreationActivity.this, "Network error, something went wrong", Toast.LENGTH_LONG
            ).show();
        }
    }
}

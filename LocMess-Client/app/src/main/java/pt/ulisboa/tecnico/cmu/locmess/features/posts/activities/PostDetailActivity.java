package pt.ulisboa.tecnico.cmu.locmess.features.posts.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpStatus;

import java.io.IOException;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.fragments.PostFragment;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.activities.BaseActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

public class PostDetailActivity extends BaseActivity {
    public static final String TAG = PostDetailActivity.class.getCanonicalName();

    LocMessApplication mApplication;

    private PostLocMess mPost;
    private int mPostType;

    private View mIncludePostInfo;

    private TextView mLocation;
    private TextView mAuthorName;
    private TextView mAuthorEmail;
    private TextView mSubject;
    private TextView mCreationDate;
    private TextView mContent;
    private TextView mWindowStartDate;
    private TextView mWindowEndDate;
    private TextView mPolicy;
    private TextView mDeliveryMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mApplication = (LocMessApplication) getApplicationContext();

        Intent intent = getIntent();
        mPost = (PostLocMess) intent.getSerializableExtra(PostFragment.EXTRA_POST);
        mPostType = intent.getIntExtra(PostFragment.EXTRA_POST_TYPE, 0);

        mIncludePostInfo = findViewById(R.id.include_post_posted);

        mLocation = (TextView) findViewById(R.id.tv_pdetail_location);
        mAuthorName = (TextView) findViewById(R.id.tv_pdetail_name);
        mAuthorEmail = (TextView) findViewById(R.id.tv_pdetail_email);
        mSubject = (TextView) findViewById(R.id.tv_pdetail_subject);
        mCreationDate = (TextView) findViewById(R.id.tv_pdetail_date_time);
        mContent = (TextView) findViewById(R.id.tv_pdetail_content);
        mWindowStartDate = (TextView) findViewById(R.id.tv_pdetail_from_date);
        mWindowEndDate = (TextView) findViewById(R.id.tv_pdetail_to_date);
        mPolicy = (TextView) findViewById(R.id.tv_pdetail_policy);
        mDeliveryMode = (TextView) findViewById(R.id.tv_pdetail_delivery_mode);

        mLocation.setText(mPost.getLocation().getName());
        mAuthorName.setText(mPost.getSender());
        mAuthorEmail.setText(mPost.getSenderEmail());
        mSubject.setText(mPost.getSubject());

        String creationDate = mPost.getCreationDate().toString(getString(R.string.date_time_format_client));
        mCreationDate.setText(creationDate);

        mContent.setText(mPost.getContent());

        if (mPost.getPolicy() != null) {
            ConstraintLayout extraInfoLayout = (ConstraintLayout) findViewById(R.id.cl_extra_info);
            extraInfoLayout.setVisibility(View.VISIBLE);

            String startDate =
                    mPost.getStartDate().toString(getString(R.string.date_time_format_client));
            String endDate =
                    mPost.getEndDate().toString(getString(R.string.date_time_format_client));

            mWindowStartDate.setText(startDate);
            mWindowEndDate.setText(endDate);

            mPolicy.setText(mPost.getPolicy());
            mDeliveryMode.setText(mPost.getDeliveryMode());
        }
    }

    public void onInfoButtonClick(View view) {
        if (mIncludePostInfo.getVisibility() == View.GONE) {
            mIncludePostInfo.setVisibility(View.VISIBLE);
        } else {
            mIncludePostInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        switch (mPostType) {
            case PostFragment.NEARBY_POST:
                getMenuInflater().inflate(R.menu.post_detail_nearby, menu);
                getSupportActionBar().setTitle(R.string.title_menu_post_detail_nearby);
                break;
            case PostFragment.POSTED_POST:
                if (NetworkUtils.isNetworkAvailable()) {
                    getMenuInflater().inflate(R.menu.post_detail_posted, menu);
                    getSupportActionBar().setTitle(R.string.title_menu_post_detail_posted);
                }
                break;
            case PostFragment.SAVED_POST:
                getMenuInflater().inflate(R.menu.post_detail_saved, menu);
                getSupportActionBar().setTitle(R.string.title_menu_post_detail_saved);
                break;
            default:
                return super.onCreateOptionsMenu(menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_post:
                mApplication.locMessBackgroundService.postsManager.removeNearbyPost(mPost);
                mApplication.locMessBackgroundService.postsManager.addSavedPost(mPost);
                Toast.makeText(this, "Post Saved", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case R.id.action_unpost:
                if(mPost.isCentralizedMode()) {
                    new DeletePostTask(mPost).execute();
                } else {
                    mApplication.locMessBackgroundService.wiFiDirectManager.unpostPost(mPost);
                    mApplication.locMessBackgroundService.postsManager.removePostedPost(mPost);
                    mApplication.locMessBackgroundService.postsManager.addSavedPost(mPost);
                    Toast.makeText(this, "Post Unposted", Toast.LENGTH_SHORT).show();
                }
                finish();
                break;
            case R.id.action_delete_post:
                mApplication.locMessBackgroundService.postsManager.removeSavedPost(mPost);
                finish();
                Toast.makeText(this, "Post Deleted", Toast.LENGTH_SHORT).show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Represents an asynchronous task used to DELETE PostLocMess from server
     */
    private class DeletePostTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        PostLocMess mPost;

        DeletePostTask(PostLocMess post) {
            mPost = post;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String url = mPost.getUrl();
            try {
                return NetworkUtils.deleteObjFromUrl(url);
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
                    mApplication.locMessBackgroundService.postsManager.removePostedPost(mPost);
                    mApplication.locMessBackgroundService.postsManager.addSavedPost(mPost);
                    mApplication.locMessBackgroundService.sendBroadcast(new Intent().setAction(PostFragment.SYNCHRONIZE_POSTS));
                    Toast.makeText(PostDetailActivity.this, "Post Unposted", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(
                    PostDetailActivity.this, "Network error, something went wrong", Toast.LENGTH_LONG
            ).show();
        }
    }
}

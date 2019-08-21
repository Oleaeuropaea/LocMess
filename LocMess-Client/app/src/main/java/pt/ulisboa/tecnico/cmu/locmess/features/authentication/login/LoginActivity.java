package pt.ulisboa.tecnico.cmu.locmess.features.authentication.login;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;

import java.io.IOException;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.background.services.LocMessBackgroundService;
import pt.ulisboa.tecnico.cmu.locmess.features.authentication.register.RegisterActivity;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.activities.PostActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    public static final String TAG = LoginActivity.class.getCanonicalName();
    private static final int REQUEST_PERMISSIONS = 200;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        // attempt to login when user press enter
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mUsernameSignInButton = (Button) findViewById(R.id.sign_in_button);
        mUsernameSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        TextView hintText = (TextView) this.findViewById(R.id.hint_create_account);
        hintText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        if (LocMessPreferences.getInstance().isLoggedIn() && NetworkUtils.isNetworkAvailable()) {
            new RefreshTokenTask(getBaseContext()).execute();
        } else {
            requestTrackingPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "GPS features will not work", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
            }
        }
    }

    private void requestTrackingPermissions() {
        String[] perms = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(this, perms, REQUEST_PERMISSIONS);
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        else if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }

    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void launchHomeActivity() {
        LocMessApplication application = (LocMessApplication) getApplicationContext();
        application.enableLocMessBackgroundService();

        Intent intent = new Intent(LoginActivity.this, PostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String loginJson = LocMessJsonUtils.toLoginJson(mUsername, mPassword);
            try {
                return NetworkUtils.postJsonToUrl(NetworkUtils.LOGIN_URL, loginJson);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            mAuthTask = null;
            showProgress(false);

            if (result != null) {
                JsonObject jsonObject = LocMessJsonUtils.toJsonObj(result.getHttpResult());
                int httpStatus = result.getHttpStatus();

                if (httpStatus == HttpStatus.SC_OK) {
                    String token = jsonObject.get("token").getAsString();
                    JsonObject userObject = jsonObject.get("user").getAsJsonObject();
                    String username = userObject.get("username").getAsString();
                    String email = userObject.get("email").getAsString();

                    LocMessPreferences.getInstance().setUserAfterLogIn(token, username, email);
                    NetworkUtils.fetchUserInterestsTask(LoginActivity.this);
                    NetworkUtils.fetchUserPostsTask(LoginActivity.this);
                    launchHomeActivity();
                } else {
                    mUsernameView.setText("");
                    mPasswordView.setText("");
                    String error = jsonObject.get("non_field_errors").getAsString();
                    mUsernameView.setError(error);
                }
            } else {
                Toast.makeText(
                        getBaseContext(), "Network error, please try again later", Toast.LENGTH_LONG
                ).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous task used to refresh token
     */
    private class RefreshTokenTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        Context mContext;

        RefreshTokenTask(Context context) {
            mContext = context;
            showProgress(true);
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String token = LocMessPreferences.getInstance().getSessionToken();
            String tokenJson = LocMessJsonUtils.toTokenJson(token);

            try {
                return NetworkUtils.postJsonToUrl(NetworkUtils.REFRESH_TOKEN_URL, tokenJson);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            showProgress(false);
            if (result != null) {
                if (result.getHttpStatus() == HttpStatus.SC_OK) {
                    JsonObject jsonObject = LocMessJsonUtils.toJsonObj(result.getHttpResult());
                    String token = jsonObject.get("token").getAsString();
                    LocMessPreferences.getInstance().setSessionToken(token);
                } else {
                    LocMessPreferences.getInstance().clearPreferences();
                    return;
                }
            } else {
                NetworkUtils.setIsNetworkAvailable(false);
                Toast.makeText(mContext, "Network error, No Connectivity.", Toast.LENGTH_LONG).show();
            }
            launchHomeActivity();
        }
    }

}


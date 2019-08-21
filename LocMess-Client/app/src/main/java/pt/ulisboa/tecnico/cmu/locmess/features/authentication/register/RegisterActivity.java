package pt.ulisboa.tecnico.cmu.locmess.features.authentication.register;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.regex.Matcher;

import pt.ulisboa.tecnico.cmu.locmess.R;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessApplication;
import pt.ulisboa.tecnico.cmu.locmess.app.LocMessPreferences;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.activities.PostActivity;
import pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils;
import pt.ulisboa.tecnico.cmu.locmess.utils.NetworkUtils;


public class RegisterActivity extends AppCompatActivity {
    public static final String TAG = PostActivity.class.getCanonicalName();

    /**
     * Keep track of the register task to ensure we can cancel it if requested.
     */
    private UserRegistrationTask mRegTask = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mEmailView;
    private EditText mPasswordView;
    private EditText mPasswordRepeatView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username_register);
        mEmailView = (EditText) findViewById(R.id.email_register);
        mPasswordView = (EditText) findViewById(R.id.password_register);
        mPasswordRepeatView = (EditText) findViewById(R.id.password_register_repeat);

        Button mSignInButton = (Button) findViewById(R.id.sign_up_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        TextView hintText = (TextView) this.findViewById(R.id.hint_login);
        hintText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mLoginFormView = findViewById(R.id.username_register_form);
        mProgressView = findViewById(R.id.register_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptRegister() {
        if (mRegTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mPasswordRepeatView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String passwordRepeat = mPasswordRepeatView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username, if the user entered one.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        // Check for a valid email address and if the user entered one.
        else if (TextUtils.isEmpty(email) || !isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid password1, if the user entered one.
        else if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid password repeat, if the user entered one.
        else if (TextUtils.isEmpty(passwordRepeat)) {
            mPasswordRepeatView.setError(getString(R.string.error_field_required));
            focusView = mPasswordRepeatView;
            cancel = true;
        }

        // Check if password == passwordRepeat
        else if (!password.equals(passwordRepeat)) {
            mPasswordView.setText("");
            mPasswordRepeatView.setText("");

            focusView = mPasswordView;
            mPasswordView.setError("Passwords didn't match");
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
            mRegTask = new UserRegistrationTask(this, username, email, password);
            mRegTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        Matcher matcher = Patterns.EMAIL_ADDRESS.matcher(email);
        return matcher.matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8;
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
        //LAUCH SERVICE
        LocMessApplication application = (LocMessApplication) getApplicationContext();
        application.enableLocMessBackgroundService();

        Intent intent = new Intent(this, PostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private class UserRegistrationTask extends AsyncTask<Void, Void, NetworkUtils.NetworkResult> {
        private final Context mContext;
        private final String mUsername;
        private final String mEmail;
        private final String mPassword;

        UserRegistrationTask(Context context, String username, String email, String password) {
            mContext = context;
            mUsername = username;
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected NetworkUtils.NetworkResult doInBackground(Void... params) {
            String registerJson = LocMessJsonUtils.toRegisterJson(mUsername, mEmail, mPassword, mPassword);
            try {
                return NetworkUtils.postJsonToUrl(NetworkUtils.REGISTER_URL, registerJson);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final NetworkUtils.NetworkResult result) {
            mRegTask = null;
            showProgress(false);

            if (result != null) {
                JsonObject jsonObject = LocMessJsonUtils.toJsonObj(result.getHttpResult());
                int httpStatus = result.getHttpStatus();

                if (httpStatus == HttpStatus.SC_CREATED) {
                    String token = jsonObject.get("token").getAsString();
                    JsonObject userObject = jsonObject.get("user").getAsJsonObject();
                    String username = userObject.get("username").getAsString();
                    String email = userObject.get("email").getAsString();

                    LocMessPreferences.getInstance().setUserAfterLogIn(token, username, email);
                    LocMessPreferences.getInstance().setUserInterestsJson("[]");
                    launchHomeActivity();
                } else {
                    String error;

                    if (jsonObject.has("username")) {
                        mUsernameView.setText("");
                        error = jsonObject.get("username").getAsString();
                        mUsernameView.setError(error);
                    }

                    if (jsonObject.has("email")) {
                        mEmailView.setText("");
                        error = jsonObject.get("email").getAsString();
                        mEmailView.setError(error);
                    }

                    if (jsonObject.has("password1")) {
                        mPasswordView.setText("");
                        mPasswordRepeatView.setText("");
                        error = jsonObject.get("password1").getAsString();
                        mPasswordView.setError(error);
                    }
                }
            } else {
                Toast.makeText(
                        getBaseContext(), "Network error, please try again later", Toast.LENGTH_LONG
                ).show();
            }
        }

        @Override
        protected void onCancelled() {
            mRegTask = null;
            showProgress(false);
        }
    }
}


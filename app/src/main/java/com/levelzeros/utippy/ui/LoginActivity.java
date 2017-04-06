package com.levelzeros.utippy.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.levelzeros.utippy.MainActivity;
import com.levelzeros.utippy.R;
import com.levelzeros.utippy.utility.NetworkUtils;
import com.levelzeros.utippy.utility.PreferenceUtils;

import java.net.URL;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private Context mContext = this;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private UserDetailTask mUserDetailTask = null;

    // UI references
    private EditText mStudentIdEditText;
    private EditText mPasswordEditText;
    private TextView mCancelOption;
    private View mProgressView;
    private View mLoginFormView;

    //Variables
    private String mDomain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().setElevation(0f);

        mDomain = getIntent().getStringExtra(WelcomeActivity.ELEARNING_DOMAIN);
        if (TextUtils.isEmpty(mDomain)) {
            Toast.makeText(mContext, getString(R.string.error_identify_domain), Toast.LENGTH_LONG).show();
            finish();
        }

        mCancelOption = (TextView) findViewById(R.id.login_cancel);
        mCancelOption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //TODO: Programmatically set EditText hint based on domain
        // Set up the login form.
        mStudentIdEditText = (EditText) findViewById(R.id.student_id);

        mPasswordEditText = (EditText) findViewById(R.id.password);
        mPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mStudentIdEditText.setError(null);
        mPasswordEditText.setError(null);

        // Store values at the time of the login attempt.
        String studentId = mStudentIdEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordEditText.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordEditText;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(studentId)) {
            mStudentIdEditText.setError(getString(R.string.error_field_required));
            focusView = mStudentIdEditText;
            cancel = true;
        } else if (!isStudentIdValid(studentId)) {
            mStudentIdEditText.setError(getString(R.string.error_invalid_email));
            focusView = mStudentIdEditText;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                Snackbar.make(mLoginFormView, getString(R.string.network_error), 10000)
                        .setAction(getString(R.string.prompt_retry), new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                attemptLogin();
                            }
                        })
                        .show();
            } else {
                showProgress(true);
                mCancelOption.setVisibility(View.GONE);
                mAuthTask = new UserLoginTask();
                mAuthTask.execute(studentId, password);
            }
        }
    }

    /**
     * Check student's username input validity
     *
     * @param username Username input for E-learning or ULearn credential
     * @return Check if the length of input is more than 3 characters
     */
    private boolean isStudentIdValid(String username) {
        return username.length() > 3;
    }

    /**
     * Check password input validity
     *
     * @param password Password input for E-learning or ULearn credential
     * @return Check if the length of password is not empty
     */
    private boolean isPasswordValid(String password) {
        return password.length() >= 0;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }


    /**
     * Update user's details upon success login, then proceed to MainActivity
     * else prompt to retry login
     *
     * @param initializationStatus Initialization status for login status
     */
    void onUserDetailsObtained(boolean initializationStatus) {
        showProgress(false);
        if (initializationStatus) {
            PreferenceUtils.updateInitializationStatus(mContext, initializationStatus);

            Intent intent = new Intent(mContext, MainActivity.class);
            mContext.startActivity(intent);

            finish();
        } else {
            mAuthTask = null;
            mUserDetailTask = null;
            mCancelOption.setVisibility(View.VISIBLE);

            mPasswordEditText.setError(getString(R.string.error_initialization));
            mPasswordEditText.requestFocus();
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<String, Void, String> {
        private static final String TAG = "UserLoginTask";

        @Override
        protected String doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            String mStudentID = params[0];
            String mPassword = params[1];

            URL webTokenRequestUrl = NetworkUtils.buildWebTokenQueryUrl(mStudentID, mPassword);

            //TODO:Edit network response based on domain API
            try {
                String jsonWebTokenResponse = NetworkUtils.getResponseFromHttpsUrl(webTokenRequestUrl, mContext);

                return NetworkUtils.getWebToken(jsonWebTokenResponse);

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String webToken) {
            mAuthTask = null;

            //Obtain user's detail upon successful login
            if (null != webToken && !webToken.isEmpty()) {

                PreferenceUtils.updateWebToken(mContext, webToken);

                mUserDetailTask = new UserDetailTask();
                mUserDetailTask.execute();

            } else {
                showProgress(false);
                mCancelOption.setVisibility(View.VISIBLE);

                mPasswordEditText.setError(getString(R.string.error_incorrect_password));
                mPasswordEditText.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }


    /**
     * Asynchronous task to obtain user's details
     */
    private class UserDetailTask extends AsyncTask<Void, Void, Boolean> {
        private static final String TAG = "UserDetailTask";

        @Override
        protected Boolean doInBackground(Void... params) {
            URL userDetailRequestUrl = NetworkUtils.buildUserDetailQueryUrl(mContext);

            try {
                String jsonWebTokenResponse = NetworkUtils.getResponseFromHttpsUrl(userDetailRequestUrl, mContext);

                return NetworkUtils.getUserDetail(jsonWebTokenResponse, mContext);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean initializationStatus) {
            onUserDetailsObtained(initializationStatus);
        }
    }
}



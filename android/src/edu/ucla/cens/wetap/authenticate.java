package edu.ucla.cens.wetap;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.util.Log;

import com.google.android.googlelogin.GoogleLoginServiceBlockingHelper;
import com.google.android.googlelogin.GoogleLoginServiceConstants;
import com.google.android.googlelogin.GoogleLoginServiceHelper;
import com.google.android.googlelogin.GoogleLoginServiceNotFoundException;

public class authenticate extends Activity {
	private static final String TAG = "Authentication";
    private static final int GET_ACCOUNT_REQUEST = 1;

	public static HttpClient httpClient;
    private SharedPreferences preferences;
    private String authToken = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        Log.d(TAG, "started authenticate intent");

        preferences = this.getSharedPreferences(getString(R.string.preferences), Activity.MODE_PRIVATE);
        preferences.edit().putBoolean("authenticated", false).commit();

        Log.d(TAG, "set initial auth state to false");


        GoogleLoginServiceHelper.getCredentials(this, GET_ACCOUNT_REQUEST,
                                                null,
                                                GoogleLoginServiceConstants.PREFER_HOSTED,
                                                "ah", true);
	}

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "got activity result");
        if (GET_ACCOUNT_REQUEST == requestCode) {
            authToken = intent.getStringExtra(GoogleLoginServiceConstants.AUTHTOKEN_KEY);
            Log.d(TAG, "got authToken: " + authToken);
            auth();
        }
    }

    private void auth() {
        Log.d(TAG, "login with google account tied to phone");

        Log.d(TAG, "get authtoken from google");

        httpClient = new DefaultHttpClient();
        String base_url = getString(R.string.baseurl);
        HttpGet auth_request = new HttpGet(base_url + "/_ah/login?continue=" +
                                           base_url + "&auth=" + authToken);

        try {
            HttpResponse response = httpClient.execute(auth_request);
            int status = response.getStatusLine().getStatusCode();

            Log.d(TAG, "send authtoken to appengine: " + base_url + "/_ah/login?continue=" + base_url + "&auth=" + authToken);

            Log.d(TAG, "auth status: " + status);
            if (HttpStatus.SC_OK == status) {
                preferences.edit().putBoolean("authenticated", true).commit();
                Log.d(TAG, "authentication successful");

//                authenticate.this.startActivity(new Intent(authenticate.this, survey.class));
//                Log.d(TAG, "started survey intent");
//                startService(new Intent(authenticate.this, survey_upload.class));
//                Log.d(TAG, "started survey upload intent");
                authenticate.this.startActivity(new Intent(authenticate.this, map.class));
                Log.d(TAG, "started map activity");
                authenticate.this.finish();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "auth was a failure");

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Could not authenticate")
            .setMessage("You can continue to take an observation but you must be able to authenticate before any data can be uploaded.")
            .setCancelable(false)
            .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    authenticate.this.startActivity(new Intent(authenticate.this, survey.class));
                    Log.d(TAG, "started survey intent");
                    startService(new Intent(authenticate.this, survey_upload.class));
                    Log.d(TAG, "started survey upload intent");
                    authenticate.this.finish();
                    return;
                }
            });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}

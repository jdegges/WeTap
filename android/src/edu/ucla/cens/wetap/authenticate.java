package edu.ucla.cens.wetap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.content.StringBody;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;

import android.view.View;

import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class authenticate extends Activity implements Runnable {
    private EditText et_email;
    private EditText et_pass;
    private CheckBox cb_save_login;
    private String email;
    private String pass;
    private boolean save_login;

	private static final String TAG = "Authentication";
    private static final int DIALOG_PROGRESS = 1;

	public static HttpClient httpClient;
    private SharedPreferences preferences;
    private String authToken = "";
    private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.authenticate);
        et_email = (EditText) findViewById(R.id.email);
        et_pass = (EditText) findViewById(R.id.password);
        cb_save_login = (CheckBox) findViewById(R.id.save_login);
        Button submit = (Button) findViewById(R.id.login);

        if (null != savedInstanceState &&
            savedInstanceState.getBoolean("dialogisshowing"))
        {
            showDialog(DIALOG_PROGRESS);
        }

        
        Log.d(TAG, "started authenticate intent");

        preferences = this.getSharedPreferences(getString(R.string.preferences), Activity.MODE_PRIVATE);
        preferences.edit().putBoolean("authenticated", false).commit();
        Log.d(TAG, "set initial auth state to false");

        email = preferences.getString("email", "");
        pass = preferences.getString("pass", "");
        save_login = preferences.getBoolean("save_login", false);

        if (!email.equals("") && !pass.equals("") && save_login) {
            Log.d(TAG, "SETTING VALUES");
            et_email.setText(email);
            et_pass.setText(pass);
            cb_save_login.setChecked(save_login);
        }

        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick (View view) {
                email = et_email.getText().toString();
                pass = et_pass.getText().toString();
                save_login = cb_save_login.isChecked();

                if (save_login) {
                    preferences.edit().putString("email", email)
                                      .putString("pass", pass)
                                      .putBoolean("save_login", true)
                                      .commit();
                }

                showDialog (DIALOG_PROGRESS);
                Thread thread = new Thread(authenticate.this);
                thread.start();
            }
        });
	}

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_PROGRESS:
            mProgressDialog = new ProgressDialog(authenticate.this);
            mProgressDialog.setTitle("Working");
            mProgressDialog.setMessage("Authenticating With Google");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            return mProgressDialog;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mProgressDialog != null) {
            dismissDialog(DIALOG_PROGRESS);
            mProgressDialog = null;
        }
    }

    public void run() {
        Message msg = new Message();
        Bundle b = new Bundle();

        b.putBoolean("authenticated", auth());
        msg.setData(b);

        handler.sendMessage(msg);
        handler.sendEmptyMessage(0);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mProgressDialog.dismiss();
            if(msg.getData().getBoolean("authenticated")) {
                authenticate.this.startActivity(new Intent(authenticate.this, home.class)); /// XXX survey.class
                Log.d(TAG, "started survey intent");
                startService(new Intent(authenticate.this, survey_upload.class));
                Log.d(TAG, "started survey upload intent");
                authenticate.this.finish();
            } else {
                //authentication failed
                auth_failed();
            }

        }
    };

    private boolean auth() {
        Log.d(TAG, "login with google account: " + email + ", " + pass);
        httpClient = new DefaultHttpClient();
        HttpPost request = new HttpPost("https://www.google.com/accounts/ClientLogin");
        try {
            MultipartEntity entity = new MultipartEntity();
            entity.addPart("Email", new StringBody(et_email.getText().toString()));
            entity.addPart("Passwd", new StringBody(et_pass.getText().toString()));
            entity.addPart("service", new StringBody("ah"));
            request.setEntity(entity);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
        try {
            
            HttpResponse response = httpClient.execute(request);
            Log.d(TAG, "Doing Google HTTPS Request");
            int status = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != status) {
                Log.d(TAG, "got status: " + status); 
                Log.d(TAG, generateString(response.getEntity().getContent()));
                return false;
            }
            authToken = generateString(response.getEntity().getContent()).split("\n")[2].substring(5);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Log.d(TAG, "google auth successful!");


        Log.d(TAG, "authenticate with appspot");

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

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void auth_failed() {
        Log.d(TAG, "auth was a failure");
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Could not authenticate")
            .setMessage("You can continue to take an observation but you must be able to authenticate before any data can be uploaded.")
            .setCancelable(false)
            .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    authenticate.this.startActivity(new Intent(authenticate.this, home.class)); // XXX survey.class
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

    public String generateString(InputStream stream) {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();

        try {
            String cur;
            while ((cur = buffer.readLine()) != null) {
                sb.append(cur + "\n");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            stream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return sb.toString();
    }
}

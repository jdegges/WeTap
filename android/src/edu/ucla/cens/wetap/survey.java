package edu.ucla.cens.wetap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import android.widget.TextView;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import android.location.LocationManager;
import android.location.Location;
import android.location.Criteria;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.ImageView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.CheckBox;
import android.app.AlertDialog;
       

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import edu.ucla.cens.wetap.light_loc;
import edu.ucla.cens.wetap.survey_db;
import edu.ucla.cens.wetap.survey_db.survey_db_row;


public class survey extends Activity
{
    private String TAG = "Survey";
    private ArrayList<ArrayList<CheckBox>> group_box_list = new ArrayList<ArrayList<CheckBox>>();
    private Button take_picture;
    private Button submit_button;
    //private Button clear_history;
    private ImageView image_thumbnail;
    private String filename = "";
    private light_loc ll;
    private survey_db sdb;
    private SharedPreferences preferences;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey);

        preferences = getSharedPreferences(getString(R.string.preferences), Activity.MODE_PRIVATE);
        if (!preferences.getBoolean("authenticated", false)) {
            Log.d(TAG, "exiting (not authenticated)");
            survey.this.finish();
            return;
        }

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Log.d(TAG, "no gps was enabled, so enabling the gps now");
            alert_no_gps();
        }

        ll = new light_loc (this, lm);
        sdb = new survey_db(this);

        Log.d(TAG, "gps listener and db are started");

        // add taste boxes
        ArrayList<CheckBox> lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.taste_same) );
        lcb.add( (CheckBox) findViewById(R.id.taste_good) );
        lcb.add( (CheckBox) findViewById(R.id.taste_bad) );
        lcb.add( (CheckBox) findViewById(R.id.taste_other) );
        group_box_list.add(lcb);
        Log.d(TAG, "added taste boxes");

        // add visibility boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.visibility_visible) );
        lcb.add( (CheckBox) findViewById(R.id.visibility_hidden) );
        group_box_list.add(lcb);
        Log.d(TAG, "added visibility boxes");

        // add operable boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.operable_functioning) );
        lcb.add( (CheckBox) findViewById(R.id.operable_needs_repair) );
        lcb.add( (CheckBox) findViewById(R.id.operable_broken) );
        group_box_list.add(lcb);
        Log.d(TAG, "added operable boxes");

        // add flow boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.flow_strong) );
        lcb.add( (CheckBox) findViewById(R.id.flow_trickle) );
        lcb.add( (CheckBox) findViewById(R.id.flow_too_strong) );
        group_box_list.add(lcb);
        Log.d(TAG, "added flow boxes");

        // add style boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.style_refilling) );
        lcb.add( (CheckBox) findViewById(R.id.style_drinking) );
        lcb.add( (CheckBox) findViewById(R.id.style_both) );
        group_box_list.add(lcb);
        Log.d(TAG, "added style boxes");

        // add submit button
        submit_button = (Button) findViewById(R.id.upload_button);

        // add picture button
        take_picture = (Button) findViewById(R.id.picture_button);

        // add clear history button
        //clear_history = (Button) findViewById(R.id.clear_history_button);
        Log.d(TAG, "added buttons");

        // add image thumbnail view
        image_thumbnail = (ImageView) findViewById(R.id.thumbnail);

        // add check box listeners
        for (int j = 0; j < group_box_list.size(); j++) {
            lcb = group_box_list.get(j);
            for (int i = 0; i < lcb.size(); i++) {
                CheckBox cb = (CheckBox) lcb.get(i);
                cb.setOnClickListener(check_box_listener);
            }
        }

        // add submit button listener
        submit_button.setOnClickListener(submit_button_listener);

        // add take picture button listener
        take_picture.setOnClickListener(take_picture_listener);

        // add clear history button listener
        //clear_history.setOnClickListener(clear_history_listener);

        // restore previous state (if available)
        if (savedInstanceState != null && savedInstanceState.getBoolean("started")) {
            for (int i = 0; i < group_box_list.size(); i++) {
                lcb = group_box_list.get(i);
                int k = savedInstanceState.getInt(Integer.toString(i));

                for (int j = 0; j < lcb.size(); j++) {
                    CheckBox cb = (CheckBox) lcb.get(j);
                    if (j == k) {
                        cb.setChecked(true);
                    } else {
                        cb.setChecked(false);
                    }
                }
            }

            filename = savedInstanceState.getString("filename");
            if ((null != filename) && (filename.toString() != "")) {
                Bitmap bm = BitmapFactory.decodeFile(filename);
                if (bm != null) {
                    image_thumbnail.setImageBitmap(bm);
                }
            }
        }

        return;
    }

    @Override
    public boolean onCreateOptionsMenu (Menu m) {
        super.onCreateOptionsMenu (m);

        m.add (Menu.NONE, 0, Menu.NONE, "Map").setIcon (android.R.drawable.ic_menu_mapmode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem i) {
        switch (i.getItemId()) {
            case 0:
                survey.this.startActivity (new Intent(survey.this, map.class));
                survey.this.finish();
                return true;
            default:
                return false;
        }
    }


    private void alert_no_gps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Yout GPS seems to be disabled, You need GPS to run this application. do you want to enable it?")
               .setCancelable(false)
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        survey.this.startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 3);
                    }
                })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        survey.this.finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    protected void onPause() {
        if (null != ll) {
            ll.my_delete();
            ll = null;
        }
        super.onPause();
    }
    protected void onResume() {
        super.onResume();
        if (null == ll) {
            ll = new light_loc(this, (LocationManager) getSystemService(Context.LOCATION_SERVICE));
        }
    }

    protected void onStop() {
        if (null != ll) {
            ll.my_delete();
            ll = null;
        }
        super.onStop();
    }
    protected void onStart() {
        super.onStart();
        if (null == ll) {
            ll = new light_loc(this, (LocationManager) getSystemService(Context.LOCATION_SERVICE));
        }
    }

    protected void onDestroy() {
        if (null != ll) {
            ll.my_delete();
            ll = null;
        }
        super.onDestroy();
    }

    // if this activity gets killed for any reason, save the status of the
    // check boxes so that they are filled in the next time it gets run
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("started", true);
        savedInstanceState.putString("filename", filename);
        List<CheckBox> lcb;
        CheckBox cb;

        for (int i = 0; i < group_box_list.size(); i++) {
            lcb = group_box_list.get(i);
            for (int j = 0; j < lcb.size(); j++) {
                cb = (CheckBox) lcb.get(j);
                if (cb.isChecked()) {
                    savedInstanceState.putInt(Integer.toString(i), j);
                }
            }
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    OnClickListener check_box_listener = new OnClickListener() {
        public void onClick(View v) {
            List<CheckBox> lcb;
            CheckBox cb = (CheckBox) v;
            boolean checked = cb.isChecked();

            for (int i = 0; i < group_box_list.size(); i++) {
                lcb = group_box_list.get(i);
                int index = lcb.indexOf(cb);
                
                if(-1 != index) {
                    for (i = 0; i < lcb.size(); i++) {
                        cb = (CheckBox) lcb.get(i);
                        if (i != index &&
                            cb.isChecked()) {
                            cb.setChecked(false);
                        }
                    }
                    return;
                }
            }
        }
    };

    OnClickListener submit_button_listener = new OnClickListener() {
        public void onClick(View v) {
            Date d = new Date();

            String q_taste = "0";
            String q_visibility = "0";
            String q_operable = "0";
            String q_flow = "0";
            String q_style = "0";

            List<CheckBox> lcb = group_box_list.get(0);
            for (int i = 0; i < lcb.size(); i++) {
                CheckBox cb = (CheckBox) lcb.get(i);
                if (cb.isChecked()) {
                    q_taste = Integer.toString(i);
                    break;
                }
            }

            lcb = group_box_list.get(1);
            for (int i = 0; i < lcb.size(); i++) {
                CheckBox cb = (CheckBox) lcb.get(i);
                if (cb.isChecked()) {
                    q_visibility = Integer.toString(i);
                    break;
                }
            }

            lcb = group_box_list.get(2);
            for (int i = 0; i < lcb.size(); i++) {
                CheckBox cb = (CheckBox) lcb.get(i);
                if (cb.isChecked()) {
                    q_operable = Integer.toString(i);              
                    break;
                }
            }

            lcb = group_box_list.get(3);
            for (int i = 0; i < lcb.size(); i++) {
                CheckBox cb = (CheckBox) lcb.get(i);
                if (cb.isChecked()) {
                    q_flow = Integer.toString(i);              
                    break;
                }
            }

            lcb = group_box_list.get(4);
            for (int i = 0; i < lcb.size(); i++) {
                CheckBox cb = (CheckBox) lcb.get(i);
                if (cb.isChecked()) {
                    q_style = Integer.toString(i);              
                    break;
                }
            }

            String longitude = "";
            String latitude = "";
            String time = Long.toString(d.getTime());
            String photo_filename = filename;

            sdb.open();
            long row_id = sdb.createEntry(q_taste, q_visibility, q_operable,
                                          q_flow, q_style, longitude, latitude,
                                          time, getString(R.string.version), photo_filename);
            sdb.close();

            sdb.open();
            survey_db_row sr = sdb.fetchEntry(row_id);
            sdb.close();

            Log.d("SUBMIT SURVEY", Long.toString(sr.row_id) + ", " +
                                   sr.q_taste + ", " +
                                   sr.q_visibility + ", " +
                                   sr.q_operable + ", " +
                                   sr.q_flow + ", " +
                                   sr.q_style + ", " +
                                   sr.longitude + ", " +
                                   sr.latitude + ", " +
                                   sr.time + ", " +
                                   sr.version + ", " +
                                   sr.photo_filename + ".");

            // restart this view
            Toast.makeText(survey.this, "Survey successfully submitted!", Toast.LENGTH_LONG).show();
            survey.this.startActivity (new Intent(survey.this, survey.class));
            survey.this.finish();
        }
    };

    OnClickListener take_picture_listener = new OnClickListener() {
        public void onClick(View v) {
            Intent photo_intent = new Intent(survey.this, photo.class);
            startActivityForResult(photo_intent, 0);
        }
    };

    OnClickListener clear_history_listener = new OnClickListener() {
        public void onClick(View v) {
            sdb.open();
            ArrayList<survey_db_row> sr_list = sdb.fetchAllEntries();
            sdb.close();

            for (int i = 0; i < sr_list.size(); i++) {
                survey_db_row sr = sr_list.get(i);
                File file = null;
                if ((sr.photo_filename != null) && (sr.photo_filename.toString() != "")) {
                    file = new File(sr.photo_filename.toString());
                }
                if(file != null) {
                    file.delete();
                }
                sdb.open();
                sdb.deleteEntry(sr.row_id);
                sdb.close();
            }

/*
            sdb.open();
            sdb.refresh_db();
            sdb.close();
            */
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_CANCELED != resultCode) {
            filename = data.getAction().toString();
            if ((null != filename) && (filename.toString() != "")) {
                Bitmap bm = BitmapFactory.decodeFile(filename);
                if (bm != null) {
                    image_thumbnail.setImageBitmap(bm);
                }
            }
        }
    }
}

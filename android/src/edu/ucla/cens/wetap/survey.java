package edu.ucla.cens.wetap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import android.widget.TextView;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.ImageView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.TableRow;
import android.app.AlertDialog;
       

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

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
    private survey_db sdb;
    private SharedPreferences preferences;
    private final int GB_INDEX_OPER = 0;
    private final int GB_INDEX_TASTE = 1;
    private final int GB_INDEX_FLOW = 2;
    private final int GB_INDEX_WHEEL = 3;
    private final int GB_INDEX_CHILD = 4;
    private final int GB_INDEX_REFILL = 5;
    private final int GB_INDEX_REFILL_AUX = 6;
    private final int GB_INDEX_VIS = 7;
    private final int GB_INDEX_LOC = 8;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey);

        preferences = getSharedPreferences(getString(R.string.preferences), Activity.MODE_PRIVATE);
        // allow users to collect data even if they are not yet authenticated
        // let the survey_upload service make sure they are auth'd before
        // uploading... (lets users collect data without internet conn)
        //if (!preferences.getBoolean("authenticated", false)) {
        //    Log.d(TAG, "exiting (not authenticated)");
        //    survey.this.finish();
        //    return;
        //}

        sdb = new survey_db(this);

        /* start location service */
        startService (new Intent(survey.this, light_loc.class));
        preferences.edit().putBoolean ("light_loc", true).commit ();

        Log.d(TAG, "gps listener and db are started");

        ArrayList<CheckBox> lcb;

        // add operable boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.operable_functioning) );
        lcb.add( (CheckBox) findViewById(R.id.operable_broken) );
        lcb.add( (CheckBox) findViewById(R.id.operable_needs_repair) );
        group_box_list.add(lcb);
        Log.d(TAG, "added operable boxes");

        // add taste boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.taste_same) );
        lcb.add( (CheckBox) findViewById(R.id.taste_good) );
        lcb.add( (CheckBox) findViewById(R.id.taste_bad) );
        lcb.add( (CheckBox) findViewById(R.id.taste_other) );
        group_box_list.add(lcb);
        Log.d(TAG, "added taste boxes");

        // add flow boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.flow_strong) );
        lcb.add( (CheckBox) findViewById(R.id.flow_trickle) );
        lcb.add( (CheckBox) findViewById(R.id.flow_too_strong) );
        lcb.add( (CheckBox) findViewById(R.id.flow_cant_answer) );
        group_box_list.add(lcb);
        Log.d(TAG, "added flow boxes");

        // add access wheelchair box:
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.question_5_option_0) );
        group_box_list.add(lcb);
        Log.d(TAG, "added wheelchair box");

        // add access child box:
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.question_5_option_1) );
        group_box_list.add(lcb);
        Log.d(TAG, "added child box");

        // add access refill box:
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.question_5_option_2) );
        group_box_list.add(lcb);
        Log.d(TAG, "added refill box");

        // add alternate accessibility questions
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.question_6_option_0) );
        lcb.add( (CheckBox) findViewById(R.id.question_6_option_1) );
        lcb.add( (CheckBox) findViewById(R.id.question_6_option_2) );
        group_box_list.add(lcb);
        Log.d(TAG, "added alternate accessibility boxes");

        // add visibility boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.visibility_visible) );
        lcb.add( (CheckBox) findViewById(R.id.visibility_hidden) );
        group_box_list.add(lcb);
        Log.d(TAG, "added visibility boxes");

        // add location boxes
        lcb = new ArrayList<CheckBox>();
        lcb.add( (CheckBox) findViewById(R.id.location_indoor) );
        lcb.add( (CheckBox) findViewById(R.id.location_outdoors) );
        group_box_list.add(lcb);
        Log.d(TAG, "added location boxes");

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
                        update_checkbox_status (cb);
                        break;
//                    } else {
//                        cb.setChecked(false);
                    }
                }
            }

            filename = savedInstanceState.getString("filename");
            if ((null != filename) && (!filename.toString().equals(""))) {
                Bitmap bm = BitmapFactory.decodeFile(filename);
                if (bm != null) {
                    take_picture.setText("Retake Picture");
                    image_thumbnail.setVisibility(View.VISIBLE);
                    image_thumbnail.setImageBitmap(bm);
                }
            }
        }

        return;
    }

    @Override
    public boolean onCreateOptionsMenu (Menu m) {
        super.onCreateOptionsMenu (m);

        m.add (Menu.NONE, 0, Menu.NONE, "Home").setIcon (android.R.drawable.ic_menu_revert);
        m.add (Menu.NONE, 1, Menu.NONE, "Map").setIcon (android.R.drawable.ic_menu_mapmode);
        m.add (Menu.NONE, 2, Menu.NONE, "About").setIcon (android.R.drawable.ic_menu_info_details);
        m.add (Menu.NONE, 3, Menu.NONE, "Instructions").setIcon (android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem index) {
        Context ctx = survey.this;
        Intent i;
        switch (index.getItemId()) {
            case 0:
                i = new Intent (ctx, home.class);
                break;
            case 1:
                i = new Intent (ctx, map.class);
                break;
            case 2:
                i = new Intent (ctx, about.class);
                break;
            case 3:
                i = new Intent (ctx, instructions.class);
                break;
            default:
                return false;
        }
        ctx.startActivity (i);
        this.finish();
        return true;
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

    public void update_checkbox_status (CheckBox cb) {
        List<CheckBox> lcb;
        boolean checked = cb.isChecked();

        if (R.id.question_5_option_2 == cb.getId()) {
            TableRow tr = (TableRow) findViewById(R.id.question_6_row);
            tr.setVisibility(checked ? View.GONE : View.VISIBLE);
            return;
        }

        if (R.id.operable_broken == cb.getId()) {
            View v = findViewById(R.id.taste_row);
            v.setVisibility(checked ? View.GONE : View.VISIBLE);

            v = (TableRow) findViewById(R.id.flow_row);
            v.setVisibility(checked ? View.GONE : View.VISIBLE);

            CheckBox ncb = (CheckBox) findViewById(R.id.question_5_option_2);
            ncb.setVisibility(checked ? View.GONE : View.VISIBLE);

            if (false == ncb.isChecked()) {
                v = findViewById(R.id.question_6_row);
                v.setVisibility(checked ? View.GONE : View.VISIBLE);
            }
        }

        // dont do anything if this box was unchecked
        if (false == checked) {
            return;
        }

        for (int i = 0; i < group_box_list.size(); i++) {
            lcb = group_box_list.get(i);
            int index = lcb.indexOf(cb);

            // continue on if the check box wasn't found in this checkbox group
            if(-1 == index) {
                continue;
            }

            // switch all of the other checkboxes in this group off
            for (i = 0; i < lcb.size(); i++) {
                cb = (CheckBox) lcb.get(i);
                if (i != index
                        && cb.isChecked())
                {
                    cb.setChecked(false);
                    checked = false;
                    if (R.id.operable_broken == cb.getId()) {
                        View v = findViewById(R.id.taste_row);
                        v.setVisibility(checked ? View.GONE : View.VISIBLE);

                        v = findViewById(R.id.flow_row);
                        v.setVisibility(checked ? View.GONE : View.VISIBLE);

                        v = findViewById(R.id.question_5_row);
                        v.setVisibility(checked ? View.GONE : View.VISIBLE);

                        CheckBox ncb = (CheckBox) findViewById(R.id.question_5_option_2);
                        ncb.setVisibility(checked ? View.GONE : View.VISIBLE);

                        if (false == ncb.isChecked()) {
                            v = findViewById(R.id.question_6_row);
                            v.setVisibility(checked ? View.GONE : View.VISIBLE);
                        }
                    }
                }
            }
            return;
        }
    }

    OnClickListener check_box_listener = new OnClickListener() {
        public void onClick(View v) {
            update_checkbox_status ((CheckBox) v);
        }
    };

    OnClickListener submit_button_listener = new OnClickListener() {
        private String get_group_result (int index) {
            List<CheckBox> lcb = group_box_list.get(index);
            for (int i = 0; i < lcb.size(); i++) {
                CheckBox cb = (CheckBox) lcb.get(i);
                if (cb.isChecked()) {
                    return Integer.toString(i+1);
                }
            }
            return "0";
        }

        public void onClick(View v) {
            Date d = new Date();

            String q_location = "0";
            String q_visibility = "0";
            String q_operable = "0";
            String q_wheel = "0";
            String q_child = "0";
            String q_refill = "0";
            String q_refill_aux = "0";
            String q_taste = "0";
            String q_flow = "0";

            q_location = get_group_result (GB_INDEX_LOC);
            q_visibility = get_group_result (GB_INDEX_VIS);
            q_operable = get_group_result (GB_INDEX_OPER);
            q_wheel = get_group_result (GB_INDEX_WHEEL);
            q_child = get_group_result (GB_INDEX_CHILD);
            q_refill = get_group_result (GB_INDEX_REFILL);
            q_refill_aux = get_group_result (GB_INDEX_REFILL_AUX);
            q_taste = get_group_result (GB_INDEX_TASTE);
            q_flow = get_group_result (GB_INDEX_FLOW);

            /* make sure they dont submit an incomplete survey */
            if (q_location.equals("0")
                || q_visibility.equals("0")
                || q_operable.equals("0"))
            {
                Toast
                .makeText (survey.this,
                           "You have not answered one or more questions. Please fill them all out.",
                           Toast.LENGTH_LONG)
                .show();
                return;
            }

            if (!q_operable.equals("2")
                && !q_refill.equals("1")
                && q_refill_aux.equals("0"))
            {
                Toast
                .makeText (survey.this,
                           "You have not marked why you couldn't refill from this fountain.",
                           Toast.LENGTH_LONG)
                .show();
                return;
            }

            if (!q_operable.equals("2")
                && (q_taste.equals("0")
                    || q_flow.equals("0")))
            {
                Toast
                .makeText (survey.this,
                           "You must fill out both the taste and flow questions.",
                           Toast.LENGTH_LONG)
                .show();
                return;
            }

            /* if the fountain was broken then throw out anything that couldn't
             * have been answered */
            if (q_operable.equals("2")) {
                q_refill =
                q_refill_aux =
                q_taste = 
                q_flow = "0";
            }

            /* if they could refill a bottle at the fountain then throw out
             * refill aux questions */
            if (q_refill.equals("1")) {
                q_refill_aux = "0";
            }

            String longitude = "";
            String latitude = "";
            String time = Long.toString(d.getTime());
            String photo_filename = filename;

            sdb.open();
            long row_id = sdb.createEntry(q_location, q_visibility, q_operable,
                q_wheel, q_child, q_refill, q_refill_aux, q_taste, q_flow,
                longitude, latitude, time, getString(R.string.version),
                photo_filename);
            sdb.close();

            sdb.open();
            survey_db_row sr = sdb.fetchEntry(row_id);
            sdb.close();

            Log.d("SUBMIT SURVEY", Long.toString(sr.row_id) + ", " +
                                   sr.q_taste + ", " +
                                   sr.q_visibility + ", " +
                                   sr.q_operable + ", " +
                                   sr.q_flow + ", " +
                                   sr.q_location + ", " +
                                   sr.longitude + ", " +
                                   sr.latitude + ", " +
                                   sr.time + ", " +
                                   sr.version + ", " +
                                   sr.photo_filename + ".");

            /* start location service */
            if (!preferences.getBoolean("light_loc", false)) {
                startService (new Intent(survey.this, light_loc.class));
                preferences.edit().putBoolean ("light_loc", true).commit ();
            }

            // popup success toast and return to home page
            Toast.makeText(survey.this, "Survey successfully submitted!", Toast.LENGTH_LONG).show();
            survey.this.startActivity (new Intent(survey.this, home.class));
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
                if ((sr.photo_filename != null) && (!sr.photo_filename.toString().equals(""))) {
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
            if ((null != filename) && (!filename.toString().equals(""))) {
                Bitmap bm = BitmapFactory.decodeFile(filename);
                if (bm != null) {
                    take_picture.setText("Retake Picture");
                    image_thumbnail.setVisibility(View.VISIBLE);
                    image_thumbnail.setImageBitmap(bm);
                }
            }
        }
    }
}

package edu.ucla.cens.wetap;

import java.util.List;
import java.util.ArrayList;

import java.io.OutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;

import android.os.Bundle;

import android.content.SharedPreferences;

import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.util.Log;




public class popup extends Activity {
    String TAG = "POPUP";
    @Override
    public void onCreate (Bundle b) {
        super.onCreate (b);
        setContentView (R.layout.popup);

        SharedPreferences perf = this.getSharedPreferences(getString(R.string.preferences), Activity.MODE_PRIVATE);
        String req_key = perf.getString("site_key", "");

        String flow = "Not applicable";
        String photo = ""; 
        String operable = "Not rated";
        String version = "Not rated";
        String wheel = "No";
        String child = "No";
        String refill = "No";
        String refill_aux = "Not applicable";
        String visibility = "Not rated";
        String taste = "Not applicable";
        String location = "Not specified";

        if (req_key != null && req_key != "") {
            String site_url = "http://we-tap.appspot.com/get_a_point?key=" + req_key;
            String site_data = getUrlData (site_url);

            Log.d(TAG, "THE SITE URL: " + site_url);

            JSONObject entry;
            try { entry = new JSONObject (site_data.toString()); }
            catch (JSONException e) { e.printStackTrace(); return; }

            try { flow = (String) entry.get("q_flow"); }
            catch (JSONException e) { flow = null; }
            catch (ClassCastException e) { flow = null; }
            try { photo = (String) entry.get("photo"); }
            catch (JSONException e) { photo = null; }
            catch (ClassCastException e) { photo = null; }
            try { operable = (String) entry.get("q_operable"); }
            catch (JSONException e) { operable = null; }
            catch (ClassCastException e) { operable = null; }
            try { version = (String) entry.get("version"); }
            catch (JSONException e) { version = null; }
            catch (ClassCastException e) { version = null; }
            try { wheel = (String) entry.get("q_wheel"); }
            catch (JSONException e) { wheel = null; }
            catch (ClassCastException e) { wheel = null; }
            try { child = (String) entry.get("q_child"); }
            catch (JSONException e) { child = null; }
            catch (ClassCastException e) { child = null; }
            try { refill = (String) entry.get("q_refill"); }
            catch (JSONException e) { refill = null; }
            catch (ClassCastException e) { refill = null; }
            try { refill_aux = (String) entry.get("q_refill_aux"); }
            catch (JSONException e) { refill_aux = null; }
            catch (ClassCastException e) { refill_aux = null; }
            try { visibility = (String) entry.get("q_visibility"); }
            catch (JSONException e) { visibility = null; }
            catch (ClassCastException e) { visibility = null; }
            try { taste = (String) entry.get("q_taste"); }
            catch (JSONException e) { taste = null; }
            catch (ClassCastException e) { taste = null; }
            try { location = (String) entry.get("q_location"); }
            catch (JSONException e) { location = null; }
            catch (ClassCastException e) { location = null; }
        }

        Log.d("POPUP", "about to set values from appspot");

        WebView wv = (WebView) findViewById (R.id.image);
        wv.getSettings().setJavaScriptEnabled(false);
        wv.loadUrl(photo);
        Log.d("POPUP", "loaded url: " + photo);

        TextView tv = (TextView) findViewById (R.id.taste_score);
        tv.setText (decode_survey("taste", taste));

        tv = (TextView) findViewById (R.id.visibility_score);
        tv.setText (decode_survey("visibility", visibility));

        tv = (TextView) findViewById (R.id.operable_score);
        tv.setText (decode_survey("operable", operable));

        tv = (TextView) findViewById (R.id.flow_score);
        tv.setText (decode_survey("flow", flow));
        
        tv = (TextView) findViewById (R.id.wheel_score);
        tv.setText (decode_survey("binary", wheel));

        tv = (TextView) findViewById (R.id.child_score);
        tv.setText (decode_survey("binary", child));

        tv = (TextView) findViewById (R.id.refill_score);
        tv.setText (decode_survey("binary", refill));

        tv = (TextView) findViewById (R.id.refill_aux_score);
        tv.setText (decode_survey("refill_aux", refill_aux));

        tv = (TextView) findViewById (R.id.location_score);
        tv.setText (decode_survey("location", location));
    }

    private String decode_survey (String q, String v) {
        int k;

        if (q == null || v == null) {
            return "Not rated";
        }

        k = Integer.valueOf(v);

        if (q.equals("taste")) {
            switch (k) {
                case 0: return "Not applicable";
                case 1: return "Same as home tap";
                case 2: return "Better than home tap";
                case 3: return "Worse than home tap";
                case 4: return "Can't answer";
            }
        } else if (q.equals("visibility")) {
            switch (k) {
                case 1: return "Visible";
                case 2: return "Hidden";
            }
        } else if (q.equals("operable")) {
            switch (k) {
                case 1: return "Working";
                case 2: return "Broken";
                case 3: return "Needs repair";
            }
        } else if (q.equals("flow")) {
            switch (k) {
                case 0: return "Not applicable";
                case 1: return "Strong";
                case 2: return "Trickle";
                case 3: return "Too strong";
            }
        } else if (q.equals("binary")) {
            switch (k) {
                case 0: return "No";
                case 1: return "Yes";
            }
        } else if (q.equals("refill_aux")) {
            switch (k) {
                case 0: return "Not applicable";
                case 1: return "No room";
                case 2: return "Not enough water flow.";
                case 3: return "Other";
            }
        } else if (q.equals("location")) {
            switch (k) {
                case 1: return "Indoor";
                case 2: return "Outdoors";
            }
        }
        return "Not rated";
    }


    private String getUrlData(String url) {
        String websiteData = null;
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            URI uri = new URI(url);
            HttpGet method = new HttpGet(uri);
            HttpResponse res = client.execute(method);
            InputStream data = res.getEntity().getContent();
            websiteData = generateString(data);
        } catch (ClientProtocolException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }
        catch (URISyntaxException e) { e.printStackTrace(); }
        return websiteData;
    }

    private String generateString(InputStream stream) {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();

        try {
            String cur;
            while ((cur = buffer.readLine()) != null) {
                sb.append(cur + "\n");
            }
        } catch (IOException e) { e.printStackTrace(); }

        try { stream.close(); } catch (IOException e) { e.printStackTrace(); }
        return sb.toString();
    }

}

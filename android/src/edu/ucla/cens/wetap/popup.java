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

        String flow = "Not rated";
        String photo = ""; 
        String operable = "Not rated";
        String version = "Not rated";
        String style = "Not rated";
        String visibility = "Not rated";
        String taste = "Not rated";

        if (req_key != null && req_key != "") {
            String site_url = "http://we-tap.appspot.com/get_a_point?key=" + req_key;
            String site_data = getUrlData (site_url);

            try {
                JSONObject entry = new JSONObject (site_data.toString());
                flow = (String) entry.get("q_flow");
                photo = (String) entry.get("photo");
                operable = (String) entry.get("q_operable");
                version = (String) entry.get("version");
                style = (String) entry.get("q_style");
                visibility = (String) entry.get("q_visibility");
                taste = (String) entry.get("q_taste");
            } catch (JSONException e) { e.printStackTrace(); }
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
        
        tv = (TextView) findViewById (R.id.style_score);
        tv.setText (decode_survey("style", style));
    }

    private String decode_survey (String q, String v) {
        int k = Integer.valueOf(v);

        if (q.equals("taste")) {
            switch (k) {
                case 0: return "Same as home tap";
                case 1: return "Better";
                case 2: return "Worse";
                case 3: return "Can't answer";
            }
        } else if (q.equals("visibility")) {
            switch (k) {
                case 0: return "Visible";
                case 1: return "Hidden";
            }
        } else if (q.equals("operable")) {
            switch (k) {
                case 0: return "Working";
                case 1: return "Broken";
                case 2: return "Needs repair";
            }
        } else if (q.equals("flow")) {
            switch (k) {
                case 0: return "Strong";
                case 1: return "Trickle";
                case 2: return "Too strong";
            }
        } else if (q.equals("style")) {
            switch (k) {
                case 0: return "Refilling";
                case 1: return "Drinking";
                case 2: return "Both";
            }
        }
        return "";
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

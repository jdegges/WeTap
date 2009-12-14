package edu.ucla.cens.wetap;

import java.util.List;
import java.util.ArrayList;
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

import org.xmlpull.v1.XmlPullParser;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Overlay;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.GeoPoint;

import android.os.Bundle;
import android.os.Vibrator;
import android.os.Looper;

import android.util.Log;
import android.util.AttributeSet;
import android.util.Xml;

import android.widget.LinearLayout;
import android.widget.ZoomControls;
import android.widget.Toast;

import android.graphics.drawable.Drawable;

import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.content.res.Resources;
import android.content.Intent;
import android.content.SharedPreferences;

import android.app.Activity;

public class map extends MapActivity {
    private SharedPreferences preferences;
    private LinearLayout linear_layout;
    private MapView map_view;
    private ZoomControls zoom_controls;
    List<Overlay> overlay_list;
    Drawable marker;
    MySiteOverlay site_overlay;
    MyLocationOverlay location_overlay;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.map);

        preferences = this.getSharedPreferences(getString(R.string.preferences), Activity.MODE_PRIVATE);

        linear_layout = (LinearLayout) findViewById (R.id.zoomview);
        map_view = (MapView) findViewById (R.id.mapview);
        overlay_list = map_view.getOverlays ();
        zoom_controls = (ZoomControls) map_view.getZoomControls();
        marker = this.getResources().getDrawable (R.drawable.androidmarker);

        // add zoom controls to the map view
        linear_layout.addView (zoom_controls);

        // set zoom level to 16 (pretty good default zoom level)
        map_view.getController().setZoom(16);

        // focus the map on the current GPS location
        location_overlay = new MyLocationOverlay (this, map_view);
        location_overlay.enableMyLocation();
        location_overlay.runOnFirstFix(new Runnable() {
                public void run () {
                    map_view.getController().animateTo(location_overlay.getMyLocation());
                }
            }
        );
        overlay_list.add (location_overlay);


        // create an overlay and populate it with sites from the
        // appengine database
        site_overlay = new MySiteOverlay (marker);
        overlay_list.add (site_overlay);
    }

    protected void onPause() {
        if (null != location_overlay) {
            location_overlay.disableMyLocation();
        }
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        if (null != location_overlay) {
            location_overlay.enableMyLocation();
        }
    }

    protected void onStop() {
        if (null != location_overlay) {
            location_overlay.disableMyLocation();
        }
        super.onStop();
    }

    protected void onStart() {
        super.onStart();
        if (null != location_overlay) {
            location_overlay.enableMyLocation();
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu (Menu m) {
        super.onCreateOptionsMenu (m);

        m.add (Menu.NONE, 0, Menu.NONE, "Survey").setIcon (android.R.drawable.ic_menu_agenda);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem i) {
        switch (i.getItemId()) {
            case 0:
                map.this.startActivity (new Intent(map.this, survey.class));
                map.this.finish();
                return true;
            default:
                return false;
        }
    }

    public class MySiteOverlay<Item> extends ItemizedOverlay {
        private ArrayList<OverlayItem> overlay_items = new ArrayList<OverlayItem>();
        private int last_tap_index = -1;
        private float down_x = -1;
        private float down_y = -1;
        private long down_t = -1;
        private Thread vibrator;
        private final int long_press_delay = 1000; // milliseconds
        private final int max_dx = 15;
        private final int max_dy = 15;
    
        MySiteOverlay(Drawable defaultMarker) {
            super (boundCenterBottom(defaultMarker));

            String point_url = "http://we-tap.appspot.com/get_point_summary";
            String point_data = getUrlData (point_url);

            try {
                JSONObject json = new JSONObject (point_data.toString());
                for (int i = 0;; i++) {
                    JSONObject entry = json.getJSONObject(Integer.toString(i));
                    String text = "Taste: " + decode_survey ("taste", (String)entry.get("q_taste"));
                    overlay_items.add (new OverlayItem(get_point(Float.valueOf((String)entry.get("latitude")),
                                                       Float.valueOf((String)entry.get("longitude"))),
                                             text,
                                             (String)entry.get("key")));
                }
            } catch (JSONException e) {}

            populate();
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
            } else if (q.equals("location")) {
                switch (k) {
                    case 0: return "Indoor";
                    case 1: return "Outdoors";
                }
            }
            return "";
        }

        private GeoPoint get_point (double lat, double lon) {
            return new GeoPoint ((int)(lat*1000000.0), (int)(lon*1000000.0));
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
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
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


    
        public void addOverlay (OverlayItem overlay) {
            overlay_items.add (overlay);
            populate();
        }
    
        @Override
        protected OverlayItem createItem (int i) {
            return overlay_items.get(i);
        }
    
        @Override
        public int size () {
            return overlay_items.size();
        }

        @Override
        protected boolean onTap (int index) {
            Toast.makeText (map.this, overlay_items.get(index).getTitle(),
                            Toast.LENGTH_SHORT).show();
            last_tap_index = index;
            return true;
        }

        @Override
        public boolean onTouchEvent (MotionEvent event, MapView map_view) {
            float dx;
            float dy;
            long dt;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (-1 != last_tap_index) {
                        down_x = Math.abs(event.getX());
                        down_y = Math.abs(event.getY());
                        down_t = Math.abs(event.getEventTime());

                        vibrator = new Thread (new Runnable() {
                                public void run () {
                                    // wait for the user to press down for 'long_press_delay'
                                    // seconds, if the user hasnt let go by then then the
                                    // rest of this function will execute
                                    for (int i = 0; i < long_press_delay; i+= long_press_delay/10) {
                                        try { Thread.sleep(long_press_delay/10); }
                                        catch (InterruptedException e) {}

                                        if (-1 == down_x || -1 == down_y || -1 == down_t) {
                                            return;
                                        }
                                    }

                                    // vibrate the phone for a few ms
                                    ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(50);

                                    // record which point the user clicked on
                                    preferences.edit().putString("site_key", overlay_items.get(last_tap_index).getSnippet()).commit();

                                    // start up the popup activity to display info on that site
                                    map.this.startActivity (new Intent(map.this, popup.class));
                                    return;
                                }
                            }
                        );

                        vibrator.start();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    down_x = down_y = -1;
                    down_t = -1;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (-1 == down_x || -1 == down_y || -1 == down_t) {
                        return false;
                    }

                    dx = Math.abs(down_x - Math.abs(event.getX()));
                    dy = Math.abs(down_y - Math.abs(event.getY()));

                    if (dx >= max_dx || dy >= max_dy) {
                        down_x = down_y = -1;
                        down_t = -1;
                    }

                    break;
                default:
                    break;
            }
            return false;
        }
    }

}

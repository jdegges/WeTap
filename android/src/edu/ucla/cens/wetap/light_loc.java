package edu.ucla.cens.wetap;

import android.os.Bundle;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.app.Service;
import android.app.Activity;


import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import edu.ucla.cens.wetap.survey_db;
import edu.ucla.cens.wetap.survey_db.survey_db_row;

public class light_loc extends Service {
    private LocationManager lm;
    private LocationListener ll;
    private survey_db sdb;
    private SharedPreferences pref;
    private Context ctx;
    private static final String TAG = "LIGHT_LOC LOCATION SERVICE";

    @Override
    public void onCreate () {
        super.onCreate ();

        ctx = light_loc.this;
        pref = getSharedPreferences (getString (R.string.preferences), Activity.MODE_PRIVATE);

        lm = (LocationManager) getSystemService (Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled (LocationManager.GPS_PROVIDER)) {
            Log.d (TAG, "stopping service... no GPS provider");
            return;
        }

        ll = new location_listener ();
        lm.requestLocationUpdates (LocationManager.GPS_PROVIDER, 10000, 0, ll);
        sdb = new survey_db (ctx);
        Log.d (TAG, "service started. added location update listener");
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }

    @Override
    public void onDestroy () {
        if (null != lm && null != ll) {
            lm.removeUpdates (ll);
        }
        ll = null;
        lm = null;
        super.onDestroy ();
    }

    private class location_listener implements LocationListener {
        public void onLocationChanged (Location loc) {
            if (null != loc) {
                Log.d (TAG, "THE LOCATION HAS JUST CHANGED TO: " + loc.toString ());
                String lat = Double.toString (loc.getLatitude ());
                String lon = Double.toString (loc.getLongitude ());
                int ret;

                sdb.open();
                ret = sdb.update_gpsless_entries (lon, lat);
                sdb.close();

                if (0 != ret) {
                    pref.edit ().putBoolean ("light_loc", false).commit ();
                    Log.d (TAG, "stopping location listener service: light_loc");
                    stopSelf ();
                }
            }
        }

        public void onProviderDisabled (String arg) {}
        public void onProviderEnabled (String provider) {}
        public void onStatusChanged (String provider, int status, Bundle extras) {}
    }
}

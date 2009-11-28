package edu.ucla.cens.wetap;

import android.os.Bundle;
import android.content.Context;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import edu.ucla.cens.wetap.survey_db;
import edu.ucla.cens.wetap.survey_db.survey_db_row;

public class light_loc {
    private LocationManager lm;
    private LocationListener ll;
    private survey_db sdb;

    light_loc(Context ctx, LocationManager locm) {
        lm = locm;
        ll = new location_listener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, ll);
        sdb = new survey_db (ctx);
    }

    public void my_delete() {
        lm.removeUpdates(ll);
        ll = null;
        lm = null;
    }

    private class location_listener implements LocationListener {
        public void onLocationChanged(Location loc) {
            if (null != loc) {
                Log.d("ON LOCATION CHANGED", "THE LOCATION HAS JUST CHANGED TO: " + loc.toString());
                String lat = Double.toString(loc.getLatitude());
                String lon = Double.toString(loc.getLongitude());

                sdb.open();
                sdb.update_gpsless_entries (lon, lat);
                sdb.close();
            }
        }

        public void onProviderDisabled(String arg) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}

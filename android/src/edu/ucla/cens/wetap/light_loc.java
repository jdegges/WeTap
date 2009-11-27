package edu.ucla.cens.wetap;

import android.os.Bundle;
import android.content.Context;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

public class light_loc {
    private LocationManager lm;
    private LocationListener ll;
    private Location last_loc = null;

    light_loc(LocationManager locm) {
        lm = locm;
        ll = new location_listener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, ll);
        last_loc = null;
    }

    public void my_delete() {
        Log.d("REMOVING UPDATES", "@@@@@@@@@@@@@@@@@@@@@@@2@@@@@@@@@@@@@@@@@@@@@@@2@@@@@@@@@@@@@@@@@@@@@@@2@@@@@@@@@@@@@@@@@@@@@@@2@@@@@@@@@@@@@@@@@@@@@@@2@@@@@@@@@@@@@@@@@@@@@@@2");
        lm.removeUpdates(ll);
        ll = null;
        lm = null;
    }

    public Location get_location () {
        while (null == last_loc) {
            try{ Thread.sleep(500); }
            catch (InterruptedException e) {}
        }
        return last_loc;
    }

    private class location_listener implements LocationListener {
        public void onLocationChanged(Location loc) {
            if (null != loc) {
                last_loc = loc;
                Log.d("ON LOCATION CHANGED", "THE LOCATION HAS JUST CHANGED TO: " + loc.toString());
            }
        }

        public void onProviderDisabled(String arg) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}

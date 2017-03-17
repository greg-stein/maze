package com.example.neutrino.maze;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import static android.location.GpsStatus.GPS_EVENT_SATELLITE_STATUS;

/**
 * Created by Greg Stein on 3/16/2017.
 */

public class GpsScanner implements GpsStatus.Listener{
    public static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 1;
    private final LocationManager mLocationManager;
    private GpsStatus mGpsStatus = null;

    public GpsScanner(LocationManager locationManager) {
        mLocationManager = locationManager;
        if (ActivityCompat.checkSelfPermission(AppSettings.appActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COARSE_LOCATION);
        }
        mLocationManager.addGpsStatusListener(this);
    }

    @Override
    public void onGpsStatusChanged(int status) {
        if (status == GPS_EVENT_SATELLITE_STATUS) {
            if ( Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(AppSettings.appActivity, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(AppSettings.appActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return  ;
            }
            mGpsStatus = mLocationManager.getGpsStatus(mGpsStatus);
            Iterable<GpsSatellite> satellites = mGpsStatus.getSatellites();
            for(GpsSatellite satellite : satellites) {
                final float azimuth = satellite.getAzimuth();
                final float elevation = satellite.getElevation();
                final float snr = satellite.getSnr();
            }
        }
    }
}

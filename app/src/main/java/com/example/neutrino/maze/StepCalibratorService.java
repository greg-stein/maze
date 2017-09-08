package com.example.neutrino.maze;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.neutrino.maze.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Greg Stein on 8/30/2017.
 */
public class StepCalibratorService extends Service implements LocationListener, SensorListener.IStepDetectedListener {
    private static final int GPS_UPDATE_INTERVAL = 1000; // 1 sec
    public static final int MIN_DISTANCE = 5;
    private static float calibratorWalkedDistance;
    private static int calibratorStepsDetected;
    private static boolean calibrationCompleted;
    private static float calibratorUserStepLength;
    private static boolean isCalibrationActive;
    private long mLastLocationMillis;
    private Location mLastLocation;
    private boolean isGPSFix;

    private boolean mLocationPermissionsGranted;
    private SensorListener mSensorListener;

    public static void loadFromConfig(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        calibratorWalkedDistance = settings.getFloat("calibratorWalkedDistance", 0f);
        calibratorStepsDetected = settings.getInt("calibratorStepsDetected", 0);
        calibrationCompleted = settings.getBoolean("calibrationCompleted", false);
        calibratorUserStepLength = settings.getFloat("calibratorUserStepLength", 0f);
        isCalibrationActive = settings.getBoolean("isCalibrationActive", false);
    }

    public static void saveToConfig(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();

        editor.
                putFloat("calibratorWalkedDistance", calibratorWalkedDistance).
                putInt("calibratorStepsDetected", calibratorStepsDetected).
                putBoolean("calibrationCompleted", calibrationCompleted).
                putFloat("calibratorUserStepLength", calibratorUserStepLength).
                putBoolean("isCalibrationActive", isCalibrationActive).
                apply();
    }

    private GnssStatus.Callback mGnssStatusCallback;
    @Deprecated private GpsStatus.Listener mStatusListener;
    private LocationManager mLocationManager;

    public int counter=0;
    public StepCalibratorService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");
    }

    public StepCalibratorService() {
    }

    @Override
    public void onCreate() {
        mSensorListener = SensorListener.getInstance(this);
        mSensorListener.addStepDetectedListener(this);
        loadFromConfig(this);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mLocationPermissionsGranted = MainActivity.locationPermissionsGranted(this);
        if (!mLocationPermissionsGranted) {
            // Kill this service as without GPS it is impossible to calibrate user's step length
            stopSelf();
            return;
        }

        // TODO: Check whether it does make sense to acquire location each second
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL, MIN_DISTANCE, this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mGnssStatusCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    satelliteStatusChanged();
                }

                @Override
                public void onFirstFix(int ttffMillis) {
                    gpsFixAcquired();

                }
            };
            mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
        } else {
            mStatusListener = new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                    switch (event) {
                        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                            satelliteStatusChanged();

                            break;
                        case GpsStatus.GPS_EVENT_FIRST_FIX:
                            // Do something.
                            gpsFixAcquired();
                            break;
                    }
                }
            };
            mLocationManager.addGpsStatusListener(mStatusListener);
        }
    }

    private void gpsFixAcquired() {
        // Do something.
        isGPSFix = true;
    }

    private void satelliteStatusChanged() {
        if (mLastLocation != null)
            isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < (GPS_UPDATE_INTERVAL * 2);

        if (isGPSFix) { // A fix has been acquired.
            // Do something.
        } else { // The fix has been lost.
            // Do something.
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;

        mLastLocationMillis = SystemClock.elapsedRealtime();

        // Do something.

        mLastLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startTimer();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("EXIT", "ondestroy!");
        mLocationManager.removeUpdates(this);
        saveToConfig(this);
        mSensorListener.removeStepDetectedListener(this);
        stopTimerTask();

        // Schedule resurrection of this service if we have permissions
        if (mLocationPermissionsGranted) {
            Intent broadcastIntent = new Intent("com.example.neutrino.maze.RestartSensor");
            sendBroadcast(broadcastIntent);
        }
    }

    private Timer timer;
    private TimerTask timerTask;
    long oldTime=0;

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 1 second
        timer.schedule(timerTask, 1000, 1000); //
    }

    /**
     * it sets the timer to print the counter every x seconds
     */
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.i("in timer", "in timer ++++  "+ (counter++));
            }
        };
    }

    /**
     * not needed
     */
    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStepDetected() {
        if (isGPSFix) {
            mCurrentSessionSteps++;
        }
    }
}
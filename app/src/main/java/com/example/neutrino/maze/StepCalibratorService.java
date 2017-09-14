package com.example.neutrino.maze;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.neutrino.maze.ui.MainActivity;
import com.example.neutrino.maze.util.Log4jHelper;

/**
 * Created by Greg Stein on 8/30/2017.
 */
public class StepCalibratorService extends Service implements LocationListener, SensorListener.IStepDetectedListener {
    private static final int GPS_MIN_TIME = 1000; // 1 sec
    public static final int GPS_MIN_DISTANCE = 10; // 10 meters
    public static final int MIN_SESSION_DISTANCE = 100; // in m
    public static final float MAX_WALKING_SPEED = 2.0f; // in m/s
    public static final int CALIBRATION_DISTANCE = 1000; // m
    private static final double SHORTEST_POSSIBLE_STEP = 0.3d; // 30cm
    private static final double LONGEST_POSSIBLE_STEP = 1.0d;  // 100cm
    public static final double GPS_ACCURACY = 5.0; // meters. Minimum acceptable accuracy
    public static final String STR_CALIBRATOR_WALKED_DISTANCE = "calibratorWalkedDistance";
    public static final String STR_CALIBRATOR_STEPS_DETECTED = "calibratorStepsDetected";
    public static final String STR_CALIBRATION_COMPLETED = "calibrationCompleted";
    public static final String STR_CALIBRATOR_USER_STEP_LENGTH = "calibratorUserStepLength";
    public static final String STEP_CALIBRATOR_PREFERENCES = "StepCalibratorServicePreferences";

    private static final Object stepsCounterMutex = new Object();
    private static final Object distanceMutex = new Object();
    private static float calibratorWalkedDistance;
    private static int calibratorStepsDetected;
    private static boolean calibrationCompleted;
    private static float calibratorUserStepLength;
    private long mLastLocationMillis;
    private Location mLastLocation;
    private int mCurrentSessionSteps = 0;
    private float mCurrentSessionDistance = 0f;

    private boolean mLocationPermissionsGranted;
    private SensorListener mSensorListener;
    private static org.apache.log4j.Logger log = Log4jHelper.getLogger("StepCalibratorService");
    private int mStepsFromLastLocation = 0;
    private LocationManager mLocationManager;

    public static void loadFromConfig(Service service) {
        SharedPreferences settings = service.getApplicationContext().
                getSharedPreferences(STEP_CALIBRATOR_PREFERENCES, MODE_PRIVATE);
        calibratorWalkedDistance = settings.getFloat(STR_CALIBRATOR_WALKED_DISTANCE, 0f);
        calibratorStepsDetected = settings.getInt(STR_CALIBRATOR_STEPS_DETECTED, 0);
        calibrationCompleted = settings.getBoolean(STR_CALIBRATION_COMPLETED, false);
        calibratorUserStepLength = settings.getFloat(STR_CALIBRATOR_USER_STEP_LENGTH, 0f);

        log.info("loadFromConfig: calibratorWalkedDistance = " + calibratorWalkedDistance);
        log.info("loadFromConfig: calibratorStepsDetected = " + calibratorStepsDetected);
    }

    public static void saveToConfig(Service service) {
        SharedPreferences settings = service.getApplicationContext().
                getSharedPreferences(STEP_CALIBRATOR_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        // TODO: remove after debug
//        calibratorWalkedDistance = calibratorStepsDetected = 0;
        editor.
                putFloat(STR_CALIBRATOR_WALKED_DISTANCE, calibratorWalkedDistance).
                putInt(STR_CALIBRATOR_STEPS_DETECTED, calibratorStepsDetected).
                putBoolean(STR_CALIBRATION_COMPLETED, calibrationCompleted).
                putFloat(STR_CALIBRATOR_USER_STEP_LENGTH, calibratorUserStepLength).
                apply();
    }

    public StepCalibratorService(Context applicationContext) {
        super();
    }

    private boolean calibrationCriteriaSatisfied() {
        return calibratorWalkedDistance > CALIBRATION_DISTANCE;
    }

    @Override
    public void onCreate() {
        mSensorListener = SensorListener.getInstance(this);
        mSensorListener.addStepDetectedListener(this);
        loadFromConfig(this);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mLocationPermissionsGranted = MainActivity.locationPermissionsGranted(this);
        if (!mLocationPermissionsGranted || calibrationCriteriaSatisfied()) {
            // Kill this service as without GPS it is impossible to calibrate user's step length
            stopSelf();
            return;
        }

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                GPS_MIN_TIME, GPS_MIN_DISTANCE, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;

        long currentMillis = SystemClock.elapsedRealtime();
        float distanceFromLastLocation;

        if (mLastLocation != null) {
            distanceFromLastLocation = location.distanceTo(mLastLocation); // in meters
            log.info("onLocationChanged: distanceFromLastLocation = " + distanceFromLastLocation);

            boolean locationTooFar = distanceFromLastLocation > GPS_MIN_DISTANCE * 1.5;

            float speed = location.getSpeed();
            if (speed == 0.0f) {
                float elapsedSeconds = (currentMillis - mLastLocationMillis) / 1000.0f;
                speed = distanceFromLastLocation / elapsedSeconds;
            }
            boolean speedTooHigh = speed > MAX_WALKING_SPEED;

            // This indicates if user is not moving (or indoors where GPS is inaccurate)
            boolean sloshingUser = (mStepsFromLastLocation > (distanceFromLastLocation / SHORTEST_POSSIBLE_STEP));

            boolean poorAccuracy = !location.hasAccuracy() || location.getAccuracy() >= GPS_ACCURACY;

            log.info("onLocationChanged: locationTooFar = " + locationTooFar);
            log.info("onLocationChanged: speedTooHigh = " + speedTooHigh);
            log.info("onLocationChanged: sloshingUser = " + sloshingUser);
            log.info("onLocationChanged: poorAccuracy = " + poorAccuracy);

            if (locationTooFar || speedTooHigh || sloshingUser) {// || poorAccuracy) {

                storeAndResetSession();

                if (calibrationCriteriaSatisfied()) {
                    // Job done (c) Orc from Warcraft
                    jobDone();
                    stopSelf();
                }
            } else {
                // Increment current session
                synchronized (distanceMutex) {
                    mCurrentSessionDistance += distanceFromLastLocation;
                }
            }
        }

        mLastLocationMillis = currentMillis;
        mStepsFromLastLocation = 0;
        mLastLocation = location;
    }

    public void storeAndResetSession() {
        log.info("onLocationChanged: mCurrentSessionDistance: " + mCurrentSessionDistance);
        log.info("onLocationChanged: mCurrentSessionSteps: " + mCurrentSessionSteps);
        // Split sessions. If current session is long enough to be accumulated, save it.
        // Also ensure that average step length is at max 1 meter

        final float sessionAverageStepLength = mCurrentSessionDistance / mCurrentSessionSteps;
        log.info("storeAndResetSession: sessionAverageStepLength: " + sessionAverageStepLength);
        if ((mCurrentSessionDistance > MIN_SESSION_DISTANCE) &&
                (sessionAverageStepLength >= SHORTEST_POSSIBLE_STEP) &&
                (sessionAverageStepLength <= LONGEST_POSSIBLE_STEP)) {

            // This could be called from onDestroy or onLocationChanged
            synchronized (this) {
                calibratorWalkedDistance += mCurrentSessionDistance;
                calibratorStepsDetected += mCurrentSessionSteps;
            }
        }
        synchronized (distanceMutex) {
            mCurrentSessionDistance = 0;
        }
        synchronized (stepsCounterMutex) {
            mCurrentSessionSteps = 0;
        }

        log.info("storeAndResetSession: calibratorWalkedDistance: " + calibratorWalkedDistance);
        log.info("storeAndResetSession: calibratorStepsDetected: " + calibratorStepsDetected);

        log.info("------------------------------------------------------------");

//        Toast.makeText(this, String.format("Session ended. Steps: %d, distance: %.2f",
//                calibratorStepsDetected, calibratorWalkedDistance), Toast.LENGTH_SHORT).show();
    }

    public void jobDone() {
        final float averageStepLength = calibratorWalkedDistance / calibratorStepsDetected;

        calibrationCompleted = true;
        calibratorUserStepLength = averageStepLength;
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
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mLocationManager.removeUpdates(this);
        mSensorListener.removeStepDetectedListener(this);

        storeAndResetSession();
        if (calibrationCriteriaSatisfied()) {
            jobDone();
        } else if (mLocationPermissionsGranted ) {
            // Schedule resurrection of this service if we have permissions and job is not done
            Intent broadcastIntent = new Intent("com.example.neutrino.maze.RestartSensor");
            sendBroadcast(broadcastIntent);
        }
        saveToConfig(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStepDetected() {
        synchronized (stepsCounterMutex) {
            mCurrentSessionSteps++;
        }

        log.info("onStepDetected: mCurrentSessionSteps: " + mCurrentSessionSteps);
        Log.i("StepCalibratorService", "onStepDetected: mCurrentSessionSteps: " + mCurrentSessionSteps);
        mStepsFromLastLocation++;
    }
}
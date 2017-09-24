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

import com.example.neutrino.maze.ui.MainActivity;

/**
 * Created by Greg Stein on 8/30/2017.
 */
public class StepCalibratorService extends Service implements LocationListener, SensorListener.IStepDetectedListener {
    private static final int GPS_MIN_TIME = 0; // 0 sec - no time restriction
    public static final int GPS_MIN_DISTANCE = 20; // meters
    public static final int MIN_SESSION_DISTANCE = 50; // in m
    public static final float MIN_WALKING_SPEED = 0.9f; // in m/s
    public static final float MAX_WALKING_SPEED = 1.9f; // in m/s
    // http://ac.els-cdn.com/S1877042813045515/1-s2.0-S1877042813045515-main.pdf?_tid=8fa735fa-9d62-11e7-8126-00000aab0f01&acdnat=1505843410_51f6ef0fc62581f5f17fcda53092754f
    public static final int CALIBRATION_DISTANCE = 1000; // m
    private static final float SHORTEST_POSSIBLE_STEP = 0.58f; // 58cm - 2% of population has shorter stride
    private static final float LONGEST_POSSIBLE_STEP = 0.94f;  // 94cm - 2% of population has longer stride
//    http://n.saunier.free.fr/saunier/stock/saunier11pedestrian-stride.pdf
//    http://homepage.stat.uiowa.edu/~mbognar/applets/normal.html
    public static final double GPS_ACCURACY = 5.0; // meters. Minimum acceptable accuracy
    public static final String STR_CALIBRATOR_WALKED_DISTANCE = "calibratorWalkedDistance";
    public static final String STR_CALIBRATOR_STEPS_DETECTED = "calibratorStepsDetected";
    public static final String STR_CALIBRATION_COMPLETED = "calibrationCompleted";
    public static final String STR_CALIBRATOR_USER_STEP_LENGTH = "calibratorUserStepLength";
    public static final String STEP_CALIBRATOR_PREFERENCES = "StepCalibratorServicePreferences";

    private static final Object stepsCounterMutex = new Object();
    private static final Object distanceMutex = new Object();
    private volatile static float calibratorWalkedDistance;
    private volatile static int calibratorStepsDetected;
    private volatile static boolean calibrationCompleted;
    private volatile static float calibratorUserStepLength;

    private long mLastLocationMillis;
    private Location mLastLocation;
    private volatile int mCurrentSessionSteps = 0;
    private volatile float mCurrentSessionDistance = 0f;
    private boolean mLocationPermissionsGranted;
    private SensorListener mSensorListener;
    private int mStepsFromLastLocation = 0;
    private LocationManager mLocationManager;

    public static void loadFromConfig(Service service) {
        SharedPreferences settings = service.getApplicationContext().
                getSharedPreferences(STEP_CALIBRATOR_PREFERENCES, MODE_PRIVATE);
        calibratorWalkedDistance = settings.getFloat(STR_CALIBRATOR_WALKED_DISTANCE, 0f);
        calibratorStepsDetected = settings.getInt(STR_CALIBRATOR_STEPS_DETECTED, 0);
        calibrationCompleted = settings.getBoolean(STR_CALIBRATION_COMPLETED, false);
        calibratorUserStepLength = settings.getFloat(STR_CALIBRATOR_USER_STEP_LENGTH, 0f);
    }

    public static void saveToConfig(Service service) {
        SharedPreferences settings = service.getApplicationContext().
                getSharedPreferences(STEP_CALIBRATOR_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.
                putFloat(STR_CALIBRATOR_WALKED_DISTANCE, calibratorWalkedDistance).
                putInt(STR_CALIBRATOR_STEPS_DETECTED, calibratorStepsDetected).
                putBoolean(STR_CALIBRATION_COMPLETED, calibrationCompleted).
                putFloat(STR_CALIBRATOR_USER_STEP_LENGTH, calibratorUserStepLength).
                apply();
    }

    public StepCalibratorService() {}
    public StepCalibratorService(Context applicationContext) {
        super();
    }

    private boolean calibrationCriteriaSatisfied() {
        return calibratorWalkedDistance > CALIBRATION_DISTANCE;
    }

    @Override
    public void onCreate() {
        loadFromConfig(this);
        mSensorListener = SensorListener.getInstance(this);
        mSensorListener.addStepDetectedListener(this);
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
            boolean locationTooFar = distanceFromLastLocation > GPS_MIN_DISTANCE * 1.5;

            float speed = location.getSpeed();
            if (speed == 0.0f) {
                float elapsedSeconds = (currentMillis - mLastLocationMillis) / 1000.0f;
                speed = distanceFromLastLocation / elapsedSeconds;
            }
            boolean speedInRange = MIN_WALKING_SPEED <= speed && speed <= MAX_WALKING_SPEED;

            // TODO: check actual values of accuracy (print to log)
            boolean poorAccuracy = !location.hasAccuracy() || location.getAccuracy() >= GPS_ACCURACY;

            // Maybe it is better to complete longer distance and not cut it off?
            boolean calibrationDistanceReached = (calibratorWalkedDistance + mCurrentSessionDistance) >= CALIBRATION_DISTANCE;

            float stepLength = distanceFromLastLocation / mStepsFromLastLocation;
            boolean stepLengthInRange = SHORTEST_POSSIBLE_STEP <= stepLength
                    && stepLength <= LONGEST_POSSIBLE_STEP;

            if ((locationTooFar && poorAccuracy) || !speedInRange || !stepLengthInRange || calibrationDistanceReached) {// || poorAccuracy) {

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
                synchronized (stepsCounterMutex) {
                    mCurrentSessionSteps += mStepsFromLastLocation;
                }
            }
        }

        mLastLocationMillis = currentMillis;
        mStepsFromLastLocation = 0;
        mLastLocation = location;
    }

    public void storeAndResetSession() {
        // Split sessions. If current session is long enough to be accumulated, save it.
        // Also ensure that average step length is at max 1 meter
        final float sessionAverageStepLength = mCurrentSessionDistance / mCurrentSessionSteps;
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
        mStepsFromLastLocation++;
    }
}
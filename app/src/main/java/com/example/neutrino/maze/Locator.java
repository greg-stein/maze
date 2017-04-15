package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;
import com.example.neutrino.maze.WifiScanner.IFingerprintAvailableListener;
import com.example.neutrino.maze.SensorListener.IStepDetectedListener;
import com.example.neutrino.maze.SensorListener.IDeviceRotationListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 4/11/2017.
 */

public class Locator implements IFingerprintAvailableListener, IStepDetectedListener, IDeviceRotationListener {
    private static final float STEP_LENGTH = 0.68f; // human average step: 78cm
    private static final int WINDOW_SIZE = 3;
    private static final int MAX_VARIANCES_NUM = 4;

    private static Locator instance = new Locator();
    public static Locator getInstance() {return instance;}

    private WifiScanner mWifiScanner = WifiScanner.getInstance();
    private SensorListener mSensorListener = SensorListener.getInstance();
    private WiFiLocator mWifiLocator = WiFiLocator.getInstance();
    private MovingAveragePointsQueue mLastLocations = new MovingAveragePointsQueue(WINDOW_SIZE);
    private PointF mCurrentLocation = new PointF(Float.MAX_VALUE, Float.MAX_VALUE);
    private double mCurrentDegree;
    private List<ILocationUpdatedListener> mLocationUpdatedListeners = new ArrayList<>();
    private float mNorth = 0.0f; // North correction
    private FloorPlan mFloorPlan;

    private Locator() {
        mWifiScanner.addFingerprintAvailableListener(this);
        mSensorListener.addStepDetectedListener(this);
        mSensorListener.addDeviceRotationListener(this);
    }

    public interface ILocationUpdatedListener {
        void onLocationUpdated(PointF location);
    }

    public void addLocationUpdatedListener(ILocationUpdatedListener listener) {
        mLocationUpdatedListeners.add(listener);
    }

    public void removeLocationUpdatedListener(ILocationUpdatedListener listener) {
        mLocationUpdatedListeners.remove(listener);
    }

    private void emitLocationUpdatedEvent(PointF location) {
        for (ILocationUpdatedListener listener : mLocationUpdatedListeners)
        {
            listener.onLocationUpdated(location);
        }
    }

    private boolean locationFixRequired() {
        PointF meanOfLastLocations = mLastLocations.meanPoint();
        // If queue is empty, location fix is needed for sure
        if (Float.isNaN(meanOfLastLocations.x) || Float.isNaN(meanOfLastLocations.y)) {
            return true;
        }

        final float diffX = meanOfLastLocations.x - mCurrentLocation.x;
        final float diffY = meanOfLastLocations.y - mCurrentLocation.y;
        double squaredDistanceFromMean = diffX * diffX + diffY * diffY;

        // TODO: Define the threshold. Mind time passed between wifi readings
        return mLastLocations.getItemsNum() > 0 && squaredDistanceFromMean > MAX_VARIANCES_NUM * mLastLocations.variance();
    }

    @Override
    public void onFingerprintAvailable(WiFiFingerprint fingerprint) {
        if (fingerprint.size() == 0) return; // cannot estimate location based on nothing

        PointF location = mWifiLocator.getLocation(fingerprint);
        // Failed to estimate location - nothing to report.
        if (Float.isNaN(location.x) || Float.isNaN(location.y)) return;

        mLastLocations.add(location);

        if (locationFixRequired()) {
            // Location reset required, use wifi locator to estimate location
            mCurrentLocation = location;
            emitLocationUpdatedEvent(location);
        }
    }

    @Override
    public void onStepDetected() {
        if (locationFixRequired()) {
            // Location reset required, use wifi locator to estimate location
            mCurrentLocation = mLastLocations.getLastItem();
        } else {
            final float stepX = (float) (Math.sin(Math.toRadians(mCurrentDegree)) * STEP_LENGTH);
            final float stepY = (float) (Math.cos(Math.toRadians(mCurrentDegree)) * STEP_LENGTH);
            mCurrentLocation.offset(stepX, stepY);
        }
        emitLocationUpdatedEvent(mCurrentLocation);
    }

    @Override
    public void onDeviceRotated(double degree) {
        mCurrentDegree = degree + mNorth;
    }

    public void setNorth(float north) {
        mNorth = north;
    }

    public void setFloorPlan(FloorPlan floorPlan) {
        this.mFloorPlan = floorPlan;
    }
}

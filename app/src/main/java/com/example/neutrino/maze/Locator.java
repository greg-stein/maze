package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.SensorListener.IDeviceRotationListener;
import com.example.neutrino.maze.SensorListener.IStepDetectedListener;
import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;
import com.example.neutrino.maze.WifiScanner.IFingerprintAvailableListener;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.rendering.VectorHelper;

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

    // For debug mode only
    private List<IDistributionUpdatedListener> mDistributionUpdatedListeners = new ArrayList<>();

    private Locator() {
        mWifiScanner.addFingerprintAvailableListener(this);
        mSensorListener.addStepDetectedListener(this);
        mSensorListener.addDeviceRotationListener(this);
    }

    public interface IDistributionUpdatedListener {
        void onDistributionUpdated(PointF mean, float stdev);
    }

    public void addDistributionUpdatedListener(IDistributionUpdatedListener listener) {
        mDistributionUpdatedListeners.add(listener);
    }

    private void emitDistributionUpdatedEvent(PointF mean, float stdev) {
        if (stdev == 0) return;
        for (IDistributionUpdatedListener listener : mDistributionUpdatedListeners) {
            listener.onDistributionUpdated(mean, stdev);
        }
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
        for (ILocationUpdatedListener listener : mLocationUpdatedListeners) {
            listener.onLocationUpdated(location);
        }
    }

    private boolean locationFixRequired() {
        PointF meanOfLastLocations = mLastLocations.meanPoint();
        // If queue is empty, location fix is needed for sure
        if (Float.isNaN(meanOfLastLocations.x) || Float.isNaN(meanOfLastLocations.y)) {
            return true;
        }

        if (mCurrentLocation == null) return true;

        final float diffX = meanOfLastLocations.x - mCurrentLocation.x;
        final float diffY = meanOfLastLocations.y - mCurrentLocation.y;
        double squaredDistanceFromMean = diffX * diffX + diffY * diffY;

        if (AppSettings.inDebug) {
            emitDistributionUpdatedEvent(meanOfLastLocations, (float) Math.sqrt(mLastLocations.variance()));
        }

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
            emitLocationUpdatedEvent(mCurrentLocation);
        }
    }

    // Greg TODO: 4/17/2017
    //          - try longer average queue (4, 5, ...) and see if it makes less jumps
    //          - consider timestamping the fingerprints IN THE QUEUE, so you know max distance
    //            travelled between them. This can result in better threshold locationFixRequired()
    //          V instead of halting the location marker when it hits obstacle, move it along
    //            the obstacle, either based on angle or wifi location
    //          V draw circle showing stdev
    //          - instead of last location use mean of locations
    @Override
    public void onStepDetected() {
        if (locationFixRequired()) {
            // Location reset required, use wifi locator to estimate location
            mCurrentLocation = mLastLocations.getLastItem();
        } else {
            final float stepX = (float) (Math.sin(Math.toRadians(mCurrentDegree)) * STEP_LENGTH);
            final float stepY = (float) (Math.cos(Math.toRadians(mCurrentDegree)) * STEP_LENGTH);
            PointF proposedLocation = new PointF(mCurrentLocation.x, mCurrentLocation.y);
            proposedLocation.offset(-stepX, stepY);
            if (hitObstacle(mCurrentLocation, proposedLocation)) {
                correctLocation(getObstacle(), proposedLocation);
            } else {
                mCurrentLocation = proposedLocation;
            }
        }

        if (mCurrentLocation != null) {
            emitLocationUpdatedEvent(mCurrentLocation);
        }
    }

    private boolean hitObstacle(PointF current, PointF next) {
        List<IFloorPlanPrimitive> sketch = mFloorPlan.getSketch();
        for (IFloorPlanPrimitive primitive : sketch) {
            if (primitive instanceof Wall) {
                Wall wall = (Wall) primitive;
                if (VectorHelper.linesIntersect(wall.getA(), wall.getB(), current, next)) {
                    mObstacle = wall;
                    return true;
                }
            }
        }
        return false;
    }

    private Wall mObstacle = null;
    protected Wall getObstacle() {return mObstacle;}

    private void correctLocation(Wall obstacle, PointF proposedLocation) {
        PointF proj = VectorHelper.projection(mCurrentLocation, proposedLocation, obstacle.getA(), obstacle.getB());
        mCurrentLocation.offset(proj.x, proj.y);
    }

    private void correctLocationAlongObstacle(Wall obstacle, PointF proposedLocation) {
        PointF proj = VectorHelper.projection(mCurrentLocation, proposedLocation, obstacle.getA(), obstacle.getB());
        float magnitude = proj.length();
        proj.set(proj.x / magnitude, proj.y / magnitude); // unit projection vector
        mCurrentLocation.offset(proj.x * STEP_LENGTH, proj.y * STEP_LENGTH);
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

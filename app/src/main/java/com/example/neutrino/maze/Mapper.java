package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.Locator.ILocationUpdatedListener;
import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.rendering.FloorPlanView;

import java.util.Stack;
import static com.example.neutrino.maze.WifiScanner.IFingerprintAvailableListener;

/**
 * Created by Greg Stein on 4/13/2017.
 */

public class Mapper implements ILocationUpdatedListener, IFingerprintAvailableListener {
    private static Mapper instance = new Mapper();
    private boolean mOldWifiScannerState;
    private boolean mFingerprintPlacedAtCurrentLocation = true;
    private PointF mCurrentLocation;

    private Mapper() {}
    public static Mapper getInstance() {return instance;}

    private Locator mLocator = Locator.getInstance();
    private WifiScanner mWifiScanner = WifiScanner.getInstance();
    private FloorPlan mFloorPlan;

    private boolean mIsEnabled = false;
    // These fields are for debug only
    private Stack<Fingerprint> mRecentlyAddedFingerprints = new Stack<>();

    private FloorPlanView mFloorPlanView;

    public void enable() {
        if (!mIsEnabled) {
            mIsEnabled = true;
            mOldWifiScannerState = mLocator.isWifiScannerUsed();
            mLocator.useWifiScanner(false);
            mLocator.addLocationUpdatedListener(this);
            mWifiScanner.addFingerprintAvailableListener(this);
        }
    }

    public void disable() {
        if (mIsEnabled) {
            mIsEnabled = false;
            mLocator.useWifiScanner(mOldWifiScannerState);
            mLocator.removeLocationUpdatedListener(this);
            mWifiScanner.removeFingerprintAvailableListener(this);
        }
    }

    @Override
    public void onLocationUpdated(PointF location) {
        mCurrentLocation = location;
        mFingerprintPlacedAtCurrentLocation = false;
    }

    @Override
    public void onFingerprintAvailable(WiFiFingerprint fingerprint) {
        if (!mFingerprintPlacedAtCurrentLocation) {
            // To display the fingerprint in debug mode
            if (AppSettings.inDebug) {
                // This call will also add the fingerprint to FloorPlan
                final Fingerprint newFp = mFloorPlanView.placeWiFiMarkAt(mCurrentLocation, fingerprint);
                mRecentlyAddedFingerprints.push(newFp); // to undo addition
            } else {
                Fingerprint newFp = new Fingerprint(mCurrentLocation, fingerprint);
                mFloorPlan.getFingerprints().add(newFp);
            }

            mFingerprintPlacedAtCurrentLocation = true;
        }
    }

    public void undoLastFingerprint() {
        if (AppSettings.inDebug) {
            if (mRecentlyAddedFingerprints.size() == 0) return;

            Fingerprint recentFingerprint = mRecentlyAddedFingerprints.pop();
            mFloorPlan.getSketch().remove(recentFingerprint);
            recentFingerprint.cloak();
            mFloorPlanView.renderPrimitive(recentFingerprint);
        }
    }

    public void setFloorPlanView(FloorPlanView floorPlanView) {
        if (AppSettings.inDebug) {
            this.mFloorPlanView = floorPlanView;
        }
    }

    public void setFloorPlan(FloorPlan floorPlan) {
        this.mFloorPlan = floorPlan;
    }
}

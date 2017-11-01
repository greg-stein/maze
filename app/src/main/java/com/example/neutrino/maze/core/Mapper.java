package com.example.neutrino.maze.core;

import android.content.Context;
import android.graphics.PointF;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.rendering.FloorPlanView;

import java.util.Stack;

/**
 * Created by Greg Stein on 4/13/2017.
 */

public class Mapper implements Locator.ILocationUpdatedListener, WifiScanner.IFingerprintAvailableListener {
    private boolean mOldWifiScannerState;
    private boolean mFingerprintPlacedAtCurrentLocation = true;
    private PointF mCurrentLocation;

    private static Mapper instance = null;
    private static final Object mutex = new Object();
    public static Mapper getInstance(Context context) {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null)
                    instance = new Mapper(context);
            }
        }
        return instance;
    }

    private Mapper(Context context) {
        mLocator = Locator.getInstance(context);
        mWifiScanner = WifiScanner.getInstance(context);
    }

    private Locator mLocator;
    private WifiScanner mWifiScanner;
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
    public void onFingerprintAvailable(WiFiLocator.WiFiFingerprint fingerprint) {
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

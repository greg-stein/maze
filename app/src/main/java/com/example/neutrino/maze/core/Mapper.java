package com.example.neutrino.maze.core;

import android.content.Context;
import android.graphics.PointF;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.rendering.FloorPlanView;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;

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
    private IFuckingSimpleGenericCallback<Fingerprint> mOnNewFingerprintListener;

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

    private void enable() {
        if (!mIsEnabled) {
            mIsEnabled = true;
            mOldWifiScannerState = mLocator.isWifiScannerUsed();
            mLocator.useWifiScanner(false);
            mLocator.addLocationUpdatedListener(this);
            mWifiScanner.addFingerprintAvailableListener(this);
        }
    }

    private void disable() {
        if (mIsEnabled) {
            mIsEnabled = false;
            mLocator.useWifiScanner(mOldWifiScannerState);
            mLocator.removeLocationUpdatedListener(this);
            mWifiScanner.removeFingerprintAvailableListener(this);
        }
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
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
            Fingerprint newFp = new Fingerprint(mCurrentLocation, fingerprint);
            mRecentlyAddedFingerprints.push(newFp); // to undo addition
            emitOnNewFingerprintEvent(newFp);

            mFingerprintPlacedAtCurrentLocation = true;
        }
    }

    public void setOnNewFingerprintListener(IFuckingSimpleGenericCallback<Fingerprint> listener) {
        mOnNewFingerprintListener = listener;
    }

    private void emitOnNewFingerprintEvent(Fingerprint fingerprint) {
        if (mOnNewFingerprintListener != null) {
            mOnNewFingerprintListener.onNotify(fingerprint);
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

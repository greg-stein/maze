package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.Locator.ILocationUpdatedListener;
import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.rendering.FloorPlanView;

import java.util.Stack;

/**
 * Created by Greg Stein on 4/13/2017.
 */

public class Mapper implements ILocationUpdatedListener {
    private static Mapper instance = new Mapper();
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
            mLocator.addLocationUpdatedListener(this);
        }
    }

    public void disable() {
        if (mIsEnabled) {
            mIsEnabled = false;
            mLocator.removeLocationUpdatedListener(this);
        }
    }
    @Override
    public void onLocationUpdated(PointF location) {
        WiFiFingerprint wifiFingerprint = mWifiScanner.getLastFingerprint();

        // To display the fingerprint in debug mode
        if (AppSettings.inDebug) {
            final Fingerprint fingerprint = mFloorPlanView.placeWiFiMarkAt(location, wifiFingerprint);
            mRecentlyAddedFingerprints.push(fingerprint); // to undo addition
        } else {
            Fingerprint fingerprint = new Fingerprint(location, wifiFingerprint);
            mFloorPlan.getFingerprints().add(fingerprint);
        }
    }

    public void undoLastFingerprint() {
        if (AppSettings.inDebug) {
            Fingerprint recentFingerprint = mRecentlyAddedFingerprints.pop();
            recentFingerprint.cloak();
        }
    }

    public void setFloorPlanView(FloorPlanView floorPlanView) {
        if (AppSettings.inDebug) {
            this.mFloorPlanView = floorPlanView;
        }
    }
}

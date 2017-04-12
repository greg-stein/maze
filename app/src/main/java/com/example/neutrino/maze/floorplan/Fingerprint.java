package com.example.neutrino.maze.floorplan;

import android.graphics.Color;

import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;

/**
 * Created by Greg Stein on 9/25/2016.
 *
 * This class is for debugging/development only!!
 */
public class Fingerprint extends Footprint {
    public static int instanceNum = 0;
    public final transient int instanceId = instanceNum++;
    public static final float MARK_RADIUS = 0.05f;

    private WiFiFingerprint mWiFiFingerprint;

    public Fingerprint() {
        super();
        setColor(Color.BLUE);
    }

    public Fingerprint(float cx, float cy, WiFiFingerprint fingerprint) {
        super(cx, cy, MARK_RADIUS);
        setColor(Color.BLUE);
        this.mWiFiFingerprint = fingerprint;
    }

    public WiFiFingerprint getFingerprint() {
        return mWiFiFingerprint;
    }
}

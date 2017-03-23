package com.example.neutrino.maze.floorplan;

import android.graphics.Color;

import com.example.neutrino.maze.WiFiTug;

import java.util.Map;

/**
 * Created by Greg Stein on 9/25/2016.
 *
 * This class is for debugging/development only!!
 */
public class WifiMark extends Footprint {
    public static int instanceNum = 0;
    public final transient int instanceId = instanceNum++;
    public static final float MARK_RADIUS = 0.05f;

    private WiFiTug.Fingerprint mFingerprint;

    public WifiMark() {
        super();
        setColor(Color.BLUE);
    }

    public WifiMark(float cx, float cy, WiFiTug.Fingerprint fingerprint) {
        super(cx, cy, MARK_RADIUS);
        setColor(Color.BLUE);
        this.mFingerprint = fingerprint;
    }

    public WiFiTug.Fingerprint getFingerprint() {
        return mFingerprint;
    }
}

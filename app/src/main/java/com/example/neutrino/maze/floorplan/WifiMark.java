package com.example.neutrino.maze.floorplan;

import android.graphics.Color;

import java.util.Map;

/**
 * Created by Greg Stein on 9/25/2016.
 *
 * This class is for debugging/development only!!
 */
public class WifiMark extends Footprint {
    private static final float MARK_RADIUS = 0.05f;

    private Map<String, Integer> mFingerprint;

    public WifiMark(float cx, float cy, Map<String, Integer> fingerprint) {
        super(cx, cy, MARK_RADIUS);
        setColor(Color.BLUE);
        this.mFingerprint = fingerprint;
    }
}

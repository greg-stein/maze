package com.example.neutrino.maze.floorplan;

import android.graphics.Color;
import android.graphics.PointF;
import android.support.v4.graphics.ColorUtils;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.core.WiFiLocator.WiFiFingerprint;

/**
 * Created by Greg Stein on 9/25/2016.
 *
 * This class is for debugging/development only!!
 */
public class Fingerprint extends Footprint {
    private static final int FINGERPRINT_ALPHA = 128;
    private static final float FINGERPRINT_RADIUS = 2.50f; // in meters
    private static final int FINGERPRINT_SEGMENTS_NUM = 6;  // TODO: this should be passed to
                                                            // TODO: footprint somehow
    public static int instanceNum = 0;
    public final transient int instanceId = instanceNum++;

    private WiFiFingerprint mWiFiFingerprint;

    public Fingerprint() {
        super();
        setColor(ColorUtils.setAlphaComponent(AppSettings.fingerprintColor, FINGERPRINT_ALPHA));
    }

    public Fingerprint(float cx, float cy, WiFiFingerprint fingerprint) {
        super(cx, cy, FINGERPRINT_RADIUS);
        setColor(ColorUtils.setAlphaComponent(AppSettings.fingerprintColor, FINGERPRINT_ALPHA));
        this.mWiFiFingerprint = fingerprint;
    }

    public Fingerprint(PointF location, WiFiFingerprint fingerprint) {
        this(location.x, location.y, fingerprint);
    }

    public WiFiFingerprint getFingerprint() {
        return mWiFiFingerprint;
    }
}

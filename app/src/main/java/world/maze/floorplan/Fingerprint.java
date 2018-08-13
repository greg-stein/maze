package world.maze.floorplan;

import android.graphics.PointF;
import android.support.v4.graphics.ColorUtils;

import world.maze.AppSettings;
import world.maze.core.WiFiLocator.WiFiFingerprint;

/**
 * Created by Greg Stein on 9/25/2016.
 *
 * This class is for debugging/development only!!
 */
public class Fingerprint extends CircleBase {
    private static final int FINGERPRINT_ALPHA = 64;
    private static final float FINGERPRINT_RADIUS = 2.50f; // in meters
    public static final int FINGERPRINT_CIRCLE_SEGMENTS_NUM = 6;

    public static int instanceNum = 0;
    public final transient int instanceId = instanceNum++;

    private WiFiFingerprint mWiFiFingerprint;

    @Override
    protected int getSegmentsNum() {
        return FINGERPRINT_CIRCLE_SEGMENTS_NUM;
    }

    @Override
    protected float getRadius() {
        return FINGERPRINT_RADIUS;
    }

    public Fingerprint() {
        super();
        setColor(ColorUtils.setAlphaComponent(AppSettings.fingerprintColor, FINGERPRINT_ALPHA));
    }

    public Fingerprint(float cx, float cy, WiFiFingerprint fingerprint) {
        super(cx, cy);
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

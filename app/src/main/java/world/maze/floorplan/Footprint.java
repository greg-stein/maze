package world.maze.floorplan;

import android.graphics.PointF;

/**
 * Created by Greg Stein on 9/21/2016.
 */
// TODO: to be deleted
public class Footprint extends CircleBase {
    private static final int SEGMENTS_NUM = 32;
    private static final float RADIUS = 2.5f; // in meters

    public void setRadius(float radius) {
        mRadius = radius;
    }

    private float mRadius;

    public Footprint() {
        super();
    }

    public Footprint(float cx, float cy) {
        super(cx, cy);
    }

    public Footprint(PointF center) {
        super(center);
    }

    public Footprint(float cx, float cy, float r) { }

    @Override
    protected int getSegmentsNum() {
        return SEGMENTS_NUM;
    }

    @Override
    protected float getRadius() {
        return RADIUS;
    }

    @Override
    public boolean equals(Object another) {
        if (!super.equals(another)) return false;
        if (!(another instanceof Footprint)) return false;

        Footprint anotherFootprint = (Footprint) another;

        if (anotherFootprint.mRadius != this.mRadius) return false;

        return true;
    }
}

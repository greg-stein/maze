package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;

import com.example.neutrino.maze.rendering.GlRenderBuffer;
import com.example.neutrino.maze.rendering.VectorHelper;

/**
 * Created by Greg Stein on 9/21/2016.
 */
public class Footprint extends FloorPlanPrimitiveBase {
    private static final int SEGMENTS_NUM = 6;
    private static final int VERTICES_NUM = SEGMENTS_NUM + 2; // radial vertices + center
    private static final int VERTICES_DATA_LENGTH = VERTICES_NUM * GlRenderBuffer.COORDS_PER_VERTEX;
    private static final int INDICES_DATA_LENGTH = SEGMENTS_NUM * 3;
    private static final float DEFAULT_RADIUS = 0.1f;
    private static final float DEFAULT_CENTER_SOURCE = 0;

    private final PointF mCenter = new PointF(0, 0);
    private float mRadius;

    @Override
    protected int getVerticesNum() {
        return VERTICES_DATA_LENGTH;
    }

    @Override
    protected int getIndicesNum() {
        return INDICES_DATA_LENGTH;
    }

    public Footprint() {
        init(DEFAULT_CENTER_SOURCE, DEFAULT_CENTER_SOURCE, DEFAULT_RADIUS);
    }

    public Footprint(float cx, float cy) {
        init(cx, cy, DEFAULT_RADIUS);
    }

    public Footprint(PointF center) {
        this(center.x, center.y);
    }

    public Footprint(float cx, float cy, float r) {
        init(cx, cy, r);
    }

    private void init(float centerX, float centerY, float radius) {
        mCenter.set(centerX, centerY);
        mRadius = radius;

        VectorHelper.buildCircle(mCenter, mRadius, SEGMENTS_NUM, mVertices);

        short i = 0;
        // First vertex = center of circle
        for (int s = 0; s < INDICES_DATA_LENGTH;) {
            mIndices[s++] = 0; // first vertex of triangle is always center
            mIndices[s++] = (short) (i + 1);
            mIndices[s++] = (short) (i + 2);
            i ++;
        }
    }

    @Override
    public void updateVertices() {
        VectorHelper.buildCircle(mCenter, mRadius, SEGMENTS_NUM, mVertices);
    }

    @Override
    public void scaleVertices(float scaleFactor) {
        mCenter.set(mCenter.x * scaleFactor, mCenter.y * scaleFactor);
    }

    @Override
    public RectF getBoundingBox() {
        return new RectF(
                mCenter.x - mRadius,
                mCenter.y - mRadius,
                mCenter.x + mRadius,
                mCenter.y + mRadius);
    }

    @Override
    public boolean equals(Object another) {
        if (!super.equals(another)) return false;
        if (!(another instanceof Footprint)) return false;

        Footprint anotherFootprint = (Footprint) another;

        if (!anotherFootprint.mCenter.equals(this.mCenter.x, this.mCenter.y)) return false;
        if (anotherFootprint.mRadius != this.mRadius) return false;

        return true;
    }

    public PointF getCenter() {
        return mCenter;
    }
}

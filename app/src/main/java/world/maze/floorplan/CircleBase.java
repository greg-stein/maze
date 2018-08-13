package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;

import com.example.neutrino.maze.rendering.GlRenderBuffer;
import com.example.neutrino.maze.rendering.VectorHelper;

/**
 * Created by Greg Stein on 12/22/2017.
 */

public abstract class CircleBase extends FloorPlanPrimitiveBase {
    public static final int DEFAULT_SEGMENTS_NUM = 6;
    private static final float DEFAULT_CENTER_SOURCE = 0f;

    private final PointF mCenter = new PointF(0, 0);

    protected abstract int getSegmentsNum();
    protected abstract float getRadius();

    public CircleBase() {
        init(DEFAULT_CENTER_SOURCE, DEFAULT_CENTER_SOURCE, getRadius());
    }

    public CircleBase(float cx, float cy) {
        init(cx, cy, getRadius());
    }

    public CircleBase(PointF center) {
        this(center.x, center.y);
    }

    @Override
    protected int getVerticesDataLength() {
        int verticesNum = getSegmentsNum() + 2;
        return verticesNum * GlRenderBuffer.COORDS_PER_VERTEX;
    }

    @Override
    protected int getIndicesNum() {
        return getSegmentsNum() * 3;
    }

    @Override
    public void updateVertices() {
        VectorHelper.buildCircle(mCenter, getRadius(), getSegmentsNum(), mVertices);
    }

    @Override
    public void scaleVertices(float scaleFactor) {
        mCenter.set(mCenter.x * scaleFactor, mCenter.y * scaleFactor);
    }

    @Override
    public RectF getBoundingBox() {
        float radius = getRadius();
        return new RectF(
                mCenter.x - radius,
                mCenter.y - radius,
                mCenter.x + radius,
                mCenter.y + radius);
    }

    private void init(float centerX, float centerY, float radius) {
        mCenter.set(centerX, centerY);

        VectorHelper.buildCircle(mCenter, getRadius(), getSegmentsNum(), mVertices);

        short i = 0;
        // First vertex = center of circle
        final double indices = getIndicesNum();
        for (int s = 0; s < indices;) {
            mIndices[s++] = 0; // first vertex of triangle is always center
            mIndices[s++] = (short) (i + 1);
            mIndices[s++] = (short) (i + 2);
            i ++;
        }
    }

    public PointF getCenter() {
        return mCenter;
    }

    public void setCenter(float x, float y) {
        mCenter.set(x, y);
    }

    @Override
    public boolean equals(Object another) {
        if (!super.equals(another)) return false;
        if (!(another instanceof CircleBase)) return false;

        CircleBase anotherCircle = (CircleBase) another;

        if (!anotherCircle.mCenter.equals(this.mCenter.x, this.mCenter.y)) return false;

        return true;
    }
}

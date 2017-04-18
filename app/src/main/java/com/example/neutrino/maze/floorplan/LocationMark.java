package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;

import com.example.neutrino.maze.rendering.GlRenderBuffer;
import com.example.neutrino.maze.rendering.VectorHelper;

/**
 * Created by Greg Stein on 9/22/2016.
 */
public class LocationMark extends FloorPlanPrimitiveBase {
    private static final int SEGMENTS_NUM = 64;
    private static final int VERTICES_NUM = 2 * (SEGMENTS_NUM + 1); // it is basically two circles
    private static final int VERTICES_DATA_LENGTH = VERTICES_NUM * GlRenderBuffer.COORDS_PER_VERTEX;
    private static final int INDICES_DATA_LENGTH = SEGMENTS_NUM * 6; // two triangles per segment
    private static final float DEFAULT_INNER_RADIUS = 0.15f;//4f;
    private static final float DEFAULT_OUTER_RADIUS = 0.2f;//5f;
    private static final float DEFAULT_CENTER_SOURCE = 0;

    private final PointF mCenter = new PointF(0, 0);
    transient private float mInnerRadius;
    transient private float mOuterRadius;

    public LocationMark() {
        init(DEFAULT_CENTER_SOURCE, DEFAULT_CENTER_SOURCE, DEFAULT_INNER_RADIUS, DEFAULT_OUTER_RADIUS);
    }

    public LocationMark(float cx, float cy) {
        init(cx, cy, DEFAULT_INNER_RADIUS, DEFAULT_OUTER_RADIUS);
    }

    public LocationMark(PointF center) {
        this(center.x, center.y);
    }

    public LocationMark(float cx, float cy, float ir, float or) {
        init(cx, cy, ir, or);
    }

    private void init(float centerX, float centerY, float innerRadius, float outerRadius) {
        mCenter.set(centerX, centerY);
        mInnerRadius = innerRadius;
        mOuterRadius = outerRadius;

        VectorHelper.buildRing(mCenter, mInnerRadius, mOuterRadius, SEGMENTS_NUM, mVertices);

        for (int i = 0, j = 0; i < INDICES_DATA_LENGTH;) {
            mIndices[i++] = (short) j++;
            mIndices[i++] = (short) j;
            mIndices[i++] = (short) (j + 1);
        }
    }

    @Override
    protected int getVerticesNum() {
        return VERTICES_DATA_LENGTH;
    }

    @Override
    protected int getIndicesNum() {
        return INDICES_DATA_LENGTH;
    }

    @Override
    public void updateVertices() {
        VectorHelper.buildRing(mCenter, mInnerRadius, mOuterRadius, SEGMENTS_NUM, mVertices);
    }

    @Override
    public void scaleVertices(float scaleFactor) {
        mCenter.set(mCenter.x * scaleFactor, mCenter.y * scaleFactor);
    }

    public void setCenter(PointF center) {
        mCenter.set(center);
    }

    @Override
    public boolean equals(Object another) {
        if (!super.equals(another)) return false;
        if (!(another instanceof LocationMark)) return false;

        LocationMark anotherLocationMark = (LocationMark) another;

        if (!anotherLocationMark.mCenter.equals(this.mCenter.x, this.mCenter.y)) return false;
        if (anotherLocationMark.mInnerRadius != this.mInnerRadius) return false;
        if (anotherLocationMark.mOuterRadius!= this.mOuterRadius) return false;

        return true;
    }
}

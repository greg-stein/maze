package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;
import android.support.v4.graphics.ColorUtils;

import com.example.neutrino.maze.rendering.GlRenderBuffer;
import com.example.neutrino.maze.rendering.VectorHelper;

/**
 * Created by Greg Stein on 3/26/2018.
 */

public class Ring extends FloorPlanPrimitiveBase {
    public static final int DEFAULT_SEGMENTS_NUM = 6;
    private static final float DEFAULT_CENTER_SOURCE = 0f;
    private static final float DEFAULT_RADIUS_SOURCE = 2f;

    private final PointF mCenter;
    private transient float mInnerRadius;
    private transient float mOuterRadius;
    private transient final int mSegmentsNum;
    private final transient PointF mTapLocation = new PointF();

    public float getInnerRadius() {
        return mInnerRadius;
    }

    public float getOuterRadius() {
        return mOuterRadius;
    }

    public void setInnerRadius(float innerRadius) {
        mInnerRadius = innerRadius;
    }

    public void setOuterRadius(float outerRadius) {
        mOuterRadius = outerRadius;
    }

    public Ring() {
        mCenter = new PointF(DEFAULT_CENTER_SOURCE, DEFAULT_CENTER_SOURCE);
        mInnerRadius = DEFAULT_RADIUS_SOURCE/2;
        mOuterRadius = DEFAULT_RADIUS_SOURCE;
        mSegmentsNum = DEFAULT_SEGMENTS_NUM;
        init(DEFAULT_CENTER_SOURCE, DEFAULT_CENTER_SOURCE, DEFAULT_RADIUS_SOURCE/2, DEFAULT_RADIUS_SOURCE, DEFAULT_SEGMENTS_NUM);
    }

    public Ring(float cx, float cy, float innerRadius, float outerRadius, int segmentsNum) {
        super(getVerticesDataLength(segmentsNum), getIndicesNum(segmentsNum));
        mCenter = new PointF(cx, cy);
        mInnerRadius = innerRadius;
        mOuterRadius = outerRadius;
        mSegmentsNum = segmentsNum;
        init(cx, cy, innerRadius, outerRadius, segmentsNum);
    }

    @Override
    protected int getVerticesDataLength() {
        return getVerticesDataLength(mSegmentsNum);
    }

    public static int getVerticesDataLength(int segmentsNum) {
        int verticesNum = 2 * (segmentsNum + 1); // two circles
        return verticesNum * GlRenderBuffer.COORDS_PER_VERTEX;
    }

    @Override
    protected int getIndicesNum() {
        return getIndicesNum(mSegmentsNum);
    }

    public static int getIndicesNum(int segmentsNum) {
        return segmentsNum * 3 * 2;
    }

    @Override
    public void updateVertices() {
        VectorHelper.buildRing(mCenter, mInnerRadius, mOuterRadius, mSegmentsNum, mVertices);
    }

    @Override
    public void scaleVertices(float scaleFactor) {
        mCenter.set(mCenter.x * scaleFactor, mCenter.y * scaleFactor);
    }

    @Override
    public RectF getBoundingBox() {
        return new RectF(
                mCenter.x - mOuterRadius,
                mCenter.y - mOuterRadius,
                mCenter.x + mOuterRadius,
                mCenter.y + mOuterRadius);
    }

    private void init(float centerX, float centerY, float innerRadius, float outerRadius, int segmentsNum) {
        mCenter.set(centerX, centerY);

        VectorHelper.buildRing(mCenter, innerRadius, outerRadius, segmentsNum, mVertices);

        final double indices = getIndicesNum();
        for (int i = 0, j = 0; i < indices;) {
            mIndices[i++] = (short) j;
            mIndices[i++] = (short) (j + 1);
            mIndices[i++] = (short) (j + 2);
            j++;
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
        if (!(another instanceof Ring)) return false;

        Ring anotherRing = (Ring) another;

        if (!anotherRing.mCenter.equals(this.mCenter.x, this.mCenter.y)) return false;

        return true;
    }

    @Override
    public void handleMove(float x, float y) {
        float dx = x - mTapLocation.x;
        float dy = y - mTapLocation.y;

        mCenter.offset(dx, dy);

        mTapLocation.set(x, y);
        updateVertices();
        rewriteToBuffer();
    }

    @Override
    public void setTapLocation(float x, float y) {
        mTapLocation.set(x, y);
    }

    @Override
    public void handleMoveStart() {
        setColor(ColorUtils.setAlphaComponent(getColor(), FloorPlanPrimitiveBase.ALPHA));
        rewriteToBuffer();
    }

    @Override
    public void handleMoveEnd() {
        setColor(ColorUtils.setAlphaComponent(getColor(), FloorPlanPrimitiveBase.OPAQUE));
        rewriteToBuffer();
    }

    @Override
    public boolean hasPoint(float x, float y) {
        float diffX = x - mCenter.x;
        float diffY = y - mCenter.y;
        return diffX * diffX + diffY * diffY < mOuterRadius * mOuterRadius;
    }
}

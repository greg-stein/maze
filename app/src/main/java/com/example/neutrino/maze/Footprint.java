package com.example.neutrino.maze;

import android.graphics.PointF;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Created by Greg Stein on 9/21/2016.
 */
public class Footprint implements IFloorPlanPrimitive {
    private static final int SEGMENTS_NUM = 16;
    private static final int VERTICES_NUM = SEGMENTS_NUM + 2; // radial vertices + center
    private static final int VERTICES_DATA_LENGTH = VERTICES_NUM * GlEngine.COORDS_PER_VERTEX;
    private static final int INDEX_DATA_LENGTH = SEGMENTS_NUM * 3;
    private static final float DEFAULT_RADIUS = 0.1f;
    private static final float DEFAULT_CENTER_SOURCE = 0;
    private boolean mIsRemoved;

    private transient final float mVertices[] = new float[VERTICES_DATA_LENGTH];
    private transient final short mDrawOrder[] = new short[INDEX_DATA_LENGTH]; // segment=triangle

    private transient int mVertexBufferPosition;
    private transient int mIndexBufferPosition;

    private final PointF mCenter = new PointF(0, 0);
    private float mRadius;
    private int mColor;
    private final float[] mColor4f = new float[GlEngine.COLORS_PER_VERTEX];

    public Footprint() {
        init(DEFAULT_CENTER_SOURCE, DEFAULT_CENTER_SOURCE, DEFAULT_RADIUS);
    }

    public Footprint(float cx, float cy) {
        init(cx, cy, DEFAULT_RADIUS);
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
        for (int s = 0; s < INDEX_DATA_LENGTH;) {
            mDrawOrder[s++] = 0; // first vertex of triangle is always center
            mDrawOrder[s++] = (short) (i + 1);
            mDrawOrder[s++] = (short) (i + 2);
            i ++;
        }
    }

    @Override
    public int getVerticesDataSize() {
        return VERTICES_DATA_LENGTH * GlEngine.SIZE_OF_FLOAT;
    }

    @Override
    public void updateVertices() {
        VectorHelper.buildCircle(mCenter, mRadius, SEGMENTS_NUM, mVertices);
    }

    @Override
    public void putVertices(FloatBuffer verticesBuffer) {
        mVertexBufferPosition = verticesBuffer.position();

        for (int i = 0; i < mVertices.length; i += GlEngine.COORDS_PER_VERTEX) {
            verticesBuffer.put(mVertices, i, GlEngine.COORDS_PER_VERTEX);    // put 3 floats of position
            verticesBuffer.put(mColor4f);            // put 4 floats of color
        }
    }

    @Override
    public void putIndices(ShortBuffer indexBuffer) {
        mIndexBufferPosition = indexBuffer.position();

        for (int i = 0; i < mDrawOrder.length; i++) {
            mDrawOrder[i] += mVertexBufferPosition/(GlEngine.COORDS_PER_VERTEX + GlEngine.COLORS_PER_VERTEX);
        }
        indexBuffer.put(mDrawOrder);
    }

    @Override
    public void updateBuffer(FloatBuffer verticesBuffer) {
        int lastPos = verticesBuffer.position();
        verticesBuffer.position(mVertexBufferPosition);

        putVertices(verticesBuffer);

        verticesBuffer.position(lastPos);
    }

    @Override
    public int getColor() {
        return mColor;
    }

    @Override
    public void setColor(int color) {
        this.mColor = color;
        VectorHelper.colorTo3F(mColor, mColor4f);
    }

    @Override
    public int getVertexBufferPosition() {
        return mVertexBufferPosition;
    }

    @Override
    public int getIndexBufferPosition() {
        return mIndexBufferPosition;
    }

    @Override
    public void setRemoved(boolean removed) {
        this.mIsRemoved = removed;
    }

    @Override
    public boolean isRemoved() {
        return mIsRemoved;
    }

    @Override
    public void cloak() {
        Arrays.fill(mVertices, 0);
    }
}

package com.example.neutrino.maze;

import android.graphics.PointF;
import android.graphics.RectF;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Created by neutrino on 7/7/2016.
 */
public class Wall {
    private static int instanceCounter = 0;
    public int nInstanceIndex = instanceCounter++;

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;
    private static final int VERTICES_NUM = 4; // it's a rect after all
    private static final int VERTICES_DATA_LENGTH = COORDS_PER_VERTEX * VERTICES_NUM;
    private static final float DEFAULT_WIDTH = 0.2f; // 20cm
    private static final float DEFAULT_COORDS_SOURCE = 0.5f;
    private boolean mIsRemoved;

    public ChangeType getChangeType() {
        return mChangeType;
    }

    // Different change types
    public enum ChangeType {
        CHANGE_A,
        CHANGE_B,
        CHANGE_WALL
    };

    private static final float CHANGE_ONE_END_THRESHOLD = 0.10f;

    private final float mVertices[] = new float[VERTICES_DATA_LENGTH];

    private final short mDrawOrder[] = { 0, 1, 2,   // first triangle
            1, 2, 3 }; // second triangle

    private int mVertexBufferPosition;
    private int mIndexBufferPosition;

    private final PointF mA = new PointF(0, 0);
    private final PointF mB = new PointF(0, 0);
    private float mWidth;

    private ChangeType mChangeType;
    private final PointF mTappedLocation = new PointF();

    public Wall() {
        init(-DEFAULT_COORDS_SOURCE, DEFAULT_COORDS_SOURCE, DEFAULT_COORDS_SOURCE,
                -DEFAULT_COORDS_SOURCE, DEFAULT_WIDTH);
    }

    public Wall(float x1, float y1, float x2, float y2)
    {
        init(x1, y1, x2, y2, DEFAULT_WIDTH);
    }

    public Wall(float x1, float y1, float x2, float y2, float width) {
        init(x1, y1, x2, y2, width/2);
    }

    private void init(float x1, float y1, float x2, float y2, float width) {
        mA.x = x1;
        mA.y = y1;
        mB.x = x2;
        mB.y = y2;
        mWidth = width;
        VectorHelper.splitLine(mA, mB, mWidth/2, mVertices);

        mChangeType = ChangeType.CHANGE_B;
    }

    public void updateVertices() {
        VectorHelper.splitLine(mA, mB, mWidth/2, mVertices);
    }

    public void putVertices(FloatBuffer verticesBuffer) {
        mVertexBufferPosition = verticesBuffer.position();
        for (int i = 0; i < mDrawOrder.length; i++) {
            mDrawOrder[i] += mVertexBufferPosition/GlEngine.COORDS_PER_VERTEX;
        }
        verticesBuffer.put(mVertices);
    }

    public void removeVertices(FloatBuffer verticesBuffer) {
        // This doesn't really removes this wall's vertices from the buffer,
        // Instead, it nulls them. If removal is desired, need to loop over
        // all walls in the buffer and update their vertices and indices.
        Arrays.fill(mVertices, 0);
        updateBuffer(verticesBuffer);
    }

    public void putIndices(ShortBuffer indexBuffer) {
        mIndexBufferPosition = indexBuffer.position();
        indexBuffer.put(mDrawOrder);
    }

    public void removeIndices(ShortBuffer indexBuffer) {
        // Nothing to do since the coords are simply nulled
    }

    public boolean hasPoint(float x, float y) {
        // First test if the point within bounding box of line
        RectF boundingBox = new RectF(mA.x, mA.y, mB.x, mB.y);
        boundingBox.sort();
        boundingBox.left -= mWidth/2;
        boundingBox.top -= mWidth/2;
        boundingBox.right += mWidth/2;
        boundingBox.bottom += mWidth/2;

        if (! boundingBox.contains(x, y)) {
            return false;
        }

        // Now check if distance from given point to line is less then twice the width
        // This uses method described on Wikipedia (https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line)
        float xDiff = mB.x - mA.x;
        float yDiff = mB.y - mA.y;

        float twiceArea = Math.abs(yDiff * x - xDiff * y + mB.x * mA.y - mB.y * mA.x);
        float distance = (float) (twiceArea / Math.sqrt(yDiff * yDiff + xDiff * xDiff));

        return distance <= mWidth;// /2;
    }

    public void updateBuffer(FloatBuffer verticesBuffer) {
        int lastPos = verticesBuffer.position();
        verticesBuffer.position(mVertexBufferPosition);
        verticesBuffer.put(mVertices);
        verticesBuffer.position(lastPos);
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float mWidth) {
        this.mWidth = mWidth;
    }

    public PointF getA() {
        return mA;
    }

    public void setA(float x, float y) {
        this.mA.set(x, y);
    }

    public void setA(PointF a) {
        this.mA.set(a);
    }

    public PointF getB() {
        return mB;
    }

    public void setB(float x, float y) {
        this.mB.set(x, y);
    }

    public void setB(PointF b) {
        this.mB.set(b);
    }

    public int getVertexBufferPosition() {
        return mVertexBufferPosition;
    }

    public int getIndexBufferPosition() {
        return mIndexBufferPosition;
    }

    public void handleChange(float x, float y) {
        float dx = x - mTappedLocation.x;
        float dy = y - mTappedLocation.y;

        switch (mChangeType) {
            case CHANGE_A: {
                mA.offset(dx, dy);
                break;
            }
            case CHANGE_B: {
                mB.offset(dx, dy);
                break;
            }
            case CHANGE_WALL: {
                mA.offset(dx, dy);
                mB.offset(dx, dy);
                break;
            }
        }

        mTappedLocation.set(x, y);
    }

    public void setTapLocation(float x, float y) {
        mTappedLocation.set(x, y);

        // Tapped point closer to A?
        if (PointF.length(mTappedLocation.x - mA.x, mTappedLocation.y - mA.y) <= CHANGE_ONE_END_THRESHOLD) {
            mChangeType = ChangeType.CHANGE_A;
        }
        // Closer to B?
        else if (PointF.length(mTappedLocation.x - mB.x, mTappedLocation.y - mB.y) <= CHANGE_ONE_END_THRESHOLD) {
            mChangeType = ChangeType.CHANGE_B;
        }
        else {
            mChangeType = ChangeType.CHANGE_WALL;
        }
    }

    public void setRemoved(boolean removed) {
        this.mIsRemoved = removed;
    }

    public boolean isRemoved() {
        return mIsRemoved;
    }

    public void cloak() {
        Arrays.fill(mVertices, 0);
    }
}

package com.example.neutrino.maze;

import android.graphics.PointF;
import android.graphics.RectF;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Created by neutrino on 7/7/2016.
 */
public class Wall implements IFloorPlanPrimitive {
    private static int instanceCounter = 0;
    public transient int nInstanceIndex = instanceCounter++;

    private static final int VERTICES_NUM = 4; // it's a rect after all
    private static final int VERTICES_DATA_LENGTH = VERTICES_NUM * GlEngine.COORDS_PER_VERTEX;
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

    private transient final float mVertices[] = new float[VERTICES_DATA_LENGTH];

    private transient final short mDrawOrder[] = { 0, 1, 2,   // first triangle
            1, 2, 3 }; // second triangle

    private transient int mVertexBufferPosition;
    private transient int mIndexBufferPosition;

    private final PointF mA = new PointF(0, 0);
    private final PointF mB = new PointF(0, 0);
    private float mWidth;
    private int mColor;
    private final float[] mColor4f = new float[GlEngine.COLORS_PER_VERTEX];

    private transient ChangeType mChangeType;
    private transient final PointF mTappedLocation = new PointF();

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

    @Override
    public int getVerticesDataSize() {
        return VERTICES_NUM * (GlEngine.COORDS_PER_VERTEX + GlEngine.COLORS_PER_VERTEX) * GlEngine.SIZE_OF_FLOAT;
    }

    @Override
    public void updateVertices() {
        VectorHelper.splitLine(mA, mB, mWidth/2, mVertices);
    }

    // This method puts vertex data into given buffer
    // Buffer.position() is saved internally for further updates
    // This method call should be followed immediately by putIndices() method call
    @Override
    public void putVertices(FloatBuffer verticesBuffer) {
        mVertexBufferPosition = verticesBuffer.position();

        for (int i = 0; i < mVertices.length; i += GlEngine.COORDS_PER_VERTEX) {
            // TODO: check 4-bytes alignment
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

    @Override
    public int getVertexBufferPosition() {
        return mVertexBufferPosition;
    }

    @Override
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

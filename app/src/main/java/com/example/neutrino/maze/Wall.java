package com.example.neutrino.maze;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by neutrino on 7/7/2016.
 */
public class Wall {
    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;
    private static final int VERTICES_NUM = 4; // it's a rect after all
    private static final float DEFAULT_WIDTH = 0.05f;
    private static final float DEFAULT_COORDS_SOURCE = 0.5f;

    private final float mCoords[] = new float[COORDS_PER_VERTEX * VERTICES_NUM];

    private final short mDrawOrder[] = { 0, 1, 2,   // first triangle
            1, 2, 3 }; // second triangle

    private int mVertexBufferPosition;
    private int mIndexBufferPosition;

    private final PointF mA = new PointF(0, 0);
    private final PointF mB = new PointF(0, 0);
    private float mWidth;

    public Wall() {
        init(-DEFAULT_COORDS_SOURCE, DEFAULT_COORDS_SOURCE, DEFAULT_COORDS_SOURCE,
                -DEFAULT_COORDS_SOURCE, DEFAULT_WIDTH);
    }

    public Wall(float x1, float y1, float x2, float y2)
    {
        init(x1, y1, x2, y2, DEFAULT_WIDTH);
    }

    public Wall(float x1, float y1, float x2, float y2, float width) {
        init(x1, y1, x2, y2, width);
    }

    private void init(float x1, float y1, float x2, float y2, float width) {
        mA.x = x1;
        mA.y = y1;
        mB.x = x2;
        mB.y = y2;
        mWidth = width;
        calcCoords();
    }

    public void calcCoords() {
        float[] vector = {mA.x - mB.x, mA.y - mB.y};
        float magnitude = (float) Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1]);
        float[] identityVector = {vector[0]/magnitude, vector[1]/magnitude};
        float[] orthogonalIdentityVector = {identityVector[1], -identityVector[0]};

        mCoords[0] = mA.x + mWidth * orthogonalIdentityVector[0];
        mCoords[1] = mA.y + mWidth * orthogonalIdentityVector[1];

        mCoords[3] = mA.x - mWidth * orthogonalIdentityVector[0];
        mCoords[4] = mA.y - mWidth * orthogonalIdentityVector[1];

        mCoords[6] = mB.x + mWidth * orthogonalIdentityVector[0];
        mCoords[7] = mB.y + mWidth * orthogonalIdentityVector[1];

        mCoords[9] = mB.x - mWidth * orthogonalIdentityVector[0];
        mCoords[10] = mB.y - mWidth * orthogonalIdentityVector[1];
    }

    public void putCoords(FloatBuffer vertexBuffer) {
        mVertexBufferPosition = vertexBuffer.position();
        for (int i = 0; i < mDrawOrder.length; i++) {
            mDrawOrder[i] += mVertexBufferPosition/GlEngine.COORDS_PER_VERTEX;
        }
        vertexBuffer.put(mCoords);
    }

    public boolean hasPoint(float x, float y) {
        // First test if the point within bounding box of line
        RectF boundingBox = new RectF(mA.x, mA.y, mB.x, mB.y);
        boundingBox.sort();

        if (! boundingBox.contains(x, y)) {
            return false;
        }

        // Now check if distance from given point to line is less then twice the width
        // This uses method described on Wikipedia (https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line)
        float xDiff = mB.x - mA.x;
        float yDiff = mB.y - mA.y;

        float twiceArea = Math.abs(yDiff * x - xDiff * y + mB.x * mA.y - mB.y * mA.x);
        float distance = (float) (twiceArea / Math.sqrt(yDiff * yDiff + xDiff * xDiff));
        return distance <= mWidth/2;
    }

    public void updateBuffer(FloatBuffer vertexBuffer) {
        int lastPos = vertexBuffer.position();
        vertexBuffer.position(mVertexBufferPosition);
        vertexBuffer.put(mCoords);
        vertexBuffer.position(lastPos);
    }

    public void putIndices(ShortBuffer indexBuffer) {
        mIndexBufferPosition = indexBuffer.position();
        indexBuffer.put(mDrawOrder);
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
        this.mA.x = x;
        this.mA.y = y;
    }

    public PointF getB() {
        return mB;
    }

    public void setB(float x, float y) {
        this.mB.x = x;
        this.mB.y = y;
    }

    public int getVertexBufferPosition() {
        return mVertexBufferPosition;
    }

    public int getIndexBufferPosition() {
        return mIndexBufferPosition;
    }

    public void mutate() {
        // Swap x coordinates between two points
        float tmp = mA.x;
        mA.x = mB.x;
        mB.x = tmp;

        calcCoords();
    }
}

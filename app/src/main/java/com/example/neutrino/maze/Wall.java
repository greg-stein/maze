package com.example.neutrino.maze;

import android.graphics.Point;
import android.graphics.PointF;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by neutrino on 7/7/2016.
 */
public class Wall {
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float mCoords[] = {
            // in counterclockwise order:
            -0.5f,  0.6f, 0.0f,   // top left
            -0.5f,  0.5f, 0.0f,   // bottom left
            0.5f,   0.5f, 0.0f,   // bottom right
            0.5f,   0.6f, 0.0f,   // top right
    };

    private final short mDrawOrder[] = { 0, 1, 2,   // first triangle
                                         1, 2, 3 }; // second triangle

    private int mVertexBufferPosition;
    private int mIndexBufferPosition;

    private final PointF mA = new PointF(-0.5f, 0.5f);
    private final PointF mB = new PointF(0.5f, -0.5f);
    private float mWidth = 0.05f;

    public Wall() {
        calcCoords();
    }

    private void calcCoords() {
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
        vertexBuffer.put(mCoords);
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
}

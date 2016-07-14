package com.example.neutrino.maze;

import android.graphics.Point;

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
    
    private float mA[] = {-0.5f,  0.5f};
    private float mB[] = {0.5f,   -0.5f};
    private float mWidth = 0.05f;

    public Wall() {
        calcCoords();
    }

    private void calcCoords() {
        float[] vector = {mA[0]-mB[0], mA[1]-mB[1]};
        float magnitude = (float) Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1]);
        float[] identityVector = {vector[0]/magnitude, vector[1]/magnitude};
        float[] orthogonalIdentityVector = {identityVector[1], -identityVector[0]};

        mCoords[0] = mA[0] + mWidth * orthogonalIdentityVector[0];
        mCoords[1] = mA[1] + mWidth * orthogonalIdentityVector[1];

        mCoords[3] = mA[0] - mWidth * orthogonalIdentityVector[0];
        mCoords[4] = mA[1] - mWidth * orthogonalIdentityVector[1];

        mCoords[6] = mB[0] + mWidth * orthogonalIdentityVector[0];
        mCoords[7] = mB[1] + mWidth * orthogonalIdentityVector[1];

        mCoords[9] = mB[0] - mWidth * orthogonalIdentityVector[0];
        mCoords[10] = mB[1] - mWidth * orthogonalIdentityVector[1];
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
}

package com.example.neutrino.maze;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by neutrino on 7/7/2016.
 */
public class Wall {
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float wallCoords[] = {
            // in counterclockwise order:
            -0.5f,  0.6f, 0.0f,   // top left
            -0.5f,  0.5f, 0.0f,   // bottom left
            0.5f,   0.5f, 0.0f,   // bottom right
            0.5f,   0.6f, 0.0f,   // top right
    };

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices
    private int mVertexBufferPosition;
    private int mIndexBufferPosition;

    public Wall() {
    }

    public void putCoords(FloatBuffer vertexBuffer) {
        mVertexBufferPosition = vertexBuffer.position();
        vertexBuffer.put(wallCoords);
    }

    public void putIndices(ShortBuffer indexBuffer) {
        mIndexBufferPosition = indexBuffer.position();
        indexBuffer.put(drawOrder);
    }
}

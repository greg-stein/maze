package com.example.neutrino.maze;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Greg Stein on 7/12/2016.
 */
public class GlEngine {
    static final int COORDS_PER_VERTEX = 3;
    static final int ORDER_INDICES_PER_QUAD = 6;
    static final int VERTICES_PER_QUAD = 4;

    private int mQuadsNum = 0;
    private int mLastCoordsIndex = 0;
    private int mLastOrderIndex = 0;

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    // Use to access and set the view transformation
    private int mMVPMatrixHandle;

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private final int mProgram;

    private int mPositionHandle;
    private int mColorHandle;

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 0.0f };

    public GlEngine(int quadsNum) {
        ByteBuffer bb = ByteBuffer.allocateDirect(quadsNum * VERTICES_PER_QUAD *
                COORDS_PER_VERTEX * 4); // 4 bytes per float
        bb.order(ByteOrder.nativeOrder()); // device hardware's native byte order
        vertexBuffer = bb.asFloatBuffer();

        ByteBuffer bb2 = ByteBuffer.allocateDirect(quadsNum *
                ORDER_INDICES_PER_QUAD * 2); // 2 bytes per short
        bb2.order(ByteOrder.nativeOrder());
        indexBuffer = bb2.asShortBuffer();

        int vertexShader = FloorPlanRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = FloorPlanRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    public void registerQuad(Wall quad) {
        quad.putCoords(vertexBuffer);
        quad.putIndices(indexBuffer);
        mQuadsNum++;
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Reset positions of buffers for consuming in GL
        vertexBuffer.position(0);
        indexBuffer.position(0);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw quads
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, mQuadsNum * ORDER_INDICES_PER_QUAD,
                GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}

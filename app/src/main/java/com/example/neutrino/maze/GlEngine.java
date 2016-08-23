package com.example.neutrino.maze;

import android.graphics.PointF;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 7/12/2016.
 */
public class GlEngine {
    public static final int COORDS_PER_VERTEX = 3;
    public static final int ORDER_INDICES_PER_QUAD = 6;
    public static final int VERTICES_PER_QUAD = 4;
    public static final int SIZE_OF_FLOAT = Float.SIZE/Byte.SIZE;
    public static final int SIZE_OF_SHORT = Short.SIZE/Byte.SIZE;
    private static final int BUFFERS_COUNT = 1;
    public static final int QUAD_VERTEX_DATA_SIZE = VERTICES_PER_QUAD * COORDS_PER_VERTEX * SIZE_OF_FLOAT;
    public static final float ALIGN_THRESHOLD = 0.1f;

    private int mWallsNum = 0;
    private List<Wall> mWalls = new ArrayList<Wall>();
    private List<Wall> mDeletedWalls = new ArrayList<Wall>();;

    private final FloatBuffer mVerticesBuffer;
    private final ShortBuffer mIndicesBuffer;

    private final int[] mVerticesBufferId = new int[BUFFERS_COUNT];
    private final int[] mIndicesBufferId = new int[BUFFERS_COUNT];

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

    private final int vertexStride = COORDS_PER_VERTEX * SIZE_OF_FLOAT;
    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 0.0f };

    public GlEngine(int quadsNum) {
        ByteBuffer bb = ByteBuffer.allocateDirect(quadsNum * VERTICES_PER_QUAD *
                COORDS_PER_VERTEX * SIZE_OF_FLOAT);
        bb.order(ByteOrder.nativeOrder()); // device hardware's native byte order
        mVerticesBuffer = bb.asFloatBuffer();

        ByteBuffer bb2 = ByteBuffer.allocateDirect(quadsNum *
                ORDER_INDICES_PER_QUAD * SIZE_OF_SHORT);
        bb2.order(ByteOrder.nativeOrder());
        mIndicesBuffer = bb2.asShortBuffer();

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        VectorHelper.colorTo3F(AppSettings.wallColor, color);
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public void registerWall(Wall wall) {
        wall.putVertices(mVerticesBuffer);
        wall.putIndices(mIndicesBuffer);
        mWallsNum++;
        mWalls.add(wall);
    }

    // Assumes given wall was registered
    public void unregisterWall(Wall wall) {
        wall.removeVertices(mVerticesBuffer);
        wall.removeIndices(mIndicesBuffer);
        mDeletedWalls.add(wall);
        wall.setRemoved(true);
        mWallsNum--;
    }

    public void removeWall(Wall wall) {
        wall.cloak();
        wall.setRemoved(true);
        mDeletedWalls.add(wall);
        updateSingleObject(wall);
        mWallsNum--;
    }

    public boolean hasWalls() {
        return mWallsNum > 0;
    }

    public Wall findWallHavingPoint(float x, float y) {
        for (Wall wall : mWalls) {
            if (!wall.isRemoved() && wall.hasPoint(x, y)) {
                return wall;
            }
        }

        return null;
    }

    public void copyToGpu(FloatBuffer vertices) {
        GLES20.glGenBuffers(BUFFERS_COUNT, mVerticesBufferId, 0);

        // Copy vertices data into GPU memory
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesBufferId[0]);
        // TODO: Should be vertices.limit() instead of vertices.capacity()
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.capacity() * SIZE_OF_FLOAT, vertices, GLES20.GL_DYNAMIC_DRAW);

        // Cleanup buffer
//        vertices.limit(0);
//        vertices = null;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void copyToGpu(ShortBuffer indices) {
        GLES20.glGenBuffers(BUFFERS_COUNT, mIndicesBufferId, 0);

        // Copy vertices data into GPU memory
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferId[0]);
        // TODO: Should be vertices.limit() instead of vertices.capacity()
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.capacity() * SIZE_OF_SHORT, indices, GLES20.GL_STATIC_DRAW);

        // Cleanup buffer
//        indices.limit(0);
//        indices = null;
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void updateSingleObject(Wall object) {
        object.updateBuffer(mVerticesBuffer);
        // offset in bytes
        int vertexBufferPosition = object.getVertexBufferPosition();
        int vertexOffset = vertexBufferPosition * SIZE_OF_FLOAT;

        int previousBufferPosition = mVerticesBuffer.position();
        mVerticesBuffer.position(vertexBufferPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesBufferId[0]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexOffset, QUAD_VERTEX_DATA_SIZE, mVerticesBuffer);
        mVerticesBuffer.position(previousBufferPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void render(float[] mvpMatrix) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesBufferId[0]);
        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, 0);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferId[0]);

        // Draw quads
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, mWallsNum * ORDER_INDICES_PER_QUAD,
                GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void allocateGpuBuffers() {
        int vertexPos = mVerticesBuffer.position();
        int indexPos = mIndicesBuffer.position();

        // Reset positions of buffers for consuming in GL
        mVerticesBuffer.position(0);
        mIndicesBuffer.position(0);

        // Fake full buffer
        mVerticesBuffer.limit(mVerticesBuffer.capacity());
        mIndicesBuffer.limit(mIndicesBuffer.capacity());

        copyToGpu(mVerticesBuffer);
        copyToGpu(mIndicesBuffer);

        mVerticesBuffer.position(vertexPos);
        mIndicesBuffer.position(indexPos);
    }

    public void deallocateGpuBuffers() {
        if (mVerticesBufferId[0] > 0) {
            GLES20.glDeleteBuffers(mVerticesBufferId.length, mVerticesBufferId, 0);
            mVerticesBufferId[0] = 0;
        }
        if (mIndicesBufferId[0] > 0) {
             GLES20.glDeleteBuffers(mIndicesBufferId.length, mIndicesBufferId, 0);
            mIndicesBufferId[0] = 0;
        }
    }

    @Override
    public void finalize() {
        deallocateGpuBuffers();
    }

    public void alignChangeToExistingWalls(Wall wall, PointF point) {
//        Wall existingWall = mWalls.get(0);
        // NOTE: When using single wall as reference for alignment, current wall
        //       is moved smoothly. However in the following loop, even if there
        //       presents only one wall, it still "jumps"
        // TODO: pick 3 near walls instead of looping through all of them.
        for (Wall existingWall : mWalls) {
            if (!existingWall.isRemoved()) {
                if (wall.getChangeType() == Wall.ChangeType.CHANGE_A) {
                    VectorHelper.alignVector(existingWall.getA(), existingWall.getB(), wall.getB(), point, ALIGN_THRESHOLD);
                }
                else if (wall.getChangeType() == Wall.ChangeType.CHANGE_B) {
                    VectorHelper.alignVector(existingWall.getA(), existingWall.getB(), wall.getA(), point, ALIGN_THRESHOLD);
                }
            }
        }
    }

    public int getWallsNum() {
        return mWallsNum;
    }
}

package com.example.neutrino.maze;

import android.content.res.Resources;
import android.graphics.PointF;
import android.opengl.GLES20;

import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Wall;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 7/12/2016.
 */
public class GlRenderBuffer {
    public static final int SIZE_OF_FLOAT = Float.SIZE/Byte.SIZE;
    public static final int SIZE_OF_SHORT = Short.SIZE/Byte.SIZE;

    public static final int COORDS_PER_VERTEX = 3;
    public static final int COLORS_PER_VERTEX = 4; // RGB + A
    public static final int INDICES_BUFFER_SIZE_FACTOR = 4; // we allocate indices buffer 4
    private static final int BUFFERS_COUNT = 1;
    private static final int STRIDE = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * SIZE_OF_FLOAT;

    private static final String POSITION_ATTRIBUTE = "a_Position";
    private static final String COLOR_ATTRIBUTE = "a_Color";

    private final FloatBuffer mVerticesBuffer;
    private final ShortBuffer mIndicesBuffer;

    private final int[] mVerticesBufferId = new int[BUFFERS_COUNT];
    private final int[] mIndicesBufferId = new int[BUFFERS_COUNT];


    static private String vertexShaderCode;
    static private String fragmentShaderCode;
    // Use to access and set the view transformation
    private int mMVPMatrixHandle;


    private final int mProgram;

    private int mPositionAttributeHandle;
    private int mColorAttributeHandle;

    static {
        vertexShaderCode = readResourceAsString(R.raw.vertex_shader);
        fragmentShaderCode = readResourceAsString(R.raw.fragment_shader);
    }

    private static String readResourceAsString(int resourceId) {
        Exception innerException;
        Resources r = AppSettings.appActivity.getResources();
        InputStream inputStream = r.openRawResource(resourceId);
        byte[] bytes = new byte[0];
        try {
            bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            innerException = e;
        }
        throw new RuntimeException("Cannot load shader code from resources.", innerException);
    }

    public GlRenderBuffer(int verticesNum) {
        // device hardware's native byte order
        mVerticesBuffer = ByteBuffer.allocateDirect(verticesNum *
                (COORDS_PER_VERTEX + COLORS_PER_VERTEX)* SIZE_OF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();

        mIndicesBuffer = ByteBuffer.allocateDirect(verticesNum *
                INDICES_BUFFER_SIZE_FACTOR * SIZE_OF_SHORT).order(ByteOrder.nativeOrder()).asShortBuffer();

        int vertexShader = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgram = ShaderHelper.createAndLinkProgram(vertexShader, fragmentShader,
                POSITION_ATTRIBUTE, COLOR_ATTRIBUTE);
    }

    public void registerPrimitive(IFloorPlanPrimitive primitive) {
        primitive.putVertices(mVerticesBuffer);
        primitive.putIndices(mIndicesBuffer);
    }

    public void removePrimitive(IFloorPlanPrimitive primitive) {
        primitive.cloak();
        primitive.setRemoved(true);
        updateSingleObject(primitive);
    }

    public void copyToGpu(FloatBuffer vertices) {
        GLES20.glGenBuffers(BUFFERS_COUNT, mVerticesBufferId, 0);

        // Copy vertices data into GPU memory
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesBufferId[0]);
        // TODO: Should be vertices.limit() instead of vertices.capacity()
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.capacity() * SIZE_OF_FLOAT, vertices, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void copyToGpu(ShortBuffer indices) {
        GLES20.glGenBuffers(BUFFERS_COUNT, mIndicesBufferId, 0);

        // Copy vertices data into GPU memory
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferId[0]);
        // TODO: Should be vertices.limit() instead of vertices.capacity()
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.capacity() * SIZE_OF_SHORT, indices, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void updateSingleObject(IFloorPlanPrimitive primitive) {
        primitive.updateBuffer(mVerticesBuffer);
        // offset in bytes
        int primitiveBufferOffset = primitive.getVertexBufferPosition();
        int vertexOffset = primitiveBufferOffset * SIZE_OF_FLOAT;

        int previousBufferPosition = mVerticesBuffer.position();
        mVerticesBuffer.position(primitiveBufferOffset);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesBufferId[0]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexOffset, primitive.getVerticesDataSize(), mVerticesBuffer);
        mVerticesBuffer.position(previousBufferPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void render(float[] mvpMatrix) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesBufferId[0]);
        GLES20.glUseProgram(mProgram);

        mPositionAttributeHandle = GLES20.glGetAttribLocation(mProgram, POSITION_ATTRIBUTE);
        GLES20.glVertexAttribPointer(mPositionAttributeHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                STRIDE, 0);
        GLES20.glEnableVertexAttribArray(mPositionAttributeHandle);

        mColorAttributeHandle = GLES20.glGetAttribLocation(mProgram, COLOR_ATTRIBUTE);
        GLES20.glVertexAttribPointer(mColorAttributeHandle, COLORS_PER_VERTEX, GLES20.GL_FLOAT, false,
                STRIDE, COORDS_PER_VERTEX * SIZE_OF_FLOAT);
        GLES20.glEnableVertexAttribArray(mColorAttributeHandle);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferId[0]);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);


        // Draw quads
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, mIndicesBuffer.position(),
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

    public void clear() {
        mVerticesBuffer.clear();
        mIndicesBuffer.clear();
        // TODO: more actions to come
    }
}

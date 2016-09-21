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
    public static final int SIZE_OF_FLOAT = Float.SIZE/Byte.SIZE;
    public static final int SIZE_OF_SHORT = Short.SIZE/Byte.SIZE;

    public static final int COORDS_PER_VERTEX = 3;
    public static final int COLORS_PER_VERTEX = 4; // RGB + A
    public static final int INDICES_BUFFER_SIZE_FACTOR = 4; // we allocate indices buffer 4
                                                            // times the number of vertices
    public static final int ORDER_INDICES_PER_QUAD = 6;
    private static final int BUFFERS_COUNT = 1;
    public static final float ALIGN_THRESHOLD = 0.1f;
    private static final int STRIDE = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * SIZE_OF_FLOAT;

    private static final String POSITION_ATTRIBUTE = "a_Position";
    private static final String COLOR_ATTRIBUTE = "a_Color";

    private int mMaxWallsNum = 0;
    private int mActualWallsNum = 0;
    private List<IFloorPlanPrimitive> mFloorPlanPrimitives = new ArrayList<>();
    private List<IFloorPlanPrimitive> mDeletedFloorPlanPrimitives = new ArrayList<>();

    private final FloatBuffer mVerticesBuffer;
    private final ShortBuffer mIndicesBuffer;

    private final int[] mVerticesBufferId = new int[BUFFERS_COUNT];
    private final int[] mIndicesBufferId = new int[BUFFERS_COUNT];


    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "uniform mat4 u_MVPMatrix;" +
                    "attribute vec4 a_Position;" + // Per-vertex position information we will pass in.
                    "attribute vec4 a_Color;" +    // Per-vertex color information we will pass in."
//                    "attribute vec4 vPosition;" +
                    "varying vec3 v_Position;" +
                    "varying vec4 v_Color;" + // This will be passed into the fragment shader.
                    "void main() {" +
                    "   v_Position = vec3(uMVPMatrix * a_Position);" +
                    // Pass through the color.
                    "   v_Color = a_Color;" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "   gl_Position = u_MVPMatrix * a_Position;" +
                    "}";

    // Use to access and set the view transformation
    private int mMVPMatrixHandle;

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec3 v_Position;" +		// Interpolated position for this fragment.
                    "varying vec4 v_Color;" +          	// This is the color from the vertex shader interpolated across the
                                                        // triangle per fragment.
                    "void main() {" +
                    "  gl_FragColor = v_Color;" +
                    "}";

    private final int mProgram;

    private int mPositionAttributeHandle;
    private int mColorAttributeHandle;

    public GlEngine(int verticesNum) {
        mMaxWallsNum = verticesNum;

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
        mFloorPlanPrimitives.add(primitive);
        if (primitive instanceof Wall) mActualWallsNum++;
    }

    public void removePrimitive(IFloorPlanPrimitive primitive) {
        primitive.cloak();
        primitive.setRemoved(true);
        mDeletedFloorPlanPrimitives.add(primitive);
        updateSingleObject(primitive);
        if (primitive instanceof Wall) mActualWallsNum--;
    }

    public Wall findWallHavingPoint(float x, float y) {
        for (IFloorPlanPrimitive primitive : mFloorPlanPrimitives) {
            if (primitive instanceof Wall) {
                Wall wall = (Wall) primitive;
                if (!wall.isRemoved() && wall.hasPoint(x, y)) {
                    return wall;
                }
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
                GLES20.GL_TRIANGLES, mMaxWallsNum * ORDER_INDICES_PER_QUAD,
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

    public void alignChangeToExistingWalls(Wall wall, PointF point) {

        Wall existingWall = getAnotherWall(wall);
        // NOTE: When using single wall as reference for alignment, current wall
        //       is moved smoothly. However in the following loop, even if there
        //       presents only one wall, it still "jumps"
        // TODO: pick 3 near walls instead of looping through all of them.
//        for (IFloorPlanPrimitive primitive : mFloorPlanPrimitives) {
//            if (primitive instanceof Wall) {
//                existingWall = (Wall) primitive;

                if (!existingWall.isRemoved()) {
                    if (wall.getChangeType() == Wall.ChangeType.CHANGE_A) {
                        VectorHelper.alignVector(existingWall.getA(), existingWall.getB(), wall.getB(), point, ALIGN_THRESHOLD);
                    } else if (wall.getChangeType() == Wall.ChangeType.CHANGE_B) {
                        VectorHelper.alignVector(existingWall.getA(), existingWall.getB(), wall.getA(), point, ALIGN_THRESHOLD);
                    }
                }
//            }
//        }
    }

    // This func is called when there are at least 2 walls
    private Wall getAnotherWall(Wall wall) {
        Wall firstWall = null;
        Wall secondWall = null;

        for (IFloorPlanPrimitive primitive: mFloorPlanPrimitives) {
            if (primitive instanceof Wall) {
                if (firstWall == null) {
                    firstWall = wall;
                }
                else if (secondWall == null) {
                    secondWall = wall;
                    break;
                }
            }
        }
        if (wall.equals(firstWall)) return secondWall;
        return firstWall;
    }

    public int getWallsNum() {
        return mActualWallsNum;
    }

    public List<IFloorPlanPrimitive> getFloorPlan() {
        return mFloorPlanPrimitives;
    }

    public void setFloorPlan(List<IFloorPlanPrimitive> primitives) {
        mVerticesBuffer.clear();
        mIndicesBuffer.clear();
        for(IFloorPlanPrimitive primitive: primitives) {
            primitive.updateVertices();
            registerPrimitive(primitive);
        }
    }
}

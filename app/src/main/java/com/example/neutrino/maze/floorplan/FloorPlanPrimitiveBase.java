package com.example.neutrino.maze.floorplan;

import com.example.neutrino.maze.rendering.GlRenderBuffer;
import com.example.neutrino.maze.rendering.VectorHelper;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Created by Greg Stein on 9/22/2016.
 */
public abstract class FloorPlanPrimitiveBase implements IFloorPlanPrimitive {
    protected transient final float mVertices[];
    protected transient final short mIndices[];

    protected abstract int getVerticesDataLength();

    protected abstract int getIndicesNum();

    private transient GlRenderBuffer mGlBuffer;

    protected FloorPlanPrimitiveBase() {
        mVertices = new float[getVerticesDataLength()];
        mIndices = new short[getIndicesNum()];
    }

    protected FloorPlanPrimitiveBase(int verticesNum, int indicesNum) {
        mVertices = new float[verticesNum];
        mIndices = new short[indicesNum];
    }

    @Override
    public void putVertices(FloatBuffer verticesBuffer) {
        mVertexBufferPosition = verticesBuffer.position();

        for (int i = 0; i < mVertices.length; i += GlRenderBuffer.COORDS_PER_VERTEX) {
            verticesBuffer.put(mVertices, i, GlRenderBuffer.COORDS_PER_VERTEX);    // put 3 floats of position
            verticesBuffer.put(mColor4f);            // put 4 floats of color
        }
    }

    @Override
    public void putIndices(ShortBuffer indexBuffer) {
        mIndexBufferPosition = indexBuffer.position();

        for (int i = 0; i < mIndices.length; i++) {
            mIndices[i] += mVertexBufferPosition / (GlRenderBuffer.COORDS_PER_VERTEX + GlRenderBuffer.COLORS_PER_VERTEX);
        }
        indexBuffer.put(mIndices);
    }

    @Override
    public void updateBuffer(FloatBuffer verticesBuffer) {
        int lastPos = verticesBuffer.position();
        verticesBuffer.position(mVertexBufferPosition);

        putVertices(verticesBuffer);

        verticesBuffer.position(lastPos);
    }

    @Override
    public int getVerticesDataSize() {
        return getVerticesDataLength() * (GlRenderBuffer.COORDS_PER_VERTEX + GlRenderBuffer.COLORS_PER_VERTEX) * GlRenderBuffer.SIZE_OF_FLOAT;
    }

    @Override
    public int getIndicesDataSize() {
        return getIndicesNum() * GlRenderBuffer.SIZE_OF_SHORT;
    }

    private transient int mColor;
    private transient final float[] mColor4f = new float[GlRenderBuffer.COLORS_PER_VERTEX];

    @Override
    public int getColor() {
        return mColor;
    }

    @Override
    public void setColor(int color) {
        this.mColor = color;
        VectorHelper.colorTo3F(mColor, mColor4f);
    }

    private transient int mVertexBufferPosition;

    @Override
    public int getVertexBufferPosition() {
        return mVertexBufferPosition;
    }

    private transient int mIndexBufferPosition;

    @Override
    public int getIndexBufferPosition() {
        return mIndexBufferPosition;
    }

    private boolean mIsRemoved;

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
        setRemoved(true);
        mGlBuffer.updateSingleObject(this);
    }

    @Override
    public boolean equals(Object another) {
        if (another == this) return true;
        if (!(another instanceof FloorPlanPrimitiveBase)) return false;

        FloorPlanPrimitiveBase anotherPrimitive = (FloorPlanPrimitiveBase) another;

        if (anotherPrimitive.mColor != this.mColor) return false;

        return true;
    }

    @Override
    public GlRenderBuffer getContainingBuffer() {
        return mGlBuffer;
    }

    @Override
    public void setContainingBuffer(GlRenderBuffer mGlBuffer) {
        this.mGlBuffer = mGlBuffer;
    }

    @Override
    public void rewriteToBuffer() {
        // This check is required mainly because if there is no buffer set we cannot render this
        // element. BUT, this invariant should be enforced at instantiation. We have it here just
        // because at the event of new element creation, we still didn't set the buffer, but this
        // method is called from handleMoveStart(), which in turn called from handleStartDrag()
        // This code/design smells!
        if (mGlBuffer != null) {
            mGlBuffer.updateSingleObject(this);
        }
    }

    @Override
    public void handleMove(float x, float y) {
    }

    @Override
    public void setTapLocation(float x, float y) {
    }

    @Override
    public void handleMoveStart() {}

    @Override
    public void handleMoveEnd() {}

    @Override
    public boolean hasPoint(float x, float y) {
        return false;
    }
}

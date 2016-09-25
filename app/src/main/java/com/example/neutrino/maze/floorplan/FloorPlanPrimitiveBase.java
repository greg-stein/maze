package com.example.neutrino.maze.floorplan;

import com.example.neutrino.maze.GlEngine;
import com.example.neutrino.maze.VectorHelper;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Created by Greg Stein on 9/22/2016.
 */
public abstract class FloorPlanPrimitiveBase implements IFloorPlanPrimitive {
    protected transient final float mVertices[];
    protected transient final short mIndices[];

    protected abstract int getVerticesNum();
    protected abstract int getIndicesNum();

    protected FloorPlanPrimitiveBase() {
        mVertices = new float[getVerticesNum()];
        mIndices = new short[getIndicesNum()];
    }

    @Override
    public void putVertices(FloatBuffer verticesBuffer) {
        mVertexBufferPosition = verticesBuffer.position();

        for (int i = 0; i < mVertices.length; i += GlEngine.COORDS_PER_VERTEX) {
            verticesBuffer.put(mVertices, i, GlEngine.COORDS_PER_VERTEX);    // put 3 floats of position
            verticesBuffer.put(mColor4f);            // put 4 floats of color
        }
    }

    @Override
    public void putIndices(ShortBuffer indexBuffer) {
        mIndexBufferPosition = indexBuffer.position();

        for (int i = 0; i < mIndices.length; i++) {
            mIndices[i] += mVertexBufferPosition/(GlEngine.COORDS_PER_VERTEX + GlEngine.COLORS_PER_VERTEX);
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
        return getVerticesNum() * (GlEngine.COORDS_PER_VERTEX + GlEngine.COLORS_PER_VERTEX) * GlEngine.SIZE_OF_FLOAT;
    }

    private int mColor;
    private final float[] mColor4f = new float[GlEngine.COLORS_PER_VERTEX];

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
    }

}

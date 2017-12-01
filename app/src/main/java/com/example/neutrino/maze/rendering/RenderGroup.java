package com.example.neutrino.maze.rendering;

import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 11/22/2017.
 */

public class RenderGroup {
    private GlRenderBuffer mCurrentBuffer;
    private List<GlRenderBuffer> mGlBuffers = new ArrayList<>();
    private List<IFloorPlanPrimitive> mElements;

    private boolean mReadyForRender;

    public RenderGroup(List<IFloorPlanPrimitive> elements) {
        mElements = elements;
    }

    // This method should run on GL thread
    public void prepareForRender() {
        if (mElements == null || mElements.size() == 0) return;

        mCurrentBuffer = new GlRenderBuffer(GlRenderBuffer.DEFAULT_BUFFER_VERTICES_NUM);
        mGlBuffers.add(mCurrentBuffer);

        for (IFloorPlanPrimitive element : mElements) {
            element.updateVertices();

            if (!mCurrentBuffer.put(element)) {
                mCurrentBuffer.allocateGpuBuffers();
                mCurrentBuffer = new GlRenderBuffer(GlRenderBuffer.DEFAULT_BUFFER_VERTICES_NUM);
                mGlBuffers.add(mCurrentBuffer);
                mCurrentBuffer.put(element);
            }
        }
        mCurrentBuffer.allocateGpuBuffers();

        mReadyForRender = true;
    }

    public boolean isReadyForRender() {
        return mReadyForRender;
    }

    public void render(float[] scratch) {
        for (GlRenderBuffer glBuffer : mGlBuffers) {
            glBuffer.render(scratch);
        }
    }

    public void glDeallocate() {
        for (GlRenderBuffer buffer : mGlBuffers) {
            buffer.deallocateGpuBuffers();
        }
    }
}

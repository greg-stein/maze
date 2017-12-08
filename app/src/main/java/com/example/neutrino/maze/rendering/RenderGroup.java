package com.example.neutrino.maze.rendering;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.IMoveable;

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

    public IFloorPlanPrimitive findElementHavingPoint(PointF p) {
        for (IFloorPlanPrimitive element : mElements) {
            if (element.hasPoint(p.x, p.y) && !element.isRemoved()) {
                return element;
            }
        }

        return null;
    }

    public boolean isEmpty() {
        return mElements.isEmpty();
    }

    public void removeElement(IFloorPlanPrimitive element) {
        mElements.remove(element);
    }

    public void clear() {
        for(IFloorPlanPrimitive primitive : mElements) {
            if (!primitive.isRemoved()) { // TODO: check if this is always true
                primitive.cloak();
            }
        }
        mElements.clear();
    }
}

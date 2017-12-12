package com.example.neutrino.maze.rendering;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 11/22/2017.
 */

public class RenderGroup {
    private GlRenderBuffer mCurrentBuffer = null;
    private List<GlRenderBuffer> mGlBuffers = new ArrayList<>();
    private List<IFloorPlanPrimitive> mRenderedElements = new ArrayList<>();
    private List<IFloorPlanPrimitive> mElementsNotRenderedYet;

    private boolean mReadyForRender;

    public RenderGroup(List<IFloorPlanPrimitive> elements) {
        mElementsNotRenderedYet = elements;
        mReadyForRender = false;
    }

    // This method should run on GL thread
    public void prepareForRender() {
        if (mElementsNotRenderedYet == null || mElementsNotRenderedYet.size() == 0) return;

        if (mCurrentBuffer == null) {
            mCurrentBuffer = new GlRenderBuffer(GlRenderBuffer.DEFAULT_BUFFER_VERTICES_NUM);
            mGlBuffers.add(mCurrentBuffer);
        }

        for (IFloorPlanPrimitive element : mElementsNotRenderedYet) {
            element.updateVertices();

            if (!mCurrentBuffer.put(element)) {
                mCurrentBuffer.allocateGpuBuffers();
                mCurrentBuffer = new GlRenderBuffer(GlRenderBuffer.DEFAULT_BUFFER_VERTICES_NUM);
                mGlBuffers.add(mCurrentBuffer);
                mCurrentBuffer.put(element);
            }

            mRenderedElements.add(element);
        }
        mElementsNotRenderedYet.clear();
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
        for (IFloorPlanPrimitive element : mRenderedElements) {
            if (element.hasPoint(p.x, p.y) && !element.isRemoved()) {
                return element;
            }
        }

        return null;
    }

    public boolean isEmpty() {
        return mRenderedElements.isEmpty();
    }

    public void addElement(IFloorPlanPrimitive element) {
        mElementsNotRenderedYet.add(element);
        mReadyForRender = false;
    }

    public void removeElement(IFloorPlanPrimitive element) {
        mRenderedElements.remove(element);
    }

    public void clear() {
        for(IFloorPlanPrimitive primitive : mRenderedElements) {
            if (!primitive.isRemoved()) { // TODO: check if this is always true
                primitive.cloak();
            }
        }
        mRenderedElements.clear();
    }
}

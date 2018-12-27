package world.maze.rendering;

import android.graphics.PointF;

import world.maze.floorplan.IFloorPlanPrimitive;
import world.maze.floorplan.IMoveable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Greg Stein on 11/22/2017.
 */

public class ElementsRenderGroup extends RenderGroupBase {
    private GlRenderBuffer mCurrentBuffer = null;
    private List<GlRenderBuffer> mGlBuffers = new ArrayList<>();
    private List<IFloorPlanPrimitive> mRenderedElements = new ArrayList<>();
    private List<IFloorPlanPrimitive> mElementsNotRenderedYet = new ArrayList<>();
    private boolean mReadyForRender;

    public ElementsRenderGroup(Collection<? extends IFloorPlanPrimitive> elements) {
        if (elements != null) {
            mElementsNotRenderedYet.addAll(elements);
        }
        mReadyForRender = false;
    }

    // This method should run on GL thread
    @Override
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

    @Override
    public boolean isReadyForRender() {
        return mReadyForRender;
    }

    @Override
    public void render(float[] scratch, float deviceAngle) {
        if (isVisible()) {
            for (GlRenderBuffer glBuffer : mGlBuffers) {
                glBuffer.render(scratch);
            }
        }
    }

    @Override
    public void glDeallocate() {
        for (GlRenderBuffer buffer : mGlBuffers) {
            buffer.deallocateGpuBuffers();
        }
    }

    @Override
    public IMoveable findElementHavingPoint(PointF p) {
        for (IFloorPlanPrimitive element : mRenderedElements) {
            if (element.hasPoint(p.x, p.y) && !element.isRemoved()) {
                return element;
            }
        }

        return null;
    }

    @Override
    public boolean isEmpty() {
        return mRenderedElements.isEmpty();
    }

    public void addElement(IFloorPlanPrimitive element) {
        mElementsNotRenderedYet.add(element);
        mReadyForRender = false;
        setChanged(true);
        emitElementAddedEvent(element);
    }

    @Override
    public void removeElement(IMoveable element) {
        mRenderedElements.remove(element);
        setChanged(true);
        emitElementRemovedEvent(element);
    }

    @Override
    public void clear() {
        for(IFloorPlanPrimitive primitive : mRenderedElements) {
            if (!primitive.isRemoved()) { // TODO: check if this is always true
                // TODO: this will call glUpdateBuffer for each element. It is possible to update
                // TODO: the whole buffer instead, which is faster
                primitive.cloak();
            }
        }
        mElementsNotRenderedYet.clear();
        mRenderedElements.clear();
        // TODO: What about emitting element deleted event? How to handle it?
    }
}

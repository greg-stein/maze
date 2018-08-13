package world.maze.rendering;

import world.maze.core.IMainView;
import world.maze.floorplan.IMoveable;

/**
 * Created by Greg Stein on 3/1/2018.
 */

public abstract class RenderGroupBase implements IRenderGroup {
    private boolean mChanged = false;
    private boolean mVisible = false;
    private IMainView.IRenderGroupChangedListener mChangedListener;

    @Override
    public boolean isVisible() {
        return mVisible;
    }

    @Override
    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    @Override
    public boolean isChanged() {
        return mChanged;
    }

    @Override
    public void setChanged(boolean changed) {
        if (changed != mChanged) {
            mChanged = changed;
        }
    }

    @Override
    public void setChangedListener(IMainView.IRenderGroupChangedListener listener) {
        mChangedListener = listener;
    }

    @Override
    public void setChangedElement(IMoveable element) {
        setChanged(true);
        emitElementChangedEvent(element);
    }

    protected void emitElementAddedEvent(IMoveable newElement) {
        if (mChangedListener != null) {
            mChangedListener.onElementAdd(newElement);
        }
    }

    protected void emitElementChangedEvent(IMoveable changedElement) {
        if (mChangedListener != null) {
            mChangedListener.onElementChange(changedElement);
        }
    }

    protected void emitElementRemovedEvent(IMoveable removedElement) {
        if (mChangedListener != null) {
            mChangedListener.onElementRemoved(removedElement);
        }
    }
}

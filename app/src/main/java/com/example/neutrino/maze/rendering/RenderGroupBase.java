package com.example.neutrino.maze.rendering;

import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;

/**
 * Created by Greg Stein on 3/1/2018.
 */

public abstract class RenderGroupBase implements IRenderGroup {
    private boolean mChanged = false;
    private boolean mVisible = false;
    private IFuckingSimpleGenericCallback<IRenderGroup> mChangedListener;

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
            emitOnChangedEvent();
        }
    }

    @Override
    public void setChangedListener(IFuckingSimpleGenericCallback<IRenderGroup> listener) {
        mChangedListener = listener;
    }

    private void emitOnChangedEvent() {
        if (mChangedListener != null) {
            mChangedListener.onNotify(this);
        }
    }
}

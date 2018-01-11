package com.example.neutrino.maze.rendering;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.IMoveable;

/**
 * Created by Greg Stein on 1/2/2018.
 */

public interface IRenderGroup {
    // This method should run on GL thread
    void prepareForRender();

    boolean isVisible();

    void setVisible(boolean visible);

    boolean isReadyForRender();

    void render(float[] scratch, float deviceAngle);

    void glDeallocate();

    IMoveable findElementHavingPoint(PointF p);

    boolean isEmpty();

    void removeElement(IMoveable element);

    void clear();

    boolean isChanged();

    void setChanged(boolean changed);
}

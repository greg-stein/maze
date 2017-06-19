package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Created by Greg Stein on 4/3/2017.
 */
public class Tag {
    private PointF mLocation;
    private String mLabel;
    private boolean mSearchable;
    private boolean mVisible;
    private transient float[] mBoundaryCorners;
    private transient float[] mBoundaryCornersTransformed;

    public Tag(PointF location, String label) {
        mLocation = location;
        mLabel = label;
    }

    public Tag(float x, float y, String label) {
        mLocation = new PointF(x, y);
        mLabel = label;
    }

    public PointF getLocation() {
        return mLocation;
    }

    public void setLocation(PointF location) {
        this.mLocation = location;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        this.mLabel = label;
    }

    public boolean isSearchable() {
        return mSearchable;
    }

    public void setSearchable(boolean searchable) {
        this.mSearchable = searchable;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(boolean visible) {
        this.mVisible = visible;
    }
    
    // In the clockwise order: topLeft, topRight, bottomRight, bottomLeft
    public float[] getBoundaryCorners() {
        return mBoundaryCorners;
    }

    public void setBoundaryCorners(float[] boundaries) {
        this.mBoundaryCorners = boundaries;
    }

    public void setBoundaryCorners(RectF boundaries) {
        if (this.mBoundaryCorners == null) mBoundaryCorners = new float[8];

        mBoundaryCorners[0] = boundaries.left;
        mBoundaryCorners[1] = boundaries.top;
        mBoundaryCorners[2] = boundaries.right;
        mBoundaryCorners[3] = boundaries.top;
        mBoundaryCorners[4] = boundaries.right;
        mBoundaryCorners[5] = boundaries.bottom;
        mBoundaryCorners[6] = boundaries.left;
        mBoundaryCorners[7] = boundaries.bottom;
    }

    public float[] getBoundaryCornersTransformed() {
        if (this.mBoundaryCornersTransformed == null)
            mBoundaryCornersTransformed = new float[8];
        return mBoundaryCornersTransformed;
    }
}

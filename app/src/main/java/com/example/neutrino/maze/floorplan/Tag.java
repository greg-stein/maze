package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;

import com.example.neutrino.maze.rendering.VectorHelper;

/**
 * Created by Greg Stein on 4/3/2017.
 */
public class Tag implements IMoveable {
    private PointF mLocation;
    private String mLabel;
    private boolean mSearchable;
    private boolean mVisible;
    private transient float[] mBoundaryCorners;
    private transient float[] mBoundaryCornersTransformed;
    private transient final PointF mTapLocation;
    private transient boolean mIsBeingMoved;
    private transient float mRenderedTextWidth;
    private transient float mRenderedTextHeight;

    public Tag() {
        mBoundaryCorners = new float[8];
        mBoundaryCornersTransformed = new float[8];
        mTapLocation = new PointF();
        mIsBeingMoved = false;
    }

    public Tag(PointF location, String label) {
        this();
        mLocation = location;
        mLabel = label;
    }

    public Tag(float x, float y, String label) {
        this();
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

    @Override
    public void handleChange(float x, float y) {
        float dx = x - mTapLocation.x;
        float dy = y - mTapLocation.y;

        this.mLocation.offset(dx, dy);
        mTapLocation.set(x, y);
        updateBoundariesRect();
    }

    @Override
    public void setTapLocation(float x, float y) {
        // Remember tap location relatively to topLeft corner
        mTapLocation.set(x, y);
    }

    @Override
    public boolean hasPoint(float x, float y) {
        return VectorHelper.rectHasPoint(mBoundaryCornersTransformed, x, y);
    }

    @Override
    public void handleChangeStart() {
        mIsBeingMoved = true;
    }

    @Override
    public void handleChangeEnd() {
        mIsBeingMoved = false;
    }

    // In the clockwise order: topLeft, topRight, bottomRight, bottomLeft
    public float[] getBoundaryCorners() {
        return mBoundaryCorners;
    }

    public void setBoundaryCorners(float[] boundaries) {
        this.mBoundaryCorners = boundaries;
    }

    public float[] getBoundaryCornersTransformed() {
        return mBoundaryCornersTransformed;
    }

    public boolean isBeingMoved() {
        return mIsBeingMoved;
    }

    public float getRenderedTextWidth() {
        return mRenderedTextWidth;
    }

    public void setRenderedTextWidth(float mRenderedTextWidth) {
        this.mRenderedTextWidth = mRenderedTextWidth;
    }

    public float getRenderedTextHeight() {
        return mRenderedTextHeight;
    }

    public void setRenderedTextHeight(float mRenderedTextHeight) {
        this.mRenderedTextHeight = mRenderedTextHeight;
    }

    public void updateBoundariesRect() {
        float left = mLocation.x - mRenderedTextWidth / 2;
        float top = mLocation.y - mRenderedTextHeight / 2;
        float right = mLocation.x + mRenderedTextWidth / 2;
        float bottom = mLocation.y + mRenderedTextHeight / 2;

        mBoundaryCorners[0] = left;
        mBoundaryCorners[1] = top;
        mBoundaryCorners[2] = right;
        mBoundaryCorners[3] = top;
        mBoundaryCorners[4] = right;
        mBoundaryCorners[5] = bottom;
        mBoundaryCorners[6] = left;
        mBoundaryCorners[7] = bottom;
    }
}

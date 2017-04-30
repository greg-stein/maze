package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;

/**
 * Created by Greg Stein on 4/3/2017.
 */
public class Tag {
    private PointF mLocation;
    private String mLabel;
    private boolean mSearchable;
    private boolean mVisible;

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
}

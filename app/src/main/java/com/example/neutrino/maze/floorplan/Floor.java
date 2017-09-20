package com.example.neutrino.maze.floorplan;

import com.example.neutrino.maze.floorplan.transitions.Teleport;

import java.util.List;

/**
 * Created by Greg Stein on 7/5/2017.
 * Entity to serialize/deserialize for the webservice
 */
public class Floor {
    private String mName;
    private String mId; // TODO: this should be changed to ObjectId later
    private List<Teleport> mTeleports;

    public Floor(String mName, String mId) {
        this.mName = mName;
        this.mId = mId;
    }

    private transient boolean mIsSelected;

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public List<Teleport> getTeleports() {
        return mTeleports;
    }

    public void setTeleports(List<Teleport> teleports) {
        mTeleports = teleports;
    }
}

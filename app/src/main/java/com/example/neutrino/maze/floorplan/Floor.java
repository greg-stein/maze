package com.example.neutrino.maze.floorplan;

import com.example.neutrino.maze.floorplan.transitions.ITeleport;
import com.example.neutrino.maze.floorplan.transitions.Teleport;

import java.util.Collections;
import java.util.List;

/**
 * Created by Greg Stein on 7/5/2017.
 * Entity to serialize/deserialize for the webservice
 */
public class Floor {
    public static Floor current;

    private String mName;
    private String mId; // TODO: this should be changed to ObjectId later
    private List<ITeleport> mTeleports;
    private List<Tag> mTags;

    public Floor() {}

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

    public List<ITeleport> getTeleports() {
        return mTeleports;
    }

    public void setTeleports(List<ITeleport> teleports) {
        mTeleports = teleports;
    }

    public List<Tag> getTags() {
        if (mTags != null) {
            return Collections.unmodifiableList(mTags);
        }
        return null;
    }

    public void setTags(List<Tag> tags) {
        mTags = tags;
    }
}

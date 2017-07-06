package com.example.neutrino.maze.floorplan;

/**
 * Created by Greg Stein on 7/5/2017.
 * Entity to serialize/deserialize for the webservice
 */
public class Floor {
    private String mName;
    private String mId; // TODO: this should be changed to ObjectId later

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
}

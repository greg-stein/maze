package com.example.neutrino.maze.floorplan;

/**
 * Created by Greg Stein on 7/7/2017.
 */

public class Building {
    private String mName;
    private String mAddress;
    private String mType;
    private String mID;

    public Building(String mName, String mAddress, String mType, String mID) {
        this.mName = mName;
        this.mAddress = mAddress;
        this.mType = mType;
        this.mID = mID;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        this.mAddress = address;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String getID() {
        return mID;
    }

    public void setID(String id) {
        this.mID = id;
    }
}

package com.example.neutrino.maze.floorplan;

import com.example.neutrino.maze.floorplan.transitions.ITeleport;
import com.example.neutrino.maze.floorplan.transitions.Teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg Stein on 7/7/2017.
 */

public class Building {
    public static Building current = null;

    private String mName;
    private String mAddress;
    private String mType;
    private String mID;
    private List<Floor> mFloors;
    private transient Floor mCurrentFloor;

    private transient Map<String, List<ITeleport>> mTeleportsById = new HashMap<>();

    public Building() {
    }

    public Building(String mName, String mAddress, String mType, String mID) {
        this.mName = mName;
        this.mAddress = mAddress;
        this.mType = mType;
        this.mID = mID;
        initTeleportsMap();
    }

    public static boolean isFloorDefined() {
        return Building.current != null && Building.current.mCurrentFloor != null;
    }

    private void initTeleportsMap() {
        if (null == mFloors) return;

        for (Floor floor : mFloors) {
            final List<Teleport> floorTeleports = floor.getTeleports();
            for (Teleport teleport : floorTeleports) {
                List<ITeleport> sameIdTeleports;
                final String teleportId = teleport.getId();

                if (mTeleportsById.containsKey(teleportId)) {
                    sameIdTeleports = mTeleportsById.get(teleportId);
                } else {
                    sameIdTeleports = new ArrayList<>();
                    mTeleportsById.put(teleportId, sameIdTeleports);
                }
                sameIdTeleports.add(teleport);
            }
        }
    }

    public List<ITeleport> getTeleportsById(String teleportId) {
        return mTeleportsById.get(teleportId);
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

    public List<Floor> getFloors() {
        return mFloors;
    }

    public void setFloors(List<Floor> floors) {
        mFloors = floors;
    }

    public Floor getCurrentFloor() {
        return mCurrentFloor;
    }

    public void setCurrentFloor(Floor currentFloor) {
        mCurrentFloor = currentFloor;
    }
}
package com.example.neutrino.maze.floorplan;

import com.example.neutrino.maze.floorplan.transitions.ITeleport;
import com.example.neutrino.maze.floorplan.transitions.Teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Set<String> mAccessPoints; // TODO: HashSet<Long>

    private transient Floor mCurrentFloor;
    private transient Map<String, List<ITeleport>> mTeleportsById = new HashMap<>();
    private transient boolean mDirty;

    public Building() {
    }

    public Building(String name, String address, String type, String id) {
        this.mName = name;
        this.mAddress = address;
        this.mType = type;
        this.mID = id;
        mAccessPoints = new HashSet<>();
        initTeleportsMap();
    }

    public static boolean isFloorDefined() {
        return Building.current != null && Building.current.mCurrentFloor != null;
    }

    private void initTeleportsMap() {
        if (null == mFloors) return;

        for (Floor floor : mFloors) {
            final List<ITeleport> floorTeleports = floor.getTeleports();
            for (ITeleport teleport : floorTeleports) {
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

    public String getId() {
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

    public void setCurrentFloor(String floorId) {
        for (Floor floor : mFloors) {
            if (floor.getId().equals(floorId)) {
                mCurrentFloor = floor;
                return;
            }
        }

        // TODO: deside if this is good.
        throw new RuntimeException("Floor with given ID doesn't exist in current building.");
    }

    public boolean isDirty() {
        return mDirty;
    }

    public void setDirty(boolean dirty) {
        mDirty = dirty;
    }
}
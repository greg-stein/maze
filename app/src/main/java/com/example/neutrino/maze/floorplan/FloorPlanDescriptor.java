package com.example.neutrino.maze.floorplan;

import java.util.List;

/**
 * Created by Greg Stein on 4/4/2017.
 */

public class FloorPlanDescriptor {
    private String mFloorPlanName;
    private List<String> mBuildingNames;

    public String getFloorPlanName() {
        return mFloorPlanName;
    }

    public void setFloorPlanName(String mFloorPlanName) {
        this.mFloorPlanName = mFloorPlanName;
    }

    public List<String> getBuildingNames() {
        return mBuildingNames;
    }

    public void setBuildingNames(List<String> mBuildingNames) {
        this.mBuildingNames = mBuildingNames;
    }
}

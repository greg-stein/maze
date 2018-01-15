package com.example.neutrino.maze.core;


import android.support.v4.util.Pair;

import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.RadioMapFragment;
import com.example.neutrino.maze.util.IFuckingSimpleCallback;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;
import com.example.neutrino.maze.core.WiFiLocator.WiFiFingerprint;

import java.util.List;

/**
 * Created by Greg Stein on 9/25/2017.
 */

public interface IMazeServer {
    void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback);
    void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated);
    void createFloorAsync(IFuckingSimpleGenericCallback<String> onFloorCreated);
    void upload(Building building, IFuckingSimpleCallback onDone);
    void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone);
    void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone);
    void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived);
    void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Pair<String,String>> callback);
    void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived);
    void downloadRadioMapTileAsync(String floorId, WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived);
}


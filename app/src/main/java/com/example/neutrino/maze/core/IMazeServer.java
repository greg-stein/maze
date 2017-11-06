package com.example.neutrino.maze.core;


import android.support.v4.util.Pair;

import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;
import com.example.neutrino.maze.core.WiFiLocator.WiFiFingerprint;

import java.util.List;

/**
 * Created by Greg Stein on 9/25/2017.
 */

public interface IMazeServer {
    // Searches for best matching building based on fingerprint. The returned structure
    // contains also list of floors and updated current floor id
    Building findCurrentBuilding(WiFiLocator.WiFiFingerprint fingerprint);
    void findCurrentBuildingAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Building> onDone);
    // Returns floor id
    String createFloor();

    Building createBuilding(String buildingName, String address, String type);

    // Returns building id
    String createBuilding(Building building);

    String downloadFloorPlanJson(String floorId); // doesn't include tags & teleports

    // gets estimated map tile containing given fingerprint
    List<Fingerprint> downloadRadioMapTile(String floorId, WiFiLocator.WiFiFingerprint fingerprint);

    void uploadFloorPlan(String floorId, FloorPlan floorPlan);

    // This method is used to update radio map on server. Server decides whether given data
    // qualifies as good update and then alters its maps
    void uploadFingerprints(List<Fingerprint> fingerprints);

    void updateBuilding(Building building);

    List<Building> findSimilarBuildings(String pattern);

    // Async section
    void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived);
    void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Pair<String,String>> callback);
    void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived);
    void downloadRadioMapTileAsync(String floorId, WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<List<WiFiFingerprint>> onRadioTileReceived);
}


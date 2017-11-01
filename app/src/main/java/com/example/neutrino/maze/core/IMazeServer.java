package com.example.neutrino.maze.core;

import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;

import java.util.List;

/**
 * Created by Greg Stein on 9/25/2017.
 */

public interface IMazeServer {
    // Searches for best matching building based on fingerprint. The returned structure
    // contains also list of floors and updated current floor id
    Building findCurrentBuilding(WiFiLocator.WiFiFingerprint fingerprint);

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
}

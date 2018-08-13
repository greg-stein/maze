package world.maze.core;


import android.support.v4.util.Pair;

import world.maze.floorplan.Building;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;
import world.maze.core.WiFiLocator.WiFiFingerprint;

import java.util.List;

/**
 * Created by Greg Stein on 9/25/2017.
 */

public interface IMazeServer {
    void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback);
    void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated);
    void createBuildingAsync(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback);
    void createFloorAsync(IFuckingSimpleGenericCallback<String> onFloorCreated);
    void upload(Building building, IFuckingSimpleCallback onDone);
    void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone);
    void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone);
    void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived);
    void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Pair<String,String>> callback);
    void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived);
    void downloadRadioMapTileAsync(String floorId, WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived);
}


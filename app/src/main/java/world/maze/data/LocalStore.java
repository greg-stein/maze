package world.maze.data;

import android.support.v4.util.Pair;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Building;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;

import java.util.List;

/**
 * Created by Greg Stein on 8/11/2018.
 */

public class LocalStore implements IDataKeep {
    @Override
    public void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {

    }

    @Override
    public void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated) {

    }

    @Override
    public void createBuildingAsync(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback) {

    }

    @Override
    public void createFloorAsync(IFuckingSimpleGenericCallback<String> onFloorCreated) {

    }

    @Override
    public void upload(Building building, IFuckingSimpleCallback onDone) {

    }

    @Override
    public void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone) {

    }

    @Override
    public void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone) {

    }

    @Override
    public void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived) {

    }

    @Override
    public void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Pair<String, String>> callback) {

    }

    @Override
    public void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived) {

    }

    @Override
    public void downloadRadioMapTileAsync(String floorId, WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived) {

    }
}

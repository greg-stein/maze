package world.maze.data;

import android.support.v4.util.Pair;

import java.util.List;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Building;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.IFuckingSimpleGenericCallback;

/**
 * Created by Greg Stein on 8/22/2018.
 *
 * Represents read-only data provider (based on raw resources/assets)
 */

public interface IDataProvider {
    void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback);

    void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived);

    void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Pair<String, String>> callback);

    void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived);

    void downloadRadioMapTileAsync(String floorId, WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived);
}

package world.maze.data;

import android.content.Context;
import android.support.v4.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Building;
import world.maze.floorplan.Floor;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.CommonHelper;
import world.maze.util.IFuckingSimpleGenericCallback;
import world.maze.util.JsonSerializer;

import static world.maze.data.DataAggregator.JSON_EXT;

/**
 * Created by Greg Stein on 1/7/2019.
 */

public abstract class LocalStorageDataProvider implements IDataProvider {
    protected Set<String> mFloorplanIds;
    protected Set<String> mRadioMapIds;
    protected Set<String> mBuildingIds;
    protected Context mContext;


    protected abstract String load(Context context, String absolutePath, String s);
    protected abstract Building loadBuilding(String buildingId);
    protected abstract RadioMapFragment loadRadioMapFragment(String floorId, WiFiLocator.WiFiFingerprint fingerprint);
    protected abstract FloorPlan loadFloorPlan(String floorId);

    @Override
    public void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {

    }

    @Override
    public void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived) {
        Building building = loadBuilding(buildingId);
        onBuildingReceived.onNotify(building);
    }

    // Achtung! This method assumes that there are little buildings stored locally. The implementation
    // is really slow!
    @Override
    public void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Pair<String, String>> callback) {
        Building mostSuitableBuilding = null;
        Floor mostSuitableFloor = null;

        Set<String> fingerprintMacs = new HashSet<>(fingerprint.keySet()); // MAC addresses from fingerprint
        int maxIntersectionSize = 0;

        for (String buildingId : mBuildingIds) {
            Building building = loadBuilding(buildingId);
            for (Floor floor : building.getFloors()) {
                // TODO: This is naive implementation. Use maximum WiFi level instead
                int intersectionSize = CommonHelper.intersectionSize(fingerprintMacs, floor.getMacs());
                if (intersectionSize > maxIntersectionSize) {
                    maxIntersectionSize = intersectionSize;
                    mostSuitableFloor = floor;
                    mostSuitableBuilding = building;
                }
            }
        }

        if (mostSuitableBuilding != null && mostSuitableFloor != null) {
            callback.onNotify(new Pair<>(mostSuitableBuilding.getId(), mostSuitableFloor.getId()));
            return;
        }
    }

    @Override
    public void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived) {
        FloorPlan floorPlan = loadFloorPlan(floorId);
        onFloorPlanReceived.onNotify(floorPlan /*new FloorPlan(floorId)*/);
    }

    @Override
    public void downloadRadioMapTileAsync(String floorId, WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived) {
        RadioMapFragment radioMapFragment = loadRadioMapFragment(floorId, fingerprint);
        onRadioTileReceived.onNotify(radioMapFragment);
    }

    @Override
    public Collection<String> getBuildingIds() {
        if (null == mBuildingIds) {
            return new ArrayList<>(); // empty collection
        }
        return Collections.unmodifiableSet(mBuildingIds);
    }

    @Override
    public boolean hasId(String id) {
        // REMARK: general id hold by this store. mFloorplanIds and mRadioMapIds has same ids.
        return mBuildingIds.contains(id) || mFloorplanIds.contains(id) || mRadioMapIds.contains(id);
    }
}

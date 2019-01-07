package world.maze.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Building;
import world.maze.floorplan.Floor;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.CommonHelper;
import world.maze.util.IFuckingSimpleGenericCallback;
import world.maze.util.JsonSerializer;

/**
 * Created by Greg Stein on 8/22/2018.
 */

public class AssetsDataProvider implements IDataProvider {
    private Set<String> mFloorplanIds;
    private Set<String> mRadioMapIds;
    private Set<String> mBuildingIds;
    private Context mContext;
    private Map<String, Building> mBuildingsCache = new HashMap<>();

    public AssetsDataProvider(Context context) {
        mContext = context;
    }

    public void init() throws IOException {
        final AssetManager assets = mContext.getAssets();
        mFloorplanIds = clearExtention(assets.list(DataAggregator.FLORPLANS_SUBDIR));
        mRadioMapIds = clearExtention(assets.list(DataAggregator.RADIOMAPS_SUBDIR));
        mBuildingIds = clearExtention(assets.list(DataAggregator.BUILDINGS_SUBDIR));

        mBuildingsCache.clear();
        for (String buildingId: mBuildingIds) {
            InputStream iStream = assets.open(DataAggregator.BUILDINGS_SUBDIR + "/" + buildingId + DataAggregator.JSON_EXT);
            byte[] bytes = new byte[iStream.available()];
            iStream.read(bytes);
            String buildingJson = new String(bytes);
            Building building = JsonSerializer.deserialize(buildingJson, Building.class);
            mBuildingsCache.put(buildingId, building);
        }
    }

    private Set<String> clearExtention(String[] strings) {
        Set<String> ids = new HashSet<>();
        if (strings == null) return ids;

        for (int i = 0; i < strings.length; i++) {
            int extensionStartPos = strings[i].indexOf(DataAggregator.JSON_EXT);
            if (-1 == extensionStartPos) continue;
            strings[i] = strings[i].substring(0, extensionStartPos);
            ids.add(strings[i]);
        }

        return ids;
    }

    @NonNull
    public String load(Context context, String directory, String filename) {
        final AssetManager assets = context.getAssets();
        InputStream iStream;
        byte[] bytes = null;
        try {
            iStream = assets.open(directory + "/" + filename);
            bytes = new byte[0];
            bytes = new byte[iStream.available()];
            iStream.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(bytes);
    }

    private Building loadBuilding(String buildingId) {
        String buildingJson = load(mContext, DataAggregator.BUILDINGS_SUBDIR, buildingId + DataAggregator.JSON_EXT);
        return JsonSerializer.deserialize(buildingJson, Building.class);
    }

    private FloorPlan loadFloorPlan(String floorId) {
        String floorJson = load(mContext, DataAggregator.FLORPLANS_SUBDIR, floorId + DataAggregator.JSON_EXT);
        return JsonSerializer.deserialize(floorJson, FloorPlan.class);
    }

    private RadioMapFragment loadRadioMapFragment(String floorId, WiFiLocator.WiFiFingerprint fingerprint) {
        String radioMapJson = load(mContext, DataAggregator.RADIOMAPS_SUBDIR, floorId + DataAggregator.JSON_EXT);
        return JsonSerializer.deserialize(radioMapJson, RadioMapFragment.class);
    }

    @Override
    public void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {

    }

    @Override
    public void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived) {
        if (!mBuildingIds.contains(buildingId)) {
            onBuildingReceived.onNotify(null);
            return;
        }

        Building building = loadBuilding(buildingId);
        onBuildingReceived.onNotify(building);
    }

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
        onFloorPlanReceived.onNotify(floorPlan);
    }

    @Override
    public void downloadRadioMapTileAsync(String floorId, WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived) {
        RadioMapFragment radioMap = loadRadioMapFragment(floorId, fingerprint);
        onRadioTileReceived.onNotify(radioMap);
    }

    @Override
    public Iterable<String> getBuildingIds() {
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

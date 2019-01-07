package world.maze.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.v4.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Building;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.IFuckingSimpleGenericCallback;
import world.maze.util.JsonSerializer;

/**
 * Created by Greg Stein on 8/22/2018.
 */

public class AssetsDataProvider implements IDataProvider {
    private String[] mFloorplanIds;
    private String[] mRadioMapIds;
    private String[] mBuildingIds;
    private Context mContext;
    private Map<String, Building> mBuildingsCache = new HashMap<>();

    public AssetsDataProvider(Context context) {
        mContext = context;
    }

    public void init() throws IOException {
        final AssetManager assets = mContext.getAssets();
        mFloorplanIds = assets.list(DataAggregator.FLORPLANS_SUBDIR);
        clearExtention(mFloorplanIds);
        mRadioMapIds = assets.list(DataAggregator.RADIOMAPS_SUBDIR);
        clearExtention(mRadioMapIds);
        mBuildingIds = assets.list(DataAggregator.BUILDINGS_SUBDIR);
        clearExtention(mBuildingIds);

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

    private void clearExtention(String[] strings) {
        if (strings == null) return;

        for (int i = 0; i < strings.length; i++) {
            int extensionStartPos = strings[i].indexOf(DataAggregator.JSON_EXT);
            if (-1 == extensionStartPos) continue;
            strings[i] = strings[i].substring(0, extensionStartPos);
        }
    }

    @Override
    public void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {

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

    @Override
    public Iterable<String> getBuildingIds() {
        return Arrays.asList(mBuildingIds);
    }

    @Override
    public boolean hasId(String id) {
        return false; // TODO
    }
}

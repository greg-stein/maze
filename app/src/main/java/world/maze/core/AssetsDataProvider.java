package world.maze.core;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.v4.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import world.maze.floorplan.Building;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.IFuckingSimpleGenericCallback;
import world.maze.util.JsonSerializer;

/**
 * Created by Greg Stein on 8/22/2018.
 */

public class AssetsDataProvider implements IDataProvider {
    public static final String FLORPLANS_SUBDIR = "/floorplans";
    public static final String RADIOMAPS_SUBDIR = "/radiomaps";
    public static final String BUILDINGS_SUBDIR = "/buildings";
    public static final String JSON_EXT = ".json";
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
        mFloorplanIds = assets.list(FLORPLANS_SUBDIR);
        mRadioMapIds = assets.list(RADIOMAPS_SUBDIR);
        mBuildingIds = assets.list(BUILDINGS_SUBDIR);

        mBuildingsCache.clear();
        for (String buildingId: mBuildingIds) {
            InputStream iStream = assets.open(BUILDINGS_SUBDIR + "/" + buildingId + JSON_EXT);
            byte[] bytes = new byte[iStream.available()];
            iStream.read(bytes);
            String buildingJson = new String(bytes);
            Building building = JsonSerializer.deserialize(buildingJson, Building.class);
            mBuildingsCache.put(buildingId, building);
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
}

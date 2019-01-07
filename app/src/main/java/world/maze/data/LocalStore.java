package world.maze.data;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Building;
import world.maze.floorplan.Floor;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.CommonHelper;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;
import world.maze.util.JsonSerializer;

import static world.maze.data.DataAggregator.JSON_EXT;

/**
 * Created by Greg Stein on 8/11/2018.
 */

public class LocalStore implements IDataProvider, IDataKeeper {

    public static final String DATA_ROOT = "maze";

    private Set<String> mFloorplanIds;
    private Set<String> mRadioMapIds;
    private Set<String> mBuildingIds;

    private File mFloorplansDir;
    private File mRadiomapsDir;
    private File mBuildingsDir;

    private Context mContext;

    public void init(Context context) throws IOException {
        final String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
            final String dataRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + DATA_ROOT + "/";
            mFloorplansDir = new File(dataRoot + DataAggregator.FLORPLANS_SUBDIR);
            mFloorplanIds = listInitDirectory(mFloorplansDir);
            mRadiomapsDir = new File(dataRoot + DataAggregator.RADIOMAPS_SUBDIR);
            mRadioMapIds = listInitDirectory(mRadiomapsDir);
            mBuildingsDir = new File(dataRoot + DataAggregator.BUILDINGS_SUBDIR);
            mBuildingIds = listInitDirectory(mBuildingsDir);

            mContext = context;
        }
    }

    private static Set<String> listInitDirectory(File dir) throws IOException {
        Set<String> buildingIds = new HashSet<>();

        if (!dir.exists()) {
            if (!dir.mkdirs()) throw new IOException("Failed to create directory path.");
            return buildingIds;
        }

        String[] dirContents = dir.list();
        for (String fileName : dirContents) {
            // extract filename without extension
            buildingIds.add(fileName.substring(0, fileName.lastIndexOf(JSON_EXT)));
        }

        return buildingIds;
    }

    public static void save(Context context, String data, String directory, String filename) {
        try {
            File aFile = new File(directory, filename);
            FileWriter fileWriter = new FileWriter(aFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (IOException e) {
            Toast.makeText(context, "Error saving data locally.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @NonNull
    public String load(Context context, String directory, String filename) {
        String data = null;
        FileInputStream fis;

        try {
            File aFile = new File(directory, filename);
            int fileSize = (int)aFile.length();
            byte[] bytes = new byte[fileSize];
            fis = new FileInputStream(aFile);
            fis.read(bytes);
            data = new String(bytes);
            fis.close();
        } catch (IOException e) {
            Toast.makeText(context, "Error loading data from store.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return data;
    }

    private void save(Building building) {
        String json = JsonSerializer.serialize(building);
        save(mContext, json, mBuildingsDir.getAbsolutePath(), building.getId() + JSON_EXT);
        mBuildingIds.add(building.getId());
    }

    private void save(FloorPlan floorPlan) {
        String json = JsonSerializer.serialize(floorPlan);
        save(mContext, json, mFloorplansDir.getAbsolutePath(), floorPlan.getFloorId() + JSON_EXT);
        mFloorplanIds.add(floorPlan.getFloorId());
    }

    private void save(RadioMapFragment radioMapFragment) {
        String floorId = radioMapFragment.getFloorId();

        // Do we have fingerprints on this floor?
        if (mRadioMapIds.contains(floorId)) {
            // Yes => add new fingerprints to the old ones we already have
            String jsonString = load(mContext, mRadiomapsDir.getAbsolutePath(), floorId + JSON_EXT);
            RadioMapFragment existingFragment = JsonSerializer.deserialize(jsonString, RadioMapFragment.class);
            radioMapFragment.add(existingFragment.getFingerprints());
        } else {
            mRadioMapIds.add(floorId);
        }

        // Save new fragment
        String json = JsonSerializer.serialize(radioMapFragment);
        save(mContext, json, mRadiomapsDir.getAbsolutePath(), floorId + JSON_EXT);
    }

    private Building loadBuilding(String buildingId) {
        String jsonString = load(mContext, mBuildingsDir.getAbsolutePath(), buildingId + JSON_EXT);
        Building building = JsonSerializer.deserialize(jsonString, Building.class);
        return building;
    }

    private RadioMapFragment loadRadioMapFragment(String floorId, WiFiLocator.WiFiFingerprint fingerprint) {
        String jsonString = load(mContext, mRadiomapsDir.getAbsolutePath(), floorId + JSON_EXT);
        RadioMapFragment existingFragment = JsonSerializer.deserialize(jsonString, RadioMapFragment.class);
        return existingFragment;
    }

    private FloorPlan loadFloorPlan(String floorId) {
        String jsonString = load(mContext, mFloorplansDir.getAbsolutePath(), floorId + JSON_EXT);
        FloorPlan floorPlan = JsonSerializer.deserialize(jsonString, FloorPlan.class);
        return floorPlan;
    }

    @Override
    public void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {

    }

    @Override
    public void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated) {
        String buildingId = UUID.randomUUID().toString();
        Building building = new Building();
        building.setID(buildingId);
        save(building);
        onBuildingCreated.onNotify(buildingId);
    }

    @Override
    public void createBuildingAsync(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback) {
        String buildingId = UUID.randomUUID().toString();
        Building building = new Building(name, type, address, buildingId);
        save(building);
        buildingCreatedCallback.onNotify(building);
    }

    @Override
    public void createFloorAsync(String buildingId, IFuckingSimpleGenericCallback<String> onFloorCreated) {
        String floorId = UUID.randomUUID().toString();
        // Well, yes, this is tricky. This function is called when we are about to create a new floor
        // Hence at this point we also create FloorPlan structure and RadioMap for the floor. These
        // will be requested a bit later when FloorChanged event will be fired.
        save(new FloorPlan(floorId));
        save(new RadioMapFragment(null, floorId));

        onFloorCreated.onNotify(floorId);
    }

    @Override
    public void upload(Building building, IFuckingSimpleCallback onDone) {
        save(building);
        onDone.onNotified();
    }

    @Override
    public void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone) {
        save(floorPlan);
        onDone.onNotified();
    }

    @Override
    public void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone) {
        save(radioMap);
        onDone.onNotified();
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

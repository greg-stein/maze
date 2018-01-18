package com.example.neutrino.maze.core;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.util.Pair;

import com.example.neutrino.maze.R;
import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.Floor;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.PersistenceLayer;
import com.example.neutrino.maze.floorplan.RadioMapFragment;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.util.IFuckingSimpleCallback;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;
import com.example.neutrino.maze.util.JsonSerializer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Greg Stein on 9/30/2017.
 */

public class MazeServerMock implements IMazeServer {
    public static final String BUILDING_STORE = "building.wad";

    // These objects mimic objects came from DB
    private Building dbBuilding = null;
    private final Floor dbFloor1;
    private final Floor dbFloor2;
    private FloorPlan dbFloor1Plan = null; // floor plan for fl1
    private RadioMapFragment dbRadioMap1 = null;
    private FloorPlan dbFloor2Plan = null; // floor plan for fl2
    private RadioMapFragment dbRadioMap2 = null;

    // TODO:
        // 1. Serializer for single objects - DONE
        // 2. In debug serialize into JSON: Building, radio map, floor plan for two floors
        // 3. Create raw resources and store there all the data
        // 4. write in this class static initializer that reads all the data from resources
        // writing/saving data - ?? - next stage is working with internal storage

    private static IMazeServer instance = null;
    private static final Object mutex = new Object();
    public static IMazeServer getInstance(Context context) {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new MazeServerMock(context);
                }
            }
        }
        return instance;
    }

    private final Context mContext;

    private MazeServerMock(Context context) {
        this.mContext = context;
        dbBuilding = new Building("Haifa Mall", "Flieman str. Haifa", "Mall", "building_id");
        List<Floor> floors = new ArrayList<>();
        floors.add(dbFloor1 = new Floor("1", "floor_id_1"));
        floors.add(dbFloor2 = new Floor("2", "floor_id_2"));
        dbBuilding.setFloors(floors);

        dbFloor1Plan = FloorPlan.build(loadFromRes(R.raw.haifa_mall_detailed_tags));
        List<Fingerprint> fingerprints = (List<Fingerprint>)(List<?>)loadFromRes(R.raw.radio_map);
        List<Tag> tagsFloor1 = (List<Tag>)(List<?>)loadFromRes(R.raw.tags);
        dbRadioMap1 = new RadioMapFragment(fingerprints, dbFloor1.getId());
        dbFloor1.setTags(tagsFloor1);
        dbFloor1.setTeleports(dbFloor1Plan.getTeleportsOnFloor());
    }

    private List<Object> loadFromRes(int resFloorplan) {
        String jsonString = null;
        try {
            Resources res = mContext.getResources();
            InputStream in_s = res.openRawResource(resFloorplan);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            jsonString = new String(b);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Object> floorPlan = FloorPlanSerializer.deserializeFloorPlan(jsonString);
        return floorPlan;
    }

    private static volatile int buildingSequence = 0;
    private static volatile int floorSequence = 0;

    @Override
    public void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {
        List<Building> buildings = new ArrayList<>();

        buildings.add(new Building("Haifa Mall 1", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 2", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 3", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 4", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 5", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 6", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 7", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 8", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 9", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 10", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 11", "Flieman st. Haifa", "Mall", "1"));
        buildings.add(new Building("Haifa Mall 12", "Flieman st. Haifa", "Mall", "1"));

        buildingsAcquiredCallback.onNotify(buildings);
    }

    @Override
    public void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated) {
        onBuildingCreated.onNotify(Integer.toString(buildingSequence++));
    }

    @Override
    public void createFloorAsync(IFuckingSimpleGenericCallback<String> onFloorCreated) {
        onFloorCreated.onNotify(Integer.toString(floorSequence++));
    }

    @Override
    public void upload(Building building, IFuckingSimpleCallback onDone) {
        onDone.onNotified();
    }

    @Override
    public void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone) {
        onDone.onNotified();
    }

    @Override
    public void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone) {
        onDone.onNotified();
    }

    @Override
    public void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived) {
        if (buildingId.equals(dbBuilding.getId())) {
            onBuildingReceived.onNotify(dbBuilding);
        }
        // else JOPA
    }

    @Override
    public void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Pair<String, String>> callback) {
        if (fingerprint != null && !fingerprint.isEmpty()) {
            callback.onNotify(new Pair<>(dbBuilding.getId(), dbFloor1.getId()));
        }
        // else JOPA
    }

    @Override
    public void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived) {
        if (floorId.equals(dbFloor1.getId())) {
            onFloorPlanReceived.onNotify(dbFloor1Plan);
        }
        if (floorId.equals(dbFloor2.getId())) {
            onFloorPlanReceived.onNotify(dbFloor2Plan);
        }
        // else JOPA
    }

    @Override
    public void downloadRadioMapTileAsync(String floorId, WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived) {
        if (floorId.equals(dbFloor1.getId())) {
            onRadioTileReceived.onNotify(dbRadioMap1);
        }
    }

}

package world.maze.core;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.util.Pair;

import world.maze.R;
import world.maze.data.IDataKeeper;
import world.maze.data.IDataProvider;
import world.maze.floorplan.Building;
import world.maze.floorplan.Fingerprint;
import world.maze.floorplan.Floor;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.FloorPlanSerializer;
import world.maze.floorplan.RadioMapFragment;
import world.maze.floorplan.Tag;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Greg Stein on 9/30/2017.
 */

public class MazeServerMock implements IDataProvider, IDataKeeper {
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

    private static MazeServerMock instance = null;
    private static final Object mutex = new Object();
    public static MazeServerMock getInstance(Context context) {
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
        dbBuilding = new Building("Haifa Mall", "Mall", "Flieman str. Haifa", "building_id");
        List<Floor> floors = new ArrayList<>();
        floors.add(dbFloor1 = new Floor("1", "floor_id_1"));
        floors.add(dbFloor2 = new Floor("2", "floor_id_2"));
        dbBuilding.setFloors(floors);

        dbFloor1Plan = new FloorPlan(loadFromRes(R.raw.haifa_mall_detailed_tags));
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

        buildings.add(new Building("Haifa Mall 1", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 2", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 3", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 4", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 5", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 6", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 7", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 8", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 9", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 10", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 11", "Mall", "Flieman st. Haifa", "1"));
        buildings.add(new Building("Haifa Mall 12", "Mall", "Flieman st. Haifa", "1"));

        buildingsAcquiredCallback.onNotify(buildings);
    }

    @Override
    public void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated) {
        final String buildingId = Integer.toString(buildingSequence++);
        onBuildingCreated.onNotify(buildingId);
    }

    @Override
    public void createBuildingAsync(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback) {
        final String buildingId = Integer.toString(buildingSequence++);
        final Building building = new Building(name, type, address, buildingId);
        buildingCreatedCallback.onNotify(building);
    }

    @Override
    public void createFloorAsync(String buildingId, IFuckingSimpleGenericCallback<String> onFloorCreated) {
        onFloorCreated.onNotify(Integer.toString(floorSequence++));
    }

    @Override
    public void upload(Building building, IFuckingSimpleCallback onDone) {
        dbBuilding.setName(building.getName());
        dbBuilding.setType(building.getType());
        dbBuilding.setAddress(building.getAddress());
        dbBuilding.setFloors(building.getFloors());
        building.setDirty(false);
        onDone.onNotified();
    }

    @Override
    public void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone) {
        onDone.onNotified();
    }

    @Override
    public void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone) {
        final Set<Fingerprint> fingerprints = radioMap.getFingerprints();

        if (radioMap.getFloorId().equals(dbFloor1.getId())) {
            for (Fingerprint fingerprint : fingerprints) {
                dbRadioMap1.addFingerprint(fingerprint);
            }
        } else if (radioMap.getFloorId().equals(dbFloor2.getId())) {
            for (Fingerprint fingerprint : fingerprints) {
                dbRadioMap2.addFingerprint(fingerprint);
            }
        }

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
            // test only
//            callback.onNotify(new Pair<>("", ""));
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

    @Override
    public Iterable<String> getBuildingIds() {
        return null;
    }

    @Override
    public boolean hasId(String id) {
        return false;
    }

}

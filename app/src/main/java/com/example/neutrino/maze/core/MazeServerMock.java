package com.example.neutrino.maze.core;

import android.content.Context;
import android.content.res.Resources;

import com.example.neutrino.maze.R;
import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.Floor;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.PersistenceLayer;
import com.example.neutrino.maze.util.JsonSerializer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 9/30/2017.
 */

public class MazeServerMock implements IMazeServer {
    public static final String BUILDING_STORE = "building.wad";
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
    }

    private static volatile int buildingSequence = 0;
    private static volatile int floorSequence = 0;

    @Override
    public Building findCurrentBuilding(WiFiLocator.WiFiFingerprint fingerprint) {
        // Mock the mock :)
        Building haifaMall = new Building("Haifa Mall", "Flieman st. Haifa", "Mall", "охуенно уникальный стринг суканахуй");
        Floor floor1 = new Floor("1", "1234");
        List<Floor> floors = new ArrayList<>();
        floors.add(floor1);
        haifaMall.setFloors(floors);
        return haifaMall;

//        String json = PersistenceLayer.load(mContext, BUILDING_STORE);
//        return JsonSerializer.deserialize(json, Building.class);
    }

    @Override
    public String createFloor() {
        // TODO: under Building.current?
        return Integer.toString(floorSequence++);
    }

    @Override
    public String createBuilding(Building building) {
        return Integer.toString(buildingSequence++);
    }

    @Override
    public Building createBuilding(String buildingName, String address, String type) {
        Building building = new Building(buildingName, address, type, "unique ID :)");
        final String newId = createBuilding(building);
        building.setID(newId);
        building.setFloors(new ArrayList<Floor>());

        return building;
    }

    @Override
    public String downloadFloorPlanJson(String floorId) {
//                String jsonString = PersistenceLayer.loadFloorPlan();

        String jsonString = null;
        try {
            Resources res = mContext.getResources();
            InputStream in_s = res.openRawResource(R.raw.haifa_mall_detailed_tags);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            jsonString = new String(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonString;

//                if (MazeServer.connectionAvailable(getApplicationContext())) {
//                    MazeServer server = new MazeServer(getApplicationContext());
//                    server.downloadFloorPlan(new MazeServer.AsyncResponse() {
//                        @Override
//                        public void processFinish(String jsonString) {
//                            uiFloorPlanView.setFloorPlanAsJSon(jsonString);
//                        }
//                    });
//                } else {
//                    Toast.makeText(getApplicationContext(), "No Internet connection", Toast.LENGTH_SHORT).show();
//                }
    }

    @Override
    public List<Fingerprint> downloadRadioMapTile(String floorId, WiFiLocator.WiFiFingerprint fingerprint) {
        return null;
    }

    @Override
    public void uploadFloorPlan(String floorId, FloorPlan floorPlan) {

    }

    @Override
    public void uploadFingerprints(List<Fingerprint> fingerprints) {

    }

    @Override
    public void updateBuilding(Building building) {
        String json = JsonSerializer.serialize(building);
        PersistenceLayer.save(mContext, json, BUILDING_STORE);
    }

    @Override
    public List<Building> findSimilarBuildings(String pattern) {
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

        return buildings;
    }

}

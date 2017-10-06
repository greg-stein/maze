package com.example.neutrino.maze;

import android.content.Context;
import android.content.res.Resources;

import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 9/30/2017.
 */

public class MazeServerBase implements IMazeServer {

    private static IMazeServer instance = null;
    private static final Object mutex = new Object();
    public static IMazeServer getInstance(Context context) {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new MazeServerBase(context);
                }
            }
        }
        return instance;
    }

    private final Context mContext;

    private MazeServerBase(Context context) {
        this.mContext = context;
    }

    private static volatile int buildingSequence = 0;
    private static volatile int floorSequence = 0;

    @Override
    public Building findCurrentBuilding(WiFiLocator.WiFiFingerprint fingerprint) {
        return null;
    }

    @Override
    public String createFloor() {
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

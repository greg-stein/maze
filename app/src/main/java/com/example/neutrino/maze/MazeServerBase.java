package com.example.neutrino.maze;

import android.os.Build;

import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 9/30/2017.
 */

public class MazeServerBase implements IMazeServer {

    private static IMazeServer server = new MazeServerBase();
    public static IMazeServer getServer() {
        return server;
    }

    @Override
    public Building findCurrentBuilding(WiFiLocator.WiFiFingerprint fingerprint) {
        return null;
    }

    @Override
    public String createFloor() {
        return null;
    }

    @Override
    public String createBuilding(Building building) {
        return null;
    }

    @Override
    public FloorPlan downloadFloorPlan(String floorId) {
        return null;
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

    @Override
    public Building createBuilding(String buildingName, String address, String type) {
        return new Building(buildingName, address, type, "unique ID :)");
    }

}

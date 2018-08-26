package world.maze.data;


import world.maze.floorplan.Building;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;

/**
 * Created by Greg Stein on 9/25/2017.
 *
 * Represents read-write data store. IDataProvider is read-only.
 */

public interface IDataKeep extends IDataProvider {
    void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated);
    void createBuildingAsync(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback);
    void createFloorAsync(IFuckingSimpleGenericCallback<String> onFloorCreated);
    void upload(Building building, IFuckingSimpleCallback onDone);
    void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone);
    void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone);
}


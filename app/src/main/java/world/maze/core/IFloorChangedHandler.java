package world.maze.core;

import world.maze.floorplan.Floor;

/**
 * Created by Greg Stein on 10/6/2017.
 */

public interface IFloorChangedHandler {
    void onFloorChanged(Floor floor);
}

package com.example.neutrino.maze.core;

import com.example.neutrino.maze.floorplan.Floor;

/**
 * Created by Greg Stein on 10/6/2017.
 */

public interface IFloorChangedHandler {
    void onFloorChanged(Floor floor);
}

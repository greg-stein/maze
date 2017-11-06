package com.example.neutrino.maze.core;

import com.example.neutrino.maze.floorplan.FloorPlan;

/**
 * Created by Greg Stein on 10/31/2017.
 */

public interface IMainView {
    void init();

    void render(FloorPlan floorPlan);

}

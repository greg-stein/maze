package com.example.neutrino.maze.floorplan;

/**
 * Created by Greg Stein on 6/18/2017.
 */

public interface IMoveable {
    void handleChange(float x, float y);

    void setTapLocation(float x, float y);
}

package com.example.neutrino.maze.floorplan.transitions;

import android.graphics.PointF;

/**
 * Created by Greg Stein on 7/19/2017.
 */

public interface ITeleport {
    String getId();

    void setId(String id);

    PointF getLocation();

    void setLocation(PointF location);
}

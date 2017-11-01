package com.example.neutrino.maze.floorplan.transitions;

import android.graphics.PointF;

import com.example.neutrino.maze.core.WiFiLocator.WiFiFingerprint;
import com.example.neutrino.maze.floorplan.Floor;

/**
 * Created by Greg Stein on 7/19/2017.
 */

public interface ITeleport {
    String getId();

    void setId(String id);

    PointF getLocation();

    void setLocation(PointF location);

    WiFiFingerprint getFingerprint();

    void setFingerprint(WiFiFingerprint fingerprint);

    Floor getFloor();

    void setFloor(Floor floor);
}

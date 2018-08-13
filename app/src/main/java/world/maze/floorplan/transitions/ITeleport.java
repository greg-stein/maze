package world.maze.floorplan.transitions;

import android.graphics.PointF;

import world.maze.core.WiFiLocator.WiFiFingerprint;
import world.maze.floorplan.Floor;

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

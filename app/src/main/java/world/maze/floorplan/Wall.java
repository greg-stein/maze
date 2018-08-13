package world.maze.floorplan;

import android.graphics.PointF;

import world.maze.AppSettings;

/**
 * Created by Greg Stein on 5/14/2017.
 */
// Fake class
public class Wall extends ThickLineSegment {
    public Wall() {
        super();
        setColor(AppSettings.wallColor);
    }

    public Wall(float x1, float y1, float x2, float y2)
    {
        super(x1, y1, x2, y2);
        setColor(AppSettings.wallColor);
    }

    public Wall(float x1, float y1, float x2, float y2, float thickness) {
        super(x1, y1, x2, y2, thickness);
        setColor(AppSettings.wallColor);
    }

    public Wall(PointF start, PointF end) {
        super(start, end);
        setColor(AppSettings.wallColor);
    }

    public Wall(PointF start, PointF end, float thickness) {
        super(start, end, thickness);
        setColor(AppSettings.wallColor);
    }
}

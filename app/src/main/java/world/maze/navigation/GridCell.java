package world.maze.navigation;

import android.graphics.PointF;
import android.graphics.RectF;

import world.maze.floorplan.Wall;

import java.util.HashSet;

/**
 * Created by Greg Stein on 5/9/2017.
 */
public class GridCell {
    public final HashSet<PointF> points = new HashSet<>();
    public final HashSet<Wall> obstacles = new HashSet<>();
    public final RectF boundingBox;

    public GridCell(RectF boundingBox) {
        this.boundingBox = boundingBox;
    }
}

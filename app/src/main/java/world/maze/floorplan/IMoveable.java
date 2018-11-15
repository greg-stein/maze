package world.maze.floorplan;

/**
 * Created by Greg Stein on 6/18/2017.
 */

public interface IMoveable {
    void handleMove(float x, float y);

    void setTapLocation(float x, float y);

    boolean hasPoint(float x, float y);

    void handleMoveStart();

    void handleMoveEnd();
}
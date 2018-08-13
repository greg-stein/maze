package world.maze.core;

import android.graphics.PointF;

/**
 * Created by Greg Stein on 11/8/2017.
 */

public interface IMazePresenter {
    void onCreate();

    void onResume();

    void onPause();

    void onDestroy();

    void setMapNorth(float mapNorth);

    void setLocationByUser(PointF location);
}

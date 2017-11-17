package com.example.neutrino.maze.core;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Tag;

import java.util.List;

/**
 * Created by Greg Stein on 10/31/2017.
 */

public interface IMainView {
    void init();

    void render(FloorPlan floorPlan);

    void setTags(List<Tag> tags);

    void updateLocation(PointF location);

    void setMapRotation(double degree);

    enum UiMode { MAP_VIEW_MODE, MAP_EDIT_MODE}

    UiMode getUiMode();

    interface IUiModeChangedListener {
        void onUiModeChanged(UiMode newMode);
    }

    void setUiModeChangedListener(IUiModeChangedListener listener);
}

package com.example.neutrino.maze.core;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;

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

    void setUiModeChangedListener(IFuckingSimpleGenericCallback<UiMode> listener);

    void setMapperEnabledChangedListener(IFuckingSimpleGenericCallback<Boolean> listener);

    void renderFingeprint(Fingerprint fingerprint);
}

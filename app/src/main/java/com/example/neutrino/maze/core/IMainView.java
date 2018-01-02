package com.example.neutrino.maze.core;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.rendering.ElementsRenderGroup;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;

import java.util.List;

/**
 * Created by Greg Stein on 10/31/2017.
 */

public interface IMainView {
    void init();

    ElementsRenderGroup render(List<IFloorPlanPrimitive> elements);

    void centerMapView(PointF point);

    void setTags(List<Tag> tags);

    void updateLocation(PointF location);

    void setMapRotation(double degree);

    UiMode getUiMode();

    void setUiModeChangedListener(IFuckingSimpleGenericCallback<UiMode> listener);

    void setMapperEnabledChangedListener(IFuckingSimpleGenericCallback<Boolean> listener);

    void renderFingeprint(Fingerprint fingerprint);

    void setElementFactory(IElementFactory factory);

    enum UiMode { MAP_VIEW_MODE, MAP_EDIT_MODE}

    enum MapOperation {
        MOVE, ADD, REMOVE, SET_LOCATION
    }

    enum MapOperand {
        WALL, SHORT_WALL, BOUNDARIES, TELEPORT, LOCATION_TAG
    }

    interface IElementFactory {
        IFloorPlanPrimitive createElement(MapOperand elementType, PointF dragStart);
    }
}

package com.example.neutrino.maze.core;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.IMoveable;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.rendering.ElementsRenderGroup;
import com.example.neutrino.maze.rendering.TextRenderGroup;
import com.example.neutrino.maze.util.IFuckingSimpleCallback;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;

import java.util.List;

/**
 * Created by Greg Stein on 10/31/2017.
 */

public interface IMainView {
    void init();

    ElementsRenderGroup createElementsRenderGroup(List<? extends IFloorPlanPrimitive> elements);

    TextRenderGroup createTextRenderGroup(List<? extends Tag> tags);

    void centerMapView(PointF point);

    void setTags(List<Tag> tags);

    void updateLocation(PointF location);

    void setMapRotation(double degree);

    UiMode getUiMode();

    void askUserToCreateBuilding(IFuckingSimpleGenericCallback<Boolean> userAnswerHandler);

    void setUiModeChangedListener(IFuckingSimpleGenericCallback<UiMode> listener);

    void setMapperEnabledChangedListener(IFuckingSimpleGenericCallback<Boolean> listener);

    void renderFingeprint(Fingerprint fingerprint);

    void setElementFactory(IElementFactory factory);

    void setBuildingIdProvider(IAsyncIdProvider buildingIdProvider);

    void setFloorIdProvider(IAsyncIdProvider floorIdProvider);

    void setSimilarBuildingsFinder(IAsyncSimilarBuildingsFinder buildingsFinder);

    void setBuildingCreator(IAsyncBuildingCreator iAsyncBuildingCreator);

    void setBuildingUpdater(IFuckingSimpleGenericCallback<Building> buildingUpdater);

    void showBuildingEditDialog();

    void setFloorChangedHandler(IFloorChangedHandler floorChangedHandler);

    void setOnLocateMeEnabledChangedListener(IFuckingSimpleGenericCallback<Boolean> listener);

    void setUploadButtonVisibility(boolean visible);

    void setUploadButtonClickListener(IFuckingSimpleCallback listener);

    enum UiMode { MAP_VIEW_MODE, MAP_EDIT_MODE}

    enum MapOperation {
        MOVE, ADD, REMOVE, SET_LOCATION
    }

    enum MapOperand {
        WALL, SHORT_WALL, BOUNDARIES, TELEPORT, LOCATION_TAG
    }

    interface IElementFactory {
        IMoveable createElement(MapOperand elementType, PointF location, Object... params);
    }

    interface IRenderGroupChangedListener {
        void onElementAdd(IMoveable element);
        void onElementChange(IMoveable element);
        void onElementRemoved(IMoveable element);
    }

    interface IAsyncIdProvider {
        void generateId(IFuckingSimpleGenericCallback<String> idGeneratedCallback);
    }

    interface IAsyncSimilarBuildingsFinder {
        void findBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback);
    }

    interface IAsyncBuildingCreator {
        void createBuilding(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback);
    }
}

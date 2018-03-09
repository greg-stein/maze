package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;

import com.example.neutrino.maze.floorplan.transitions.ITeleport;
import com.example.neutrino.maze.navigation.PathFinderBase;
import com.example.neutrino.maze.util.CommonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Greg Stein on 4/3/2017.
 */
public class FloorPlan {
    private List<IFloorPlanPrimitive> mSketch;
    private List<ITeleport> mTeleports; // TODO: remove
    private boolean mIsSketchDirty = false;

    public static FloorPlan build(List<Object> entities) {
        FloorPlan floorPlan = new FloorPlan();
        floorPlan.mTeleports = CommonHelper.extractObjects(ITeleport.class, entities);
//        List<Wall> walls = FloorplanVectorizer.connect(CommonHelper.extractObjects(Wall.class, entities));

        // Remove location marks from floorplan
        CommonHelper.extractObjects(LocationMark.class, entities);
        // Achtung! Synchronized!
        floorPlan.mSketch = Collections.synchronizedList(
                CommonHelper.extractObjects(IFloorPlanPrimitive.class, entities));
//        floorPlan.mSketch.addAll(walls);

        return floorPlan;
    }

    public static FloorPlan build() {
        FloorPlan floorPlan = new FloorPlan();
        floorPlan.mSketch = Collections.synchronizedList(new ArrayList<IFloorPlanPrimitive>());

        return floorPlan;
    }

    public List<Object> disassemble() {

        int entitiesNum =
                ((mSketch != null) ? mSketch.size() : 0);

        List<Object> result = new ArrayList<>(entitiesNum);

        if (mSketch != null) result.addAll(mSketch);

        return result;
    }

    public RectF getBoundaries() {
        if (mSketch == null || mSketch.size() == 0) return null;

        RectF boundaries = mSketch.get(0).getBoundingBox();

        for (IFloorPlanPrimitive primitive : mSketch) {
            boundaries.union(primitive.getBoundingBox());
        }

        return boundaries;
    }

    public PointF getCenter() {
        final RectF boundaries = getBoundaries();
        PointF floorPlanCenter = new PointF(boundaries.centerX(), boundaries.centerY());

        return floorPlanCenter;
    }

    // Returns unmodifiable list. To change it use [add/remove]Element()
    public List<IFloorPlanPrimitive> getSketch() {
        return mSketch;
    }

    public void addElement(IFloorPlanPrimitive primitive) {
        mSketch.add(primitive);
        setSketchDirty(true);
    }

    public void removeElement(IFloorPlanPrimitive primitive) {
        mSketch.remove(primitive);
        setSketchDirty(true);
    }

    public void setSketch(List<IFloorPlanPrimitive> sketch) {
        this.mSketch = Collections.synchronizedList(sketch);
    }

    // Clears any source data
    public void clear() {
        this.mSketch.clear();
    }

    public List<ITeleport> getTeleportsOnFloor() {
        return mTeleports;
    }

    public List<ITeleport> getTeleportsById(String id) {
        return null;
    }

    public boolean isSketchDirty() {
        return mIsSketchDirty;
    }

    public void setSketchDirty(boolean sketchDirty) {
        mIsSketchDirty = sketchDirty;
    }
}

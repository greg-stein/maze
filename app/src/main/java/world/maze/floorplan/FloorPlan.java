package world.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;

import world.maze.floorplan.transitions.Teleport;
import world.maze.util.CommonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Greg Stein on 4/3/2017.
 */
public class FloorPlan {
    private List<IFloorPlanPrimitive> mSketch;
    private transient List<Teleport> mTeleports; // TODO: remove
    private transient boolean mIsSketchDirty = false;
    private String mFloorId;

    public FloorPlan(String floorId) {
        this.mFloorId = floorId;
        this.mSketch = new ArrayList<>();
    }

    public FloorPlan() {

    }

    public FloorPlan(List<Object> entities) {
        CommonHelper.extractObjects(LocationMark.class, entities);
        // Achtung! Synchronized!
        mSketch = Collections.synchronizedList(
                CommonHelper.extractObjects(IFloorPlanPrimitive.class, entities));
    }

    public static FloorPlan build() {
        FloorPlan floorPlan = new FloorPlan();
        floorPlan.mSketch = Collections.synchronizedList(new ArrayList<IFloorPlanPrimitive>());

        return floorPlan;
    }

    public String getFloorId() {
        return mFloorId;
    }

    public void setFloorId(String floorId) {
        mFloorId = floorId;
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

        if (null == boundaries) return new PointF();
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

    public List<Teleport> getTeleportsOnFloor() {
        return mTeleports;
    }

    public List<Teleport> getTeleportsById(String id) {
        return null;
    }

    public boolean isSketchDirty() {
        return mIsSketchDirty;
    }

    public void setSketchDirty(boolean sketchDirty) {
        mIsSketchDirty = sketchDirty;
    }
}

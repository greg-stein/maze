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
    public static final Object mTagsListLocker = new Object(); // TODO: remove
    private List<IFloorPlanPrimitive> mSketch;
    private List<Tag> mTags; // TODO: remove
    private FloorPlanDescriptor mDescriptor; // TODO: remove
    private PathFinderBase mPathFinder; // TODO: remove
    private List<ITeleport> mTeleports; // TODO: remove
    private boolean mIsSketchDirty = false;

    public static FloorPlan build(List<Object> entities) {
        FloorPlan floorPlan = new FloorPlan();
        floorPlan.mTeleports = CommonHelper.extractObjects(ITeleport.class, entities);
        floorPlan.mTags = CommonHelper.extractObjects(Tag.class, entities);
//        List<Wall> walls = FloorplanVectorizer.connect(CommonHelper.extractObjects(Wall.class, entities));

        // Remove location marks from floorplan
        CommonHelper.extractObjects(LocationMark.class, entities);
        // Achtung! Synchronized!
        floorPlan.mSketch = Collections.synchronizedList(
                CommonHelper.extractObjects(IFloorPlanPrimitive.class, entities));
//        floorPlan.mSketch.addAll(walls);

        final List<FloorPlanDescriptor> floorPlanDescriptors = CommonHelper.extractObjects(FloorPlanDescriptor.class, entities);
        if (floorPlanDescriptors != null && floorPlanDescriptors.size() > 0) {
            floorPlan.mDescriptor = floorPlanDescriptors.get(0);
        }

        return floorPlan;
    }

    public static FloorPlan build() {
        FloorPlan floorPlan = new FloorPlan();
        floorPlan.mTags = new ArrayList<>();
        floorPlan.mSketch = Collections.synchronizedList(new ArrayList<IFloorPlanPrimitive>());
        floorPlan.mDescriptor = null;

        return floorPlan;
    }

    public void setPathFinder(PathFinderBase pathFinder) {
        this.mPathFinder = pathFinder;
    }

    public PathFinderBase getPathFinder() {
        return mPathFinder;
    }

    public List<Object> disassemble() {

        int entitiesNum =
                ((mSketch != null) ? mSketch.size() : 0) +
                ((mTags != null) ? mTags.size() : 0) +
                ((mDescriptor != null)? 1 : 0);

        List<Object> result = new ArrayList<>(entitiesNum);

        if (mSketch != null) result.addAll(mSketch);
        if (mTags != null)result.addAll(mTags);
        if (mDescriptor != null)result.add(mDescriptor);

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
    }

    public void removeElement(IFloorPlanPrimitive primitive) {
        mSketch.remove(primitive);
    }

    public void setSketch(List<IFloorPlanPrimitive> sketch) {
        this.mSketch = Collections.synchronizedList(sketch);
    }

    public List<Tag> getTags() {
        return mTags;
    }

    public void setTags(List<Tag> tags) {
        this.mTags = tags;
    }

    public FloorPlanDescriptor getDescriptor() {
        return mDescriptor;
    }

    public void setDescriptor(FloorPlanDescriptor descriptor) {
        this.mDescriptor = descriptor;
    }

    // Clears any source data
    public void clear() {
        this.mSketch.clear();
        synchronized (FloorPlan.mTagsListLocker) {
            this.mTags.clear();
        }
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

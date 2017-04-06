package com.example.neutrino.maze.floorplan;

import com.example.neutrino.maze.CommonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Greg Stein on 4/3/2017.
 */

public class FloorPlan {
    private List<IFloorPlanPrimitive> mSketch;
    private List<Fingerprint> mFingerprints;
    private List<Tag> mTags;
    private FloorPlanDescriptor mDescriptor;

    public static FloorPlan build(List<Object> entities) {
        FloorPlan floorPlan = new FloorPlan();
        floorPlan.mFingerprints = CommonHelper.extractObjects(Fingerprint.class, entities);
        floorPlan.mTags = CommonHelper.extractObjects(Tag.class, entities);
        floorPlan.mSketch = CommonHelper.extractObjects(IFloorPlanPrimitive.class, entities);

        final List<FloorPlanDescriptor> floorPlanDescriptors = CommonHelper.extractObjects(FloorPlanDescriptor.class, entities);
        if (floorPlanDescriptors != null && floorPlanDescriptors.size() > 0) {
            floorPlan.mDescriptor = floorPlanDescriptors.get(0);
        }

        return floorPlan;
    }

    public List<Object> disassemble() {
        int entitiesNum =
                ((mSketch != null) ? mSketch.size() : 0) +
                ((mFingerprints != null) ? mFingerprints.size() : 0) +
                ((mTags != null) ? mTags.size() : 0) +
                ((mDescriptor != null)? 1 : 0);

        List<Object> result = new ArrayList<>(entitiesNum);

        result.addAll(mSketch);
        result.addAll(mFingerprints);
        result.addAll(mTags);
        result.add(mDescriptor);

        return result;
    }

    public List<IFloorPlanPrimitive> getSketch() {
        return mSketch;
    }

    public void setketch(List<IFloorPlanPrimitive> sketch) {
        this.mSketch = sketch;
    }

    public List<Fingerprint> getFingerprints() {
        return mFingerprints;
    }

    public void setFingerprints(List<Fingerprint> fingerprints) {
        this.mFingerprints = fingerprints;
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
}

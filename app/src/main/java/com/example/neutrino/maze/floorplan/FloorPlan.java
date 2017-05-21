package com.example.neutrino.maze.floorplan;

import android.annotation.TargetApi;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.CommonHelper;
import com.example.neutrino.maze.navigation.FingerprintsPathFinder;
import com.example.neutrino.maze.navigation.GridPathFinder;
import com.example.neutrino.maze.navigation.PathFinderBase;
import com.example.neutrino.maze.vectorization.FloorplanVectorizer;

import org.simmetrics.StringMetric;
import org.simmetrics.metrics.CosineSimilarity;
import org.simmetrics.metrics.StringMetrics;
import org.simmetrics.simplifiers.Simplifiers;
import org.simmetrics.tokenizers.Tokenizers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.simmetrics.builders.StringMetricBuilder.with;

/**
 * Created by Greg Stein on 4/3/2017.
 */
public class FloorPlan {
    public static final Object mTagsListLocker = new Object();
    private List<IFloorPlanPrimitive> mSketch;
    private List<Fingerprint> mFingerprints;
    private List<Tag> mTags;
    private FloorPlanDescriptor mDescriptor;
    private PathFinderBase mPathFinder;

    public static FloorPlan build(List<Object> entities) {
        FloorPlan floorPlan = new FloorPlan();
        floorPlan.mFingerprints = CommonHelper.extractObjects(Fingerprint.class, entities);
        floorPlan.mTags = CommonHelper.extractObjects(Tag.class, entities);
//        List<Wall> walls = FloorplanVectorizer.connect(CommonHelper.extractObjects(Wall.class, entities));

        // Remove location marks from floorplan
        CommonHelper.extractObjects(LocationMark.class, entities);
        floorPlan.mSketch = CommonHelper.extractObjects(IFloorPlanPrimitive.class, entities);
//        floorPlan.mSketch.addAll(walls);

        final List<FloorPlanDescriptor> floorPlanDescriptors = CommonHelper.extractObjects(FloorPlanDescriptor.class, entities);
        if (floorPlanDescriptors != null && floorPlanDescriptors.size() > 0) {
            floorPlan.mDescriptor = floorPlanDescriptors.get(0);
        }

        if (AppSettings.inDebug && floorPlan.mFingerprints != null) {
            floorPlan.mSketch.addAll(floorPlan.mFingerprints);
        }

        if (floorPlan.mSketch.size() > 0) {
            floorPlan.mPathFinder = new GridPathFinder(floorPlan);
        } else {
            floorPlan.mPathFinder = new FingerprintsPathFinder(floorPlan);
        }
        floorPlan.mPathFinder.init();

        return floorPlan;
    }

    public static FloorPlan build() {
        FloorPlan floorPlan = new FloorPlan();
        floorPlan.mFingerprints = new ArrayList<>();
        floorPlan.mTags = new ArrayList<>();
        floorPlan.mSketch = new ArrayList<>();
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
        if (AppSettings.inDebug) {
            if (mSketch != null && mFingerprints != null) mSketch.removeAll(mFingerprints);
        }

        int entitiesNum =
                ((mSketch != null) ? mSketch.size() : 0) +
                ((mFingerprints != null) ? mFingerprints.size() : 0) +
                ((mTags != null) ? mTags.size() : 0) +
                ((mDescriptor != null)? 1 : 0);

        List<Object> result = new ArrayList<>(entitiesNum);

        if (mSketch != null) result.addAll(mSketch);
        if (mFingerprints != null)result.addAll(mFingerprints);
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

    public List<IFloorPlanPrimitive> getSketch() {
        return mSketch;
    }

    public void setSketch(List<IFloorPlanPrimitive> sketch) {
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

    private static class TagComparator implements Comparator<Tag> {
        private String sample;

        public TagComparator(String sample) {
            this.sample = sample;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public int compare(Tag t1, Tag t2) {
            StringMetric metric = StringMetrics.levenshtein();
//            StringMetric metric =
//            with(new CosineSimilarity<String>())
//                    .simplify(Simplifiers.toLowerCase(Locale.ENGLISH))
//                    .tokenize(Tokenizers.whitespace())
//                    .build();

            float t1Similarity = metric.compare(this.sample, t1.getLabel());
            float t2Similarity = metric.compare(this.sample, t2.getLabel());
            int compare = Float.compare(t1Similarity, t2Similarity);
            // Make compare method consistent with equals()
            if (compare == 0 && !t1.equals(t2)) {
                PointF t1Location = t1.getLocation();
                PointF t2Location = t2.getLocation();
                return Integer.compare(Float.compare(t1Location.x, t2Location.x), Float.compare(t1Location.y, t2Location.y));
            }
            return compare;
        }
    }

    public List<Tag> searchMostSimilarTags(String sample, int maxResults) {
        TagComparator comparator = new TagComparator(sample);
        synchronized (mTagsListLocker) {
            Collections.sort(mTags, comparator);
//        Collections.reverse(mTags); // slower
            List<Tag> tail = mTags.subList(Math.max(mTags.size() - maxResults, 0), mTags.size());
            Collections.reverse(tail);
            return tail;
        }
    }
}

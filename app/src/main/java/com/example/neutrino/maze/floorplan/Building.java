package com.example.neutrino.maze.floorplan;

import android.annotation.TargetApi;
import android.graphics.PointF;
import android.os.Build;

import com.example.neutrino.maze.floorplan.transitions.ITeleport;

import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Greg Stein on 7/7/2017.
 */

public class Building {
    public static Building current = null;

    private String mName;
    private String mAddress;
    private String mType;
    private String mID;
    private List<Floor> mFloors;
    private Set<String> mAccessPoints; // TODO: HashSet<Long>

    private transient Floor mCurrentFloor;
    private transient Map<String, List<ITeleport>> mTeleportsById = new HashMap<>();
    private transient boolean mDirty;

    public Building() {
    }

    public Building(String name, String address, String type, String id) {
        this.mName = name;
        this.mAddress = address;
        this.mType = type;
        this.mID = id;
        mAccessPoints = new HashSet<>();
        initTeleportsMap();
    }

    public static boolean isFloorDefined() {
        return Building.current != null && Building.current.mCurrentFloor != null;
    }

    private void initTeleportsMap() {
        if (null == mFloors) return;

        for (Floor floor : mFloors) {
            final List<ITeleport> floorTeleports = floor.getTeleports();
            for (ITeleport teleport : floorTeleports) {
                List<ITeleport> sameIdTeleports;
                final String teleportId = teleport.getId();

                if (mTeleportsById.containsKey(teleportId)) {
                    sameIdTeleports = mTeleportsById.get(teleportId);
                } else {
                    sameIdTeleports = new ArrayList<>();
                    mTeleportsById.put(teleportId, sameIdTeleports);
                }
                sameIdTeleports.add(teleport);
            }
        }
    }

    public List<ITeleport> getTeleportsById(String teleportId) {
        return mTeleportsById.get(teleportId);
    }

    public List<Tag> searchMostSimilarTags(String sample, int maxResults) {
        List<Tag> tags = new ArrayList<>();
        for (Floor floor : mFloors) {
            final List<Tag> floorTags = floor.getTags();
            if (floorTags != null) {
                tags.addAll(floorTags);
            }
        }

        // No search string? return as is.
        if (sample == null || sample.isEmpty()) {
            return tags;
        }

        TagComparator comparator = new TagComparator(sample);
        Collections.sort(tags, comparator);
//        Collections.reverse(tags); // slower
        List<Tag> tail = tags.subList(Math.max(tags.size() - maxResults, 0), tags.size());
        Collections.reverse(tail);
        return tail;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        this.mAddress = address;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String getId() {
        return mID;
    }

    public void setID(String id) {
        this.mID = id;
    }

    public List<Floor> getFloors() {
        return mFloors;
    }

    public void setFloors(List<Floor> floors) {
        mFloors = floors;
    }

    public Floor getCurrentFloor() {
        return mCurrentFloor;
    }

    public void setCurrentFloor(Floor currentFloor) {
        mCurrentFloor = currentFloor;
    }

    public void setCurrentFloor(String floorId) {
        for (Floor floor : mFloors) {
            if (floor.getId().equals(floorId)) {
                mCurrentFloor = floor;
                return;
            }
        }

        // TODO: deside if this is good.
        throw new RuntimeException("Floor with given ID doesn't exist in current building.");
    }

    public boolean isDirty() {
        return mDirty;
    }

    public void setDirty(boolean dirty) {
        mDirty = dirty;
    }

    public static class TagComparator implements Comparator<Tag> {

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
}
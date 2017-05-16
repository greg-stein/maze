package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;

import com.example.neutrino.maze.AppSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 5/14/2017.
 */

public class Path extends FloorPlanPrimitiveBase {
    private static final float PATH_THICKNESS = 0.5f; // 50 cm

    private final List<ThickLineSegment> mSegments;

    public Path(List<PointF> points) {
        super((points.size()-1) * ThickLineSegment.INDICES_DATA_LENGTH,
                (points.size()-1) * ThickLineSegment.VERTICES_DATA_LENGTH);

        mSegments = new ArrayList<>(points.size() - 1);

        for (int i = 0; i + 1 < points.size(); i++) {
            PointF start = points.get(i);
            PointF end = points.get(i+1);
            ThickLineSegment segment = new ThickLineSegment(start, end, PATH_THICKNESS);
            segment.setColor(AppSettings.pathColor);
            mSegments.add(segment);
        }
    }

    @Override
    public void updateVertices() {
        for (ThickLineSegment segment : mSegments) {
            // TODO: Change original vertices placements to create continuation of path
            segment.updateVertices();
        }
    }

    @Override
    public void scaleVertices(float scaleFactor) {
        for (ThickLineSegment segment : mSegments) {
            segment.scaleVertices(scaleFactor);
        }
    }

    @Override
    public RectF getBoundingBox() {
        RectF boundingBox = mSegments.get(0).getBoundingBox();

        for (ThickLineSegment segment : mSegments) {
            boundingBox.union(segment.getBoundingBox());
        }

        return boundingBox;
    }

    @Override
    protected int getVerticesNum() {
        return mSegments.get(0).getVerticesNum() * mSegments.size();
    }

    @Override
    protected int getIndicesNum() {
        return mSegments.get(0).getIndicesNum() * mSegments.size();
    }
}

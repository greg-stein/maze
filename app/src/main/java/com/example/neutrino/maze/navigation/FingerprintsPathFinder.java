package com.example.neutrino.maze.navigation;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.rendering.VectorHelper;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Greg Stein on 5/7/2017.
 */
public class FingerprintsPathFinder extends PathFinderBase {
    private List<PointF> mNodes;

    public FingerprintsPathFinder(FloorPlan floorPlan) {
        super(floorPlan);
        List<Fingerprint> fingerprints = floorPlan.getFingerprints();
        mNodes = new ArrayList<>(fingerprints.size());

        for (Fingerprint fingerprint : fingerprints) {
            mNodes.add(fingerprint.getCenter());
        }
    }

    @Override
    public void init() {
        initGrid();
        assignPointsToCells();
        assignObstaclesToCells();
        buildGraph();
    }

    private void assignPointsToCells() {
        for (PointF p : mNodes) {
            final int cellX = (int) ((p.x - mBoundaries.left) / mCellSize) + 1;
            final int cellY = (int) ((p.y - mBoundaries.top) / mCellSize) + 1; // should be bottom?
            mGrid[cellX][cellY].points.add(p);

            // Check position within cell to test adjacent cells if their padding overlaps p
            if (p.x > cellX * mCellSize + mBoundaries.left - mCellSize / 2) {
                if (p.y > cellY * mCellSize + mBoundaries.top - mCellSize / 2) {
                    // bottom right quarter. to check: [ ][ ][ ]
                    //                                 [ ][p][X]
                    //                                 [ ][X][X]
                    if (mGrid[cellX + 1][cellY].boundingBox.contains(p.x, p.y))
                        mGrid[cellX + 1][cellY].points.add(p);
                    if (mGrid[cellX + 1][cellY + 1].boundingBox.contains(p.x, p.y))
                        mGrid[cellX + 1][cellY + 1].points.add(p);
                    if (mGrid[cellX][cellY + 1].boundingBox.contains(p.x, p.y))
                        mGrid[cellX][cellY + 1].points.add(p);
                } else {
                    // top right quarter. to check: [ ][X][X]
                    //                              [ ][p][X]
                    //                              [ ][ ][ ]
                    if (mGrid[cellX][cellY - 1].boundingBox.contains(p.x, p.y))
                        mGrid[cellX][cellY - 1].points.add(p);
                    if (mGrid[cellX + 1][cellY - 1].boundingBox.contains(p.x, p.y))
                        mGrid[cellX + 1][cellY - 1].points.add(p);
                    if (mGrid[cellX + 1][cellY].boundingBox.contains(p.x, p.y))
                        mGrid[cellX + 1][cellY].points.add(p);
                }
            } else {
                if (p.y > cellY * mCellSize + mBoundaries.top - mCellSize / 2) {
                    // bottom left quarter. to check: [ ][ ][ ]
                    //                                [X][p][ ]
                    //                                [X][X][ ]
                    if (mGrid[cellX][cellY + 1].boundingBox.contains(p.x, p.y))
                        mGrid[cellX][cellY + 1].points.add(p);
                    if (mGrid[cellX - 1][cellY + 1].boundingBox.contains(p.x, p.y))
                        mGrid[cellX - 1][cellY + 1].points.add(p);
                    if (mGrid[cellX - 1][cellY].boundingBox.contains(p.x, p.y))
                        mGrid[cellX - 1][cellY].points.add(p);
                } else {
                    // top left quarter. to check: [X][X][ ]
                    //                             [X][p][ ]
                    //                             [ ][ ][ ]
                    if (mGrid[cellX - 1][cellY].boundingBox.contains(p.x, p.y))
                        mGrid[cellX - 1][cellY].points.add(p);
                    if (mGrid[cellX - 1][cellY - 1].boundingBox.contains(p.x, p.y))
                        mGrid[cellX - 1][cellY - 1].points.add(p);
                    if (mGrid[cellX][cellY - 1].boundingBox.contains(p.x, p.y))
                        mGrid[cellX][cellY - 1].points.add(p);
                }
            }
        }
    }

    @Override
    protected void buildGraph() {
        mGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (int x = 0; x < mGridSizeX; x++) {
            for (int y = 0; y < mGridSizeY; y++) {
                HashSet<PointF> points = mGrid[x][y].points;
                for (PointF p : points) {
                    mGraph.addVertex(p);
                }

                for (PointF p1 : points) {
                    for (PointF p2 : points) {
                        if ((p1 == p2 || obstacleBetween(mGrid[x][y], p1, p2))) {
                            continue;
                        }
                        DefaultWeightedEdge e = mGraph.addEdge(p1, p2);
                        if (e != null) { // if this edge was added previously
                            mGraph.setEdgeWeight(e, Math.hypot(p2.x - p1.x, p2.y - p1.y));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected PointF findClosestVertex(PointF source) {
        final int gridX = ((int)source.x - mBoundaries.left) / mCellSize + 1;
        final int gridY = ((int)source.y - mBoundaries.top) / mCellSize + 1;

        GridCell cell = mGrid[gridX][gridY];
        // Assumed as farthest point
        PointF closestPoint = null;
        float minDistance = Float.MAX_VALUE;
        for (PointF p : cell.points) {
            float newDistance = VectorHelper.manhattanDistance(source, p);
            if (newDistance < minDistance && !obstacleBetween(cell, source, p)) {
                closestPoint = p;
                minDistance = newDistance;
            }
        }
        return closestPoint;
    }

}

package com.example.neutrino.maze.navigation;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.VisibleForTesting;

import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.example.neutrino.maze.CommonHelper;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.rendering.VectorHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Greg Stein on 5/7/2017.
 */
public class PathFinder {
    public static final int GRID_CELL_SIZE = 20; // in meters
    // NOTE: GRID_CELL_PADDING maybe at its max GRID_CELL_SIZE/2 !!!
    public static final int GRID_CELL_PADDING = 10; // each cell is padded with this much meters on each side
    private List<PointF> mNodes;
    private List<Wall> mObstacles;
    private Rect mBoundaries = new Rect();

    private GridCell[][] mGrid;
    private WeightedGraph<PointF, DefaultWeightedEdge> mGraph;
    private int mGridSizeX;
    private int mGridSizeY;

    public PathFinder(FloorPlan floorPlan) {
        List<Fingerprint> fingerprints = floorPlan.getFingerprints();
        mNodes = new ArrayList<>(fingerprints.size());

        for (Fingerprint fingerprint : fingerprints) {
            mNodes.add(fingerprint.getCenter());
        }

        // Java HORROR
        Iterable<?> objects = floorPlan.getSketch();
        mObstacles = CommonHelper.filterObjects(Wall.class, (Iterable<Object>) objects);

        floorPlan.getBoundaries().roundOut(mBoundaries);
    }

    private void buildFingerprintsGrid() {
        initGrid();
        assignPointsToCells();
        assignObstaclesToCells();
    }

    private void assignObstaclesToCells() {
        for (Wall obstacle : mObstacles) {
            // TODO: Instead of this straight-forward method we can adopt much faster Bresenheim Algorithm
            jopa();
            for (int x = 0; x < mGridSizeX; x++) {
                for (int y = 0; y < mGridSizeY; y++) {
                    PointF obstacleStart = obstacle.getA();
                    PointF obstacleEnd = obstacle.getB();
                    RectF cellBox = mGrid[x][y].boundingBox;

                    if (VectorHelper.lineIntersect(obstacleStart, obstacleEnd, cellBox)) {
                        mGrid[x][y].obstacles.add(obstacle);
                    }
                }
            }
        }
    }

    private void jopa() {

    }

    private void assignPointsToCells() {
        for (PointF p : mNodes) {
            final int cellX = (int) ((p.x - mBoundaries.left) / GRID_CELL_SIZE) + 1;
            final int cellY = (int) ((p.y - mBoundaries.top) / GRID_CELL_SIZE) + 1; // should be bottom?
            mGrid[cellX][cellY].points.add(p);

            // Check position within cell to test adjacent cells if their padding overlaps p
            if (p.x > cellX * GRID_CELL_SIZE + mBoundaries.left - GRID_CELL_SIZE / 2) {
                if (p.y > cellY * GRID_CELL_SIZE + mBoundaries.top - GRID_CELL_SIZE / 2) {
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
                if (p.y > cellY * GRID_CELL_SIZE + mBoundaries.top - GRID_CELL_SIZE / 2) {
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

    private void initGrid() {
        mGridSizeX = mBoundaries.width() / GRID_CELL_SIZE + 2; // pad on left/right
        mGridSizeY = mBoundaries.height() / GRID_CELL_SIZE + 2; // pad on top/bottom
        mGrid = new GridCell[mGridSizeX][mGridSizeY];

        for (int x = 0; x < mGridSizeX; x++) {
            for (int y = 0; y < mGridSizeY; y++) {
                mGrid[x][y] = new GridCell(new RectF(
                        mBoundaries.left + (x-1) * GRID_CELL_SIZE - GRID_CELL_PADDING,
                        mBoundaries.top + (y-1) * GRID_CELL_SIZE - GRID_CELL_PADDING,
                        mBoundaries.left + (x) * GRID_CELL_SIZE + GRID_CELL_PADDING,
                        mBoundaries.top + (y) * GRID_CELL_SIZE + GRID_CELL_PADDING));
            }
        }
    }

    private void buildGraph() {
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

    private boolean obstacleBetween(GridCell cell, PointF p1, PointF p2) {
        for (Wall obstacle : cell.obstacles) {
            if (VectorHelper.linesIntersect(obstacle.getA(), obstacle.getB(), p1, p2)) {
                return true;
            }
        }
        return false;
    }
}

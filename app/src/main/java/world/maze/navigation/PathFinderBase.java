package world.maze.navigation;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import world.maze.util.CommonHelper;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.Wall;
import world.maze.rendering.VectorHelper;

import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 5/18/2017.
 */

public abstract class PathFinderBase {
    public static final int GRID_CELL_SIZE = 20; // in meters
    // NOTE: GRID_CELL_OVERLAP maybe at its max GRID_CELL_SIZE/2 !!!
    public static final int GRID_CELL_OVERLAP = 10; // each cell is padded with this much meters on each side

    protected final List<Wall> mObstacles;
    protected Rect mBoundaries = new Rect();
    protected GridCell[][] mGrid;
    protected int mGridSizeX;
    protected int mGridSizeY;
    protected WeightedGraph<PointF, DefaultWeightedEdge> mGraph;
    protected int mCellOverlap = GRID_CELL_OVERLAP;
    protected int mCellSize = GRID_CELL_SIZE;

    protected PathFinderBase(FloorPlan floorPlan) {
        Iterable<?> objects = floorPlan.getSketch();
        mObstacles = CommonHelper.filterObjects(Wall.class, (Iterable<Object>) objects);

        floorPlan.getBoundaries().roundOut(mBoundaries);
    }

    protected void jopa() {
    }

    public abstract void init();

    public void setCellSize(int size) {
        mCellSize = size;
    }

    public void setCellOverlap(int overlap) {
        mCellOverlap = overlap;
    }

    protected abstract void buildGraph();

    protected void initGrid() {
        mGridSizeX = mBoundaries.width() / mCellSize + 2; // pad on left/right
        mGridSizeY = mBoundaries.height() / mCellSize + 2; // pad on top/bottom
        mGrid = new GridCell[mGridSizeX][mGridSizeY];

        for (int x = 0; x < mGridSizeX; x++) {
            for (int y = 0; y < mGridSizeY; y++) {
                mGrid[x][y] = new GridCell(new RectF(
                        mBoundaries.left + (x-1) * mCellSize - mCellOverlap,
                        mBoundaries.top + (y-1) * mCellSize - mCellOverlap,
                        mBoundaries.left + (x) * mCellSize + mCellOverlap,
                        mBoundaries.top + (y) * mCellSize + mCellOverlap));
            }
        }
    }

    protected final void addEdge(PointF p1, PointF p2) {
        DefaultWeightedEdge e = mGraph.addEdge(p1, p2);
        if (e != null) { // if this edge was added previously
            mGraph.setEdgeWeight(e, Math.hypot(p2.x - p1.x, p2.y - p1.y));
        }
    }

    protected final boolean obstacleBetween(GridCell cell, PointF p1, PointF p2) {
        for (Wall obstacle : cell.obstacles) {
            if (VectorHelper.linesIntersect(obstacle.getStart(), obstacle.getEnd(), p1, p2)) {
                return true;
            }
        }
        return false;
    }

    protected void assignObstaclesToCells() {
        for (Wall obstacle : mObstacles) {
            // TODO: Instead of this straight-forward method we can adopt much faster Bresenheim Algorithm
            for (int x = 0; x < mGridSizeX; x++) {
                for (int y = 0; y < mGridSizeY; y++) {
                    PointF obstacleStart = obstacle.getStart();
                    PointF obstacleEnd = obstacle.getEnd();
                    RectF cellBox = mGrid[x][y].boundingBox;

                    if (VectorHelper.lineIntersect(obstacleStart, obstacleEnd, cellBox)) {
                        mGrid[x][y].obstacles.add(obstacle);
                    }
                }
            }
        }
    }

    public List<PointF> constructPath(PointF source, PointF destination) {
        PointF sourceVertex = findClosestVertex(source);
        PointF destVertex = findClosestVertex(destination);

        if (sourceVertex != null && destVertex != null) {
            List<DefaultWeightedEdge> edgesPath = DijkstraShortestPath.findPathBetween(mGraph, sourceVertex, destVertex);

            // Construct path as List<PointF> from edges
            List<PointF> path = new ArrayList<>(edgesPath.size() + 1); // number of edges + 1
            path.add(sourceVertex); // TODO: do we need to add source as well?
            PointF lastVertex = sourceVertex;

            for (DefaultWeightedEdge e : edgesPath) {
                PointF edgeSource = mGraph.getEdgeSource(e);
                PointF edgeTarget = mGraph.getEdgeTarget(e);

                if (edgeSource.equals(lastVertex)) {
                    path.add(edgeTarget);
                    lastVertex = edgeTarget;
                } else if (edgeTarget.equals(lastVertex)) {
                    path.add(edgeSource);
                    lastVertex = edgeSource;
                } else jopa();
            }

            return path;
        }

        return null;
    }

    protected abstract PointF findClosestVertex(PointF source);
}

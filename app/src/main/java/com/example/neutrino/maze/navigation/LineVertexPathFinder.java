package com.example.neutrino.maze.navigation;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Wall;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.List;

/**
 * Created by Greg Stein on 5/18/2017.
 */

public class LineVertexPathFinder extends PathFinderBase {
    public LineVertexPathFinder(FloorPlan floorPlan) {
        super(floorPlan);
    }

    @Override
    public void init() {
        initGrid();
        assignObstaclesToCells();
        buildGraph();
    }

    @Override
    protected void buildGraph() {
        mGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (Wall wall : mObstacles) {
            PointF wallStart = wall.getStart();
            PointF wallEnd = wall.getEnd();

            double angle = (wallEnd.x == wallStart.x) ? (Math.PI / 2) :
                    Math.atan((wallEnd.y - wallStart.y) / (wallEnd.x - wallStart.x));

            float sin = (float)Math.sin(angle);
            float cos = (float)Math.cos(angle);

//            PointF topLeft = new PointF(-cos-sin, sin-cos);
//            PointF topRight = new PointF(cos-sin, -sin-cos);
//            PointF bottomLeft = new PointF(sin-cos, sin+cos);
//            PointF bottomRight = new PointF(cos+sin, cos-sin);

            mGraph.addVertex(new PointF(wallStart.x - cos - sin, wallStart.y + sin - cos ));
            mGraph.addVertex(new PointF(wallStart.x + cos - sin, wallStart.y - sin - cos ));
            mGraph.addVertex(new PointF(wallStart.x - cos + sin, wallStart.y + sin + cos ));
            mGraph.addVertex(new PointF(wallStart.x + cos + sin, wallStart.y - sin + cos ));
            mGraph.addVertex(new PointF(wallEnd.x - cos - sin, wallEnd.y + sin - cos ));
            mGraph.addVertex(new PointF(wallEnd.x + cos - sin, wallEnd.y - sin - cos ));
            mGraph.addVertex(new PointF(wallEnd.x - cos + sin, wallEnd.y + sin + cos ));
            mGraph.addVertex(new PointF(wallEnd.x + cos + sin, wallEnd.y - sin + cos ));

        }

        for (int x = mBoundaries.left; x < mBoundaries.right - 1; x++) {
            for (int y = mBoundaries.top; y < mBoundaries.bottom - 1; y++) {
                final int cellX = (x - mBoundaries.left) / mCellSize + 1;
                final int cellY = (y - mBoundaries.top) / mCellSize + 1;
                GridCell cell = mGrid[cellX][cellY];

                PointF topLeft = new PointF(x, y);
                PointF topRight = new PointF(x+1, y);
                PointF bottomLeft = new PointF(x, y+1);
                PointF bottomRight = new PointF(x+1, y+1);
                mGraph.addVertex(topLeft);
                mGraph.addVertex(topRight);
                mGraph.addVertex(bottomLeft);
                mGraph.addVertex(bottomRight);

                if (!obstacleBetween(cell, topLeft, topRight)) addEdge(topLeft, topRight);
                if (!obstacleBetween(cell, topRight, bottomRight)) addEdge(topLeft, topRight);
                if (!obstacleBetween(cell, bottomRight, bottomLeft)) addEdge(bottomRight, bottomLeft);
                if (!obstacleBetween(cell, bottomLeft, topLeft)) addEdge(bottomLeft, topLeft);
                if (!obstacleBetween(cell, topLeft, bottomRight)) addEdge(topLeft, bottomRight);
                if (!obstacleBetween(cell, topRight, bottomLeft)) addEdge(topRight, bottomLeft);
            }
        }
    }

    @Override
    protected PointF findClosestVertex(PointF source) {
        final int cellX = (int) ((source.x - mBoundaries.left) / mCellSize + 1);
        final int cellY = (int) ((source.y - mBoundaries.top) / mCellSize + 1);
        GridCell cell = mGrid[cellX][cellY];

        PointF p = new PointF((int)source.x, (int)source.y);
        if (!obstacleBetween(cell, source, p)) return p;

        p.offset(1, 0);
        if (!obstacleBetween(cell, source, p)) return p;

        p.offset(0, 1);
        if (!obstacleBetween(cell, source, p)) return p;

        p.offset(-1, 0);
        if (!obstacleBetween(cell, source, p)) return p;

        return null;
    }
}

package com.example.neutrino.maze.navigation;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.rendering.VectorHelper;

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
//        initGrid();
//        assignObstaclesToCells();
        buildGraph();
    }

    @Override
    protected void buildGraph() {
        addVertexes();
        addEdges();
    }

    private void addVertexes() {
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
    }

    private void addEdges() {
        PointF[] allVertexes = new PointF[mGraph.vertexSet().size()];
        mGraph.vertexSet().toArray(allVertexes);

        for (int i = 0; i < allVertexes.length; i++)
            for (int j = 0; j < i; j++)
                if (!anyObstacleBetween(allVertexes[i], allVertexes[j]))
                    addEdge(allVertexes[i], allVertexes[j]);
    }

    @Override
    protected PointF findClosestVertex(PointF source) {
        PointF candidate = null;

        for (PointF point : mGraph.vertexSet()) {
            if (candidate == null || VectorHelper.squareDistance(point, source) < VectorHelper.squareDistance(candidate, source))
                if (!anyObstacleBetween(point, source))
                    candidate = point;
        }
        return candidate;
    }

    protected final boolean anyObstacleBetween(PointF p1, PointF p2) {
        for (Wall obstacle : mObstacles) {
            if (VectorHelper.linesIntersect(obstacle.getStart(), obstacle.getEnd(), p1, p2)) {
                return true;
            }
        }
        return false;
    }

}

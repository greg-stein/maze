package com.example.neutrino.maze;

import android.graphics.PointF;
import android.graphics.RectF;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.navigation.GridCell;
import com.example.neutrino.maze.navigation.FingerprintsPathFinder;

import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Greg Stein on 5/9/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class FingerprintsPathFinderTests {

    private FloorPlan getFloorPlanFromRes(String resourceFile) {
        String jsonString = null;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            // Get json file from test resources: app/src/test/resources
            InputStream in_s = classLoader.getResourceAsStream(resourceFile);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            jsonString = new String(b);
        } catch (Exception e) {
            e.printStackTrace(); // АААА! Жопа!! жопА!!
        }

        List<Object> objects = FloorPlanSerializer.deserializeFloorPlan(jsonString);
        FloorPlan floorPlan = FloorPlan.build(objects);
        return floorPlan;
    }

    private static <T> T invokeMethod(FingerprintsPathFinder pathFinder, String methodName, Object... params) {
        try {
            Class<?>[] paramTypes = new Class<?>[params.length];
            int cnt = 0;
            for (Object o : params) {
                paramTypes[cnt++] = o.getClass();
            }
            Method method = pathFinder.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return (T)method.invoke(pathFinder, params);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static <T> T readField(FingerprintsPathFinder pathFinder, String fieldName) {
        try {
            Field field = pathFinder.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T)field.get(pathFinder);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Test
    public void initGridTest() {
        FloorPlan floorPlan = FloorPlan.build();
        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();
        sketch.add(new Wall(0, 0, 40, 40));
        sketch.add(new Wall(0, 40, 40, 0));

        FingerprintsPathFinder pathFinder = new FingerprintsPathFinder(floorPlan);

        invokeMethod(pathFinder, "initGrid");

        GridCell[][] grid = readField(pathFinder, "mGrid");

        assertThat(grid.length, is(equalTo(4)));
        assertThat(grid[0].length, is(equalTo(4)));

        // Row 1
        assertThat( grid[0][0].boundingBox, is(equalTo(new RectF(
                -FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                -FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_PADDING, FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[1][0].boundingBox, is(equalTo(new RectF(
                - FingerprintsPathFinder.GRID_CELL_PADDING,
                -FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[2][0].boundingBox, is(equalTo(new RectF(
                FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                -FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[3][0].boundingBox, is(equalTo(new RectF(
                2* FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                -FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                3* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_PADDING))));

        // Row 2
        assertThat( grid[0][1].boundingBox, is(equalTo(new RectF(
                -FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                -FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[1][1].boundingBox, is(equalTo(new RectF(
                -FingerprintsPathFinder.GRID_CELL_PADDING,
                -FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[2][1].boundingBox, is(equalTo(new RectF(
                FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                -FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[3][1].boundingBox, is(equalTo(new RectF(
                2* FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                -FingerprintsPathFinder.GRID_CELL_PADDING,
                3* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        // Row 3
        assertThat( grid[0][2].boundingBox, is(equalTo(new RectF(
                -FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[1][2].boundingBox, is(equalTo(new RectF(
                -FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[2][2].boundingBox, is(equalTo(new RectF(
                FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[3][2].boundingBox, is(equalTo(new RectF(
                2* FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                3* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        // Row 4
        assertThat( grid[0][3].boundingBox, is(equalTo(new RectF(
                -FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_PADDING,
                3* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[1][3].boundingBox, is(equalTo(new RectF(
                -FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                3* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[2][3].boundingBox, is(equalTo(new RectF(
                FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                3* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));

        assertThat( grid[3][3].boundingBox, is(equalTo(new RectF(
                2* FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                2* FingerprintsPathFinder.GRID_CELL_SIZE - FingerprintsPathFinder.GRID_CELL_PADDING,
                3* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING,
                3* FingerprintsPathFinder.GRID_CELL_SIZE + FingerprintsPathFinder.GRID_CELL_PADDING))));
    }

    @Test
    public void assignPointsToCellsTest() {
        FloorPlan floorPlan = FloorPlan.build();
        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();
        sketch.add(new Wall(0, 0, 40, 40));
        sketch.add(new Wall(0, 40, 40, 0));

        List<Fingerprint> fingerprints = floorPlan.getFingerprints();
        // head
        fingerprints.add(new Fingerprint(22, 7, null));
        fingerprints.add(new Fingerprint(22, 13, null));
        fingerprints.add(new Fingerprint(32, 7, null));
        fingerprints.add(new Fingerprint(32, 13, null));

        // neck
        fingerprints.add(new Fingerprint(27, 15, null));
        fingerprints.add(new Fingerprint(27, 17, null));
        fingerprints.add(new Fingerprint(27, 20, null));

        //body
        fingerprints.add(new Fingerprint(10, 22, null));
        fingerprints.add(new Fingerprint(15, 22, null));
        fingerprints.add(new Fingerprint(20, 22, null));
        fingerprints.add(new Fingerprint(25, 22, null));
        fingerprints.add(new Fingerprint(30, 22, null));
        fingerprints.add(new Fingerprint(35, 22, null));
        fingerprints.add(new Fingerprint(32, 27, null));
        fingerprints.add(new Fingerprint(30, 32, null));
        fingerprints.add(new Fingerprint(27, 37, null));
        fingerprints.add(new Fingerprint(22, 37, null));
        fingerprints.add(new Fingerprint(17, 37, null));
        fingerprints.add(new Fingerprint(12, 37, null));
        fingerprints.add(new Fingerprint(10, 32, null));
        fingerprints.add(new Fingerprint(7, 27, null));

        // tail
        fingerprints.add(new Fingerprint(7, 7, null));
        fingerprints.add(new Fingerprint(10, 12, null));
        fingerprints.add(new Fingerprint(12, 17, null));

        FingerprintsPathFinder pathFinder = new FingerprintsPathFinder(floorPlan);
        invokeMethod(pathFinder, "initGrid");
        invokeMethod(pathFinder, "assignPointsToCells");

        GridCell[][] grid = readField(pathFinder, "mGrid");

        assertThat(grid[0][0].points, hasSize(1));
        assertThat(grid[1][0].points, hasSize(2));
        assertThat(grid[2][0].points, hasSize(2));
        assertThat(grid[3][0].points, hasSize(1));

        assertThat(grid[0][1].points, hasSize(2));
        assertThat(grid[1][1].points, hasSize(13));
        assertThat(grid[2][1].points, hasSize(14));
        assertThat(grid[3][1].points, hasSize(4));

        assertThat(grid[0][2].points, hasSize(1));
        assertThat(grid[1][2].points, hasSize(16));
        assertThat(grid[2][2].points, hasSize(17));
        assertThat(grid[3][2].points, hasSize(3));

        assertThat(grid[0][3].points, hasSize(0));
        assertThat(grid[1][3].points, hasSize(5));
        assertThat(grid[2][3].points, hasSize(5));
        assertThat(grid[3][3].points, hasSize(0));
    }

    @Test
    public void assignObstaclesToCellsTest() {
        FloorPlan floorPlan = FloorPlan.build();
        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();

        Wall a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s;
        sketch.add(a = new Wall(5, 5, 10, 20));
        sketch.add(b = new Wall(10, 20, 5, 25));
        sketch.add(c = new Wall(5, 25, 10, 40));
        sketch.add(d = new Wall(10, 40, 30, 40));
        sketch.add(e = new Wall(30, 40, 40, 20));
        sketch.add(f = new Wall(40, 20, 30, 20));
        sketch.add(g = new Wall(30, 20, 30, 15));
        sketch.add(h = new Wall(30, 15, 35, 15));
        sketch.add(i= new Wall(35, 15, 35, 5));
        sketch.add(j = new Wall(35, 5, 20, 5));
        sketch.add(k = new Wall(20, 5, 20, 15));
        sketch.add(l = new Wall(20, 15, 25, 15));
        sketch.add(m = new Wall(25, 15, 25, 20));
        sketch.add(n = new Wall(25, 20, 15, 20));
        sketch.add(o = new Wall(15, 20, 10, 5));

        // Frame
        sketch.add(p = new Wall(0, 0, 40, 0));
        sketch.add(q = new Wall(40, 0, 40, 40));
        sketch.add(r = new Wall(40, 40, 0, 40));
        sketch.add(s = new Wall(0, 40, 0, 0));

        FingerprintsPathFinder pathFinder = new FingerprintsPathFinder(floorPlan);
        invokeMethod(pathFinder, "initGrid");
        invokeMethod(pathFinder, "assignObstaclesToCells");

        GridCell[][] grid = readField(pathFinder, "mGrid");

        assertThat(grid[0][0].obstacles, hasSize(3));
        assertThat(grid[0][0].obstacles, containsInAnyOrder(p, s, a));
        assertThat(grid[1][0].obstacles, hasSize(6));
        assertThat(grid[1][0].obstacles, containsInAnyOrder(p, s, a, o, j, k));
        assertThat(grid[2][0].obstacles, hasSize(6));
        assertThat(grid[2][0].obstacles, containsInAnyOrder(p, q, o, i, j, k));
        assertThat(grid[3][0].obstacles, hasSize(4));
        assertThat(grid[3][0].obstacles, containsInAnyOrder(p, q, i, j));

        assertThat(grid[0][1].obstacles, hasSize(5));
        assertThat(grid[0][1].obstacles, containsInAnyOrder(p, s, a, b, c));
        assertThat(grid[1][1].obstacles, hasSize(11));
        assertThat(grid[1][1].obstacles, containsInAnyOrder(p, s, a, b, c, j, k, l, m, n, o));
        assertThat(grid[2][1].obstacles, hasSize(13));
        assertThat(grid[2][1].obstacles, containsInAnyOrder(p, q, e, f, g, h, i, j, k, l, m, n, o));
        assertThat(grid[3][1].obstacles, hasSize(7));
        assertThat(grid[3][1].obstacles, containsInAnyOrder(p, q, e, f, h, i, j));

        assertThat(grid[0][2].obstacles, hasSize(5));
        assertThat(grid[0][2].obstacles, containsInAnyOrder(r, s, a, b, c));
        assertThat(grid[1][2].obstacles, hasSize(11));
        assertThat(grid[1][2].obstacles, containsInAnyOrder(r, s, a, b, c, d, k, l, m, n, o));
        assertThat(grid[2][2].obstacles, hasSize(13));
        assertThat(grid[2][2].obstacles, containsInAnyOrder(q, r, d, e, f, g, h, i, k, l, m, n, o));
        assertThat(grid[3][2].obstacles, hasSize(6));
        assertThat(grid[3][2].obstacles, containsInAnyOrder(q, r, e, f, h, i));

        assertThat(grid[0][3].obstacles, hasSize(3));
        assertThat(grid[0][3].obstacles, containsInAnyOrder(r, s, c));
        assertThat(grid[1][3].obstacles, hasSize(4));
        assertThat(grid[1][3].obstacles, containsInAnyOrder(r, s, c, d));
        assertThat(grid[2][3].obstacles, hasSize(4));
        assertThat(grid[2][3].obstacles, containsInAnyOrder(q, r, d, e));
        assertThat(grid[3][3].obstacles, hasSize(3));
        assertThat(grid[3][3].obstacles, containsInAnyOrder(q, r, e));
    }

    @Test
    public void buildGraphStressTest() {
        FloorPlan floorPlan = FloorPlan.build();

        List<Fingerprint> fingerprints = floorPlan.getFingerprints();
        // head
        fingerprints.add(new Fingerprint(22, 7, null));
        fingerprints.add(new Fingerprint(22, 13, null));
        fingerprints.add(new Fingerprint(32, 7, null));
        fingerprints.add(new Fingerprint(32, 13, null));

        // neck
        fingerprints.add(new Fingerprint(27, 15, null));
        fingerprints.add(new Fingerprint(27, 17, null));
        fingerprints.add(new Fingerprint(27, 20, null));

        //body
        fingerprints.add(new Fingerprint(10, 22, null));
        fingerprints.add(new Fingerprint(15, 22, null));
        fingerprints.add(new Fingerprint(20, 22, null));
        fingerprints.add(new Fingerprint(25, 22, null));
        fingerprints.add(new Fingerprint(30, 22, null));
        fingerprints.add(new Fingerprint(35, 22, null));
        fingerprints.add(new Fingerprint(32, 27, null));
        fingerprints.add(new Fingerprint(30, 32, null));
        fingerprints.add(new Fingerprint(27, 37, null));
        fingerprints.add(new Fingerprint(22, 37, null));
        fingerprints.add(new Fingerprint(17, 37, null));
        fingerprints.add(new Fingerprint(12, 37, null));
        fingerprints.add(new Fingerprint(10, 32, null));
        fingerprints.add(new Fingerprint(7, 27, null));

        // tail
        fingerprints.add(new Fingerprint(7, 7, null));
        fingerprints.add(new Fingerprint(10, 12, null));
        fingerprints.add(new Fingerprint(12, 17, null));

        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();

        sketch.add(new Wall(5, 5, 10, 20));
        sketch.add(new Wall(10, 20, 5, 25));
        sketch.add(new Wall(5, 25, 10, 40));
        sketch.add(new Wall(10, 40, 30, 40));
        sketch.add(new Wall(30, 40, 40, 20));
        sketch.add(new Wall(40, 20, 30, 20));
        sketch.add(new Wall(30, 20, 30, 15));
        sketch.add(new Wall(30, 15, 35, 15));
        sketch.add(new Wall(35, 15, 35, 5));
        sketch.add(new Wall(35, 5, 20, 5));
        sketch.add(new Wall(20, 5, 20, 15));
        sketch.add(new Wall(20, 15, 25, 15));
        sketch.add(new Wall(25, 15, 25, 20));
        sketch.add(new Wall(25, 20, 15, 20));
        sketch.add(new Wall(15, 20, 10, 5));

        // Frame
        sketch.add(new Wall(0, 0, 40, 0));
        sketch.add(new Wall(40, 0, 40, 40));
        sketch.add(new Wall(40, 40, 0, 40));
        sketch.add(new Wall(0, 40, 0, 0));

        FingerprintsPathFinder pathFinder = new FingerprintsPathFinder(floorPlan);
        invokeMethod(pathFinder, "initGrid");
        invokeMethod(pathFinder, "assignPointsToCells");
        invokeMethod(pathFinder, "assignObstaclesToCells");
        invokeMethod(pathFinder, "buildGraph");

        WeightedGraph<PointF, DefaultWeightedEdge> graph = readField(pathFinder, "mGraph");
    }

    @Test
    public void buildGraphTest() {
        FloorPlan floorPlan = FloorPlan.build();

        List<Fingerprint> fingerprints = floorPlan.getFingerprints();
        fingerprints.add(new Fingerprint(5, 5, null));
        fingerprints.add(new Fingerprint(20, 5, null));
        fingerprints.add(new Fingerprint(35, 5, null));
        fingerprints.add(new Fingerprint(20, 15, null));
        fingerprints.add(new Fingerprint(35, 15, null));
        fingerprints.add(new Fingerprint(35, 25, null));
        fingerprints.add(new Fingerprint(5, 35, null));
        fingerprints.add(new Fingerprint(20, 35, null));
        fingerprints.add(new Fingerprint(35, 35, null));

        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();
        sketch.add(new Wall(10, 10, 10, 25));
        sketch.add(new Wall(25, 10, 25, 30));
        // Frame
        sketch.add(new Wall(0, 0, 40, 0));
        sketch.add(new Wall(40, 0, 40, 40));
        sketch.add(new Wall(40, 40, 0, 40));
        sketch.add(new Wall(0, 40, 0, 0));

        FingerprintsPathFinder pathFinder = new FingerprintsPathFinder(floorPlan);
        invokeMethod(pathFinder, "initGrid");
        invokeMethod(pathFinder, "assignPointsToCells");
        invokeMethod(pathFinder, "assignObstaclesToCells");
        invokeMethod(pathFinder, "buildGraph");

        WeightedGraph<PointF, DefaultWeightedEdge> graph = readField(pathFinder, "mGraph");
        assert graph != null;
        assertThat(graph.edgeSet(), hasSize(15));
    }

    @Test
    public void constructPathTest() {
        FloorPlan floorPlan = FloorPlan.build();

        List<Fingerprint> fingerprints = floorPlan.getFingerprints();
        fingerprints.add(new Fingerprint(5, 5, null));
        fingerprints.add(new Fingerprint(20, 5, null));
        fingerprints.add(new Fingerprint(35, 5, null));
        fingerprints.add(new Fingerprint(20, 15, null));
        fingerprints.add(new Fingerprint(35, 15, null));
        fingerprints.add(new Fingerprint(35, 25, null));
        fingerprints.add(new Fingerprint(5, 35, null));
        fingerprints.add(new Fingerprint(20, 35, null));
        fingerprints.add(new Fingerprint(35, 35, null));

        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();
        sketch.add(new Wall(10, 10, 10, 25));
        sketch.add(new Wall(25, 10, 25, 30));
        // Frame
        sketch.add(new Wall(0, 0, 40, 0));
        sketch.add(new Wall(40, 0, 40, 40));
        sketch.add(new Wall(40, 40, 0, 40));
        sketch.add(new Wall(0, 40, 0, 0));

        FingerprintsPathFinder pathFinder = new FingerprintsPathFinder(floorPlan);
        invokeMethod(pathFinder, "initGrid");
        invokeMethod(pathFinder, "assignPointsToCells");
        invokeMethod(pathFinder, "assignObstaclesToCells");
        invokeMethod(pathFinder, "buildGraph");

        List<PointF> path = pathFinder.constructPath(new PointF(0, 40), new PointF(39, 15));

        assertNotNull(path);
        assertThat(path, hasSize(4));
        assertThat(path.get(0), is(equalTo(new PointF(5, 35))));
        assertThat(path.get(1), is(equalTo(new PointF(20, 35))));
        assertThat(path.get(2), is(equalTo(new PointF(35, 25))));
        assertThat(path.get(3), is(equalTo(new PointF(35, 15))));
    }

    @Test
    public void findClosestVertexTest() {
        FloorPlan floorPlan = FloorPlan.build();

        List<Fingerprint> fingerprints = floorPlan.getFingerprints();
        fingerprints.add(new Fingerprint(5, 5, null));
        fingerprints.add(new Fingerprint(20, 5, null));
        fingerprints.add(new Fingerprint(35, 5, null));
        fingerprints.add(new Fingerprint(20, 15, null));
        fingerprints.add(new Fingerprint(35, 15, null));
        fingerprints.add(new Fingerprint(35, 25, null));
        fingerprints.add(new Fingerprint(5, 35, null));
        fingerprints.add(new Fingerprint(20, 35, null));
        fingerprints.add(new Fingerprint(35, 35, null));

        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();
        // Frame
        sketch.add(new Wall(0, 0, 40, 0));
        sketch.add(new Wall(40, 0, 40, 40));
        sketch.add(new Wall(40, 40, 0, 40));
        sketch.add(new Wall(0, 40, 0, 0));

        FingerprintsPathFinder pathFinder = new FingerprintsPathFinder(floorPlan);
        invokeMethod(pathFinder, "initGrid");
        invokeMethod(pathFinder, "assignPointsToCells");
        invokeMethod(pathFinder, "assignObstaclesToCells");
        invokeMethod(pathFinder, "buildGraph");
        PointF closestVertex = invokeMethod(pathFinder, "findClosestVertex", new PointF(0, 40));

        assertNotNull(closestVertex);
        assertThat(closestVertex.x, is(equalTo(5f)));
        assertThat(closestVertex.y, is(equalTo(35f)));
    }

    @Test
    public void realStressTest() {
        FloorPlan floorPlan = getFloorPlanFromRes("haifa_mall_detailed_tags.json");
        FingerprintsPathFinder pathFinder = new FingerprintsPathFinder(floorPlan);

        // pathFinder.init(); Does that:
        long start = System.nanoTime();
        invokeMethod(pathFinder, "initGrid");
        System.out.println(String.format("initGrid. elapsed: %.2f ms", (System.nanoTime() - start)/1000000f));
        start = System.nanoTime();
        invokeMethod(pathFinder, "assignPointsToCells");
        System.out.println(String.format("assignPointsToCells. elapsed: %.2f ms", (System.nanoTime() - start)/1000000f));
        start = System.nanoTime();
        invokeMethod(pathFinder, "assignObstaclesToCells");
        System.out.println(String.format("assignObstaclesToCells. elapsed: %.2f ms", (System.nanoTime() - start)/1000000f));
        start = System.nanoTime();
        invokeMethod(pathFinder, "buildGraph");
        System.out.println(String.format("buildGraph. elapsed: %.2f ms", (System.nanoTime() - start)/1000000f));

        PointF startPoint = new PointF(244.76593f, 55.589268f);
        PointF endPoint = new PointF(55.76811f, 49.82863f);

        start = System.nanoTime();
        List<PointF> path = pathFinder.constructPath(startPoint, endPoint);
        System.out.println(String.format("constructPath. elapsed: %.2f ms", (System.nanoTime() - start)/1000000f));
    }
}

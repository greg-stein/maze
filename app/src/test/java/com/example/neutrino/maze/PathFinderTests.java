package com.example.neutrino.maze;

import android.graphics.RectF;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.navigation.GridCell;
import com.example.neutrino.maze.navigation.PathFinder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 5/9/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class PathFinderTests {

    private static void invokeMethod(PathFinder pathFinder, String methodName) {
        Method initGrid = null;
        try {
            initGrid = pathFinder.getClass().getDeclaredMethod(methodName);
            initGrid.setAccessible(true);
            initGrid.invoke(pathFinder);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void initGridTest() {
        FloorPlan floorPlan = FloorPlan.build();
        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();
        sketch.add(new Wall(0, 0, 40, 40));
        sketch.add(new Wall(0, 40, 40, 0));

        PathFinder pathFinder = new PathFinder(floorPlan);

        invokeMethod(pathFinder, "initGrid");

        GridCell[][] grid = pathFinder.getGrid();

        assertThat(grid.length, is(equalTo(4)));
        assertThat(grid[0].length, is(equalTo(4)));

        // Row 1
        assertThat( grid[0][0].boundingBox, is(equalTo(new RectF(
                -PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                -PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_PADDING, PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[1][0].boundingBox, is(equalTo(new RectF(
                - PathFinder.GRID_CELL_PADDING,
                -PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[2][0].boundingBox, is(equalTo(new RectF(
                PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                -PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[3][0].boundingBox, is(equalTo(new RectF(
                2*PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                -PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                3*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_PADDING))));

        // Row 2
        assertThat( grid[0][1].boundingBox, is(equalTo(new RectF(
                -PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                -PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[1][1].boundingBox, is(equalTo(new RectF(
                -PathFinder.GRID_CELL_PADDING,
                -PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[2][1].boundingBox, is(equalTo(new RectF(
                PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                -PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[3][1].boundingBox, is(equalTo(new RectF(
                2*PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                -PathFinder.GRID_CELL_PADDING,
                3*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        // Row 3
        assertThat( grid[0][2].boundingBox, is(equalTo(new RectF(
                -PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[1][2].boundingBox, is(equalTo(new RectF(
                -PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[2][2].boundingBox, is(equalTo(new RectF(
                PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[3][2].boundingBox, is(equalTo(new RectF(
                2*PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                3*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        // Row 4
        assertThat( grid[0][3].boundingBox, is(equalTo(new RectF(
                -PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_PADDING,
                3*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[1][3].boundingBox, is(equalTo(new RectF(
                -PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                3*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[2][3].boundingBox, is(equalTo(new RectF(
                PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                3*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));

        assertThat( grid[3][3].boundingBox, is(equalTo(new RectF(
                2*PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                2*PathFinder.GRID_CELL_SIZE - PathFinder.GRID_CELL_PADDING,
                3*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING,
                3*PathFinder.GRID_CELL_SIZE + PathFinder.GRID_CELL_PADDING))));
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

        PathFinder pathFinder = new PathFinder(floorPlan);
        invokeMethod(pathFinder, "initGrid");
        invokeMethod(pathFinder, "assignPointsToCells");

        GridCell[][] grid = pathFinder.getGrid();

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

        PathFinder pathFinder = new PathFinder(floorPlan);
        invokeMethod(pathFinder, "initGrid");
        invokeMethod(pathFinder, "assignObstaclesToCells");

        GridCell[][] grid = pathFinder.getGrid();

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
}

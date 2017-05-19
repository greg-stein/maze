package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.navigation.FingerprintsPathFinder;
import com.example.neutrino.maze.navigation.GridPathFinder;
import com.example.neutrino.maze.navigation.PathFinderBase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

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
 * Created by Greg Stein on 5/18/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class GridPathFinderTests {
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

    private static <T> T invokeMethod(Object pathFinder, String methodName, Object... params) {
        Class<?>[] paramTypes = new Class<?>[params.length];
        try {
            int cnt = 0;
            for (Object o : params) {
                paramTypes[cnt++] = o.getClass();
            }
            Method method = pathFinder.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return (T)method.invoke(pathFinder, params);
        } catch (NoSuchMethodException e) {
            Method method = null;
            try {
                method = PathFinderBase.class.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return (T)method.invoke(pathFinder, params);
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            }
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
    public void commonTest() {
        FloorPlan floorPlan = getFloorPlanFromRes("haifa_mall_detailed_tags.json");
        GridPathFinder pathFinder = new GridPathFinder(floorPlan);

//        pathFinder.init();// Does that:
        long start = System.nanoTime();
        invokeMethod(pathFinder, "initGrid");
        System.out.println(String.format("initGrid. elapsed: %.2f ms", (System.nanoTime() - start)/1000000f));
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

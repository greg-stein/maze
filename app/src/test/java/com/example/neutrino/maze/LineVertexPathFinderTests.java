package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.navigation.FingerprintsPathFinder;
import com.example.neutrino.maze.navigation.GridPathFinder;
import com.example.neutrino.maze.navigation.LineVertexPathFinder;
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

/**
 * Created by Dima Ruinskiy on 4/2/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class LineVertexPathFinderTests {
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

    @Test
    public void commonTest() {
        FloorPlan floorPlan = getFloorPlanFromRes("haifa_mall_detailed_tags.json");
        LineVertexPathFinder pathFinder = new LineVertexPathFinder(floorPlan);
        pathFinder.setCellOverlap(2);
        pathFinder.setCellSize(5);

//        pathFinder.init();// Does that:
        long start = System.nanoTime();
        invokeMethod(pathFinder, "buildGraph");
        System.out.println(String.format("buildGraph. elapsed: %.2f ms", (System.nanoTime() - start)/1000000f));

        PointF startPoint = new PointF(244.76593f, 55.589268f);
        PointF endPoint = new PointF(55.76811f, 49.82863f);

        start = System.nanoTime();
        List<PointF> path = pathFinder.constructPath(startPoint, endPoint);
        System.out.println(String.format("constructPath. elapsed: %.2f ms", (System.nanoTime() - start)/1000000f));
    }
}

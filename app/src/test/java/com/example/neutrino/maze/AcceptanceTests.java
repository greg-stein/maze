package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.WifiMark;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 11/23/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class AcceptanceTests {
    private static final double MAX_ERROR = 5; // meters

    private List<WifiMark> marks;
    private List<Wall> walls;
    private WiFiTug wifiTug;

    // This func runs before each @Test in this class.
    @Before
    public void ReadFloorPlanFromResources() {
        String jsonString = null;

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            // Get json file from test resources: app/src/test/resources
            InputStream in_s = classLoader.getResourceAsStream("floorplan_greg_home_2nd_floor.json");

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            jsonString = new String(b);
        } catch (Exception e) {
            e.printStackTrace(); // АААА! Жопа!! жопА!!
        }

        List<IFloorPlanPrimitive> deserializedList = FloorPlanSerializer.deserializeFloorPlan(jsonString);

        marks = CommonHelper.getPrimitives(WifiMark.class, deserializedList);
        walls = CommonHelper.getPrimitives(Wall.class, deserializedList);

        wifiTug = new WiFiTug();
        wifiTug.walls = walls;
        wifiTug.marks = marks;
    }

    /**
     * The purpose of this test is to allow testing of positioning algorithm without involving the
     * device. The logic is quite simple and avoids mocking of tons of classes.
     *
     * This test picks a random wifi mark as a sample fingerprint. The selected wifi mark is
     * removed from list of wifi marks. This way we mimic the scanned fingerprint. We know actual
     * position of the scan because we know the wifi mark location.
     *
     * This test will fail if obtained position is farther than known position of wifi mark by
     * MAX_ERROR meters. This pass-condition is not perfect, but it gives some illustration. It
     * is quite flexible - change it wherever you need to test your algorithm. No hard conditions
     * on this test for production yet.
     */
    @Test
    public void WifiPositioningAcceptanceTest() {
        Random random = new Random(); // for picking random wifi mark as "current fingerprint"
        int randomIndex = random.nextInt(marks.size());
        WifiMark fingerprintMark = marks.remove(randomIndex);
        // Mimic scanned fingerprint
        wifiTug.currentFingerprint = fingerprintMark.getFingerprint();

        PointF expectedPosition = fingerprintMark.getCenter();
        PointF actualPosition = new PointF();
        wifiTug.getPosition(actualPosition);

        // Calculate distance between expected and actual positions
        actualPosition.offset(expectedPosition.x, expectedPosition.y);
        double distanceBetweenActualAndExpected = actualPosition.length();
        assertThat(distanceBetweenActualAndExpected, lessThan(MAX_ERROR));
    }
}

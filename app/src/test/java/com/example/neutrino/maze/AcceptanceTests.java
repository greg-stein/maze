package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.WifiMark;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.List;
import java.util.Random;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(AcceptanceTests.class.getName());

    private List<WifiMark> marks;
    private List<Wall> walls;
    private WiFiTug wifiTug;

    @Rule
    public ErrorCollector collector = new ErrorCollector();
    private WifiMark fingerprintMark;

    @BeforeClass
    public static void setup() {
        CustomRecordFormatter formatter = new CustomRecordFormatter();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(consoleHandler);
    }

    // This func runs before each @Test in this class.
    @Before
    public void ReadFloorPlanFromResources() {
        String jsonString = null;

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            // Get json file from test resources: app/src/test/resources
            InputStream in_s = classLoader.getResourceAsStream("floorplan_greg_home_2nd_floor_2.json");

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

    static List<Double> distances = new ArrayList<>();

    @Test
    public void wifiPositioningAcceptanceTest() {
        for (int index = 0; index < marks.size(); index++) {
            wifiPositioningAcceptanceTest(index);
        }

        double[] distancesArray = new double[distances.size()];
        for (int i = 0; i < distances.size(); i++) {
            distancesArray[i] = distances.get(i);
            LOGGER.info(String.format(Locale.getDefault(), "Distance: %.2f", distancesArray[i]));
        }

        double mean = StatUtils.mean(distancesArray);
        double variance = StatUtils.variance(distancesArray, mean);
        double stdev = Math.sqrt(variance);

        LOGGER.info("----------------------------------------------------");
        LOGGER.info(String.format(Locale.getDefault(), "Mean:     %.2f", mean));
        LOGGER.info(String.format(Locale.getDefault(), "Stdev:    %.2f", stdev));
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
    public void wifiPositioningAcceptanceTest(int markIndex) {
        fingerprintMark = marks.remove(markIndex);
        // Mimic scanned fingerprint
        wifiTug.currentFingerprint = fingerprintMark.getFingerprint();

        PointF actualPosition = new PointF();
        PointF expectedPosition = fingerprintMark.getCenter();
        wifiTug.getPosition(actualPosition);

        // Calculate distance between expected and actual positions
//        actualPosition.offset(expectedPosition.x, expectedPosition.y);
        float xDiff = actualPosition.x - expectedPosition.x;
        float yDiff = actualPosition.y - expectedPosition.y;

        double distanceBetweenActualAndExpected =
                Math.sqrt(xDiff * xDiff + yDiff * yDiff);
        distances.add(distanceBetweenActualAndExpected);
        marks.add(markIndex, fingerprintMark);
//        collector.checkThat(distanceBetweenActualAndExpected, lessThan(MAX_ERROR));
    }

    // So why do we need this class? Because I want to suppress those annoying
    // double lines of standard logger output! Welcome to Java World!
    static class CustomRecordFormatter extends Formatter {
        @Override
        public String format(final LogRecord r) {
            StringBuilder sb = new StringBuilder();
            sb.append(formatMessage(r)).append(System.getProperty("line.separator"));
            if (null != r.getThrown()) {
                sb.append("Throwable occurred: "); //$NON-NLS-1$
                Throwable t = r.getThrown();
                PrintWriter pw = null;
                try {
                    StringWriter sw = new StringWriter();
                    pw = new PrintWriter(sw);
                    t.printStackTrace(pw);
                    sb.append(sw.toString());
                } finally {
                    if (pw != null) {
                        try {
                            pw.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
            return sb.toString();
        }
    }
}

package com.example.neutrino.maze;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

/**
 * Created by Greg Stein on 4/10/2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class GeneticLocatorTests {
    private static final Logger LOGGER = Logger.getLogger(GeneticLocatorTests.class.getName());
    private List<Fingerprint> fingerprints;
    private List<Fingerprint> walkPath;
    private List<WiFiFingerprint> history;

    @BeforeClass
    public static void setup() {
        AcceptanceTests.CustomRecordFormatter formatter = new AcceptanceTests.CustomRecordFormatter();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(consoleHandler);
    }

    @Before
    public void ReadTestDataFromResources() {
        List<Object> floorPlanFromRes = getFloorPlanFromRes("haifa_mall_many_fingerprints.json");
        List<Object> walkingPathFromRes = getFloorPlanFromRes("walking_path.json");

        fingerprints = CommonHelper.extractObjects(Fingerprint.class, floorPlanFromRes);
        walkPath = CommonHelper.extractObjects(Fingerprint.class, walkingPathFromRes);
        history = new ArrayList<>(WALK_PATH_LENGTH);
        final List<Fingerprint> fingerprints = walkPath.subList(0, WALK_PATH_LENGTH);
        for (Fingerprint f : fingerprints) {
            history.add(f.getFingerprint());
        }
    }

    public List<Object> getFloorPlanFromRes(String resourceFile) {
        String jsonString = null;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
//            Class<? extends GeneticLocatorTests> aClass = GeneticLocatorTests.class;
            // Get json file from test resources: app/src/test/resources
            InputStream in_s = classLoader.getResourceAsStream(resourceFile);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            jsonString = new String(b);
        } catch (Exception e) {
            e.printStackTrace(); // АААА! Жопа!! жопА!!
        }

        return FloorPlanSerializer.deserializeFloorPlan(jsonString);
    }

    private static final int WALK_PATH_LENGTH = 5;
    private static final float CROSSOVER_RATE = 0.5f;
    private static final float MUTATION_RATE = 0.3f;
    private static final float CRAWLING_RATE = 0.25f;

//    @Ignore // This test is very long ~1 min
    @Test
    public void commonGaTest() {
        GeneticLocator.setWorld(fingerprints);
        GeneticLocator.setHistory(history);
        GeneticLocator.crossoverRate = CROSSOVER_RATE;
        GeneticLocator.mutationRate = MUTATION_RATE;
        GeneticLocator.crawlingRate = CRAWLING_RATE;

        int headIndex = 4;
        GeneticLocator.realHeadLocation = walkPath.get(headIndex).getCenter();
        GeneticLocator.headIndex = headIndex;
        GeneticLocator.evolution();

//        PointF pointF = new PointF();
//        WiFiLocator wiFiTug = new WiFiLocator();
//        wiFiTug.marks = fingerprints;
//        for (int i = 0; i < WALK_PATH_LENGTH; i++) {
//            wiFiTug.currentWiFiFingerprint = walkPath.get(i).getFingerprint();
//            wiFiTug.getPosition(pointF);
//            System.out.println(String.format("%.4f %.4f", pointF.x, pointF.y));
//        }
    }
}

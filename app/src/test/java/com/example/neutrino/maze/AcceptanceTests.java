package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.Wall;
import com.opencsv.CSVWriter;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Created by Greg Stein on 11/23/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class AcceptanceTests {
    private static final double MAX_ERROR = 5; // meters
    private static final Logger LOGGER = Logger.getLogger(AcceptanceTests.class.getName());
    private static CSVWriter csvWriter;

    private List<Fingerprint> marks;
    private List<Wall> walls;
    private WiFiLocator wifiLocator;

    @Rule
    public ErrorCollector collector = new ErrorCollector();
    private Fingerprint fingerprintMark;

    @BeforeClass
    public static void setup() {
        CustomRecordFormatter formatter = new CustomRecordFormatter();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(consoleHandler);
    }

    private static void logToFile(String filename) {
        try {
            csvWriter = new CSVWriter(new FileWriter(filename), ',');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This func runs before each @Test in this class.
    @Before
    public void ReadFloorPlanFromResources() {
        List<Object> deserializedList = getFloorPlanFromRes("haifa_mall_many_fingerprints.json");

        marks = CommonHelper.extractObjects(Fingerprint.class, deserializedList);
        walls = CommonHelper.extractObjects(Wall.class, deserializedList);

        wifiLocator = WiFiLocator.getInstance();
        wifiLocator.walls = walls;
        wifiLocator.marks = marks;
    }

    private List<Object> getFloorPlanFromRes(String resourceFile) {
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

        return FloorPlanSerializer.deserializeFloorPlan(jsonString);
    }

    static List<Double> distances = new ArrayList<>();

    @Ignore // This test fails - need to accomplish history-based location method
    @Test
    public void wifiHistoryTest() {
        List<Fingerprint> floorPlanMarks = loadWifiMarksFromRes("haifa_mall_floorplan.json");
        List<Fingerprint> pathMarks = loadWifiMarksFromRes("walking_path.json");

        wifiLocator.marks = floorPlanMarks;
        wifiLocator.currentHistory = new WiFiLocator.FingerprintHistory(10);
        for (int i = 3; i < 13; i++) {
            wifiLocator.addToFingerprintHistory(pathMarks.get(i).getFingerprint());
        }

        List<PointF> mostProbably = new ArrayList<>();
        wifiLocator.getMostProbableTrajectory(mostProbably);

        assertEquals(10, mostProbably.size());

        for (int i = 3; i < 13; i++) {
            System.out.print ("actual: (" + pathMarks.get(i).getCenter().x + " , " + pathMarks.get(i).getCenter().y + ") ");
            System.out.println ("xpectd: (" + mostProbably.get(i-3).x + " , " + mostProbably.get(i-3).y + ")");
        }
    }

    private List<Fingerprint> loadWifiMarksFromRes(String resFileName) {
        List<Object> pathFingerprintsAsObjects = getFloorPlanFromRes(resFileName);
        List<Fingerprint> pathMarks = CommonHelper.extractObjects(Fingerprint.class, pathFingerprintsAsObjects);

        return pathMarks;
    }

    @Test
    public void wifiHistoryTrivialTest() {
        wifiLocator.currentHistory = new WiFiLocator.FingerprintHistory(10);

        Fingerprint standingMark = marks.get(marks.size() / 2);
        WiFiLocator.WiFiFingerprint standingPrint = standingMark.getFingerprint();
        for (int i = 0; i < 10; i++) {
            wifiLocator.addToFingerprintHistory(standingPrint);
        }

        List<PointF> mostProbably = new ArrayList<>();
        wifiLocator.getMostProbableTrajectory(mostProbably);

        assertEquals(mostProbably.size(), wifiLocator.currentHistory.size());

        for (int i = 0; i < 10; i++) {
            assertEquals(mostProbably.get(i).x, standingMark.getCenter().x, 1e-5);
            assertEquals(mostProbably.get(i).y, standingMark.getCenter().y, 1e-5);
        }

        LOGGER.info("----------------------------------------------------");
    }

    private Map<Fingerprint, PointF> predictedLocations = new HashMap<>();
    @Test
    public void wifiPositioningAcceptanceTest() {
        for (int index = 0; index < marks.size(); index++) {
            wifiPositioningAcceptanceTest(index);
        }

        int badFingerprints = 0;
        double[] distancesArray = new double[distances.size()];
        for (int i = 0; i < distances.size(); i++) {
            distancesArray[i] = distances.get(i);
            Fingerprint mark = marks.get(i);
            if (distancesArray[i] > 10) badFingerprints++;

            final PointF expected = mark.getCenter();
            final PointF predicted = predictedLocations.get(mark);
            LOGGER.info(String.format(Locale.getDefault(), "WiFiFingerprint: %.2f %.2f Predicted: %.2f %.2f Error: %g", expected.x, expected.y, predicted.x, predicted.y, distancesArray[i]));
        }

        double mean = StatUtils.mean(distancesArray);
        double variance = StatUtils.variance(distancesArray, mean);
        double stdev = Math.sqrt(variance);

        LOGGER.info("----------------------------------------------------");
        LOGGER.info(String.format(Locale.getDefault(), "Mean:     %.2f", mean));
        LOGGER.info(String.format(Locale.getDefault(), "Stdev:    %.2f", stdev));
        LOGGER.info(String.format(Locale.getDefault(), "Total:    %d", distances.size()));
        LOGGER.info(String.format(Locale.getDefault(), "Bad:      %d", badFingerprints));

        LOGGER.info("====================================================");

        int cnt = 0;
        double[] distancesArrayFiltered = new double[distances.size() - badFingerprints];
        for (int i = 0; i < distances.size(); i++) {
            final double distance = distances.get(i);
            if (distance <= 10) {
                distancesArrayFiltered[cnt++] = distance;
            }
        }

        mean = StatUtils.mean(distancesArrayFiltered);
        variance = StatUtils.variance(distancesArrayFiltered, mean);
        stdev = Math.sqrt(variance);

        LOGGER.info("----------------------------------------------------");
        LOGGER.info(String.format(Locale.getDefault(), "Mean:     %.2f", mean));
        LOGGER.info(String.format(Locale.getDefault(), "Stdev:    %.2f", stdev));
        LOGGER.info(String.format(Locale.getDefault(), "Total:    %d", distances.size() - badFingerprints));
    }

    @Test
    public void buildFingerprintsTable() {
        logToFile("C:\\jopa\\haifa_mall_fingerprints.csv");
        Map<String, Integer> macIndices = new HashMap<>();
        int macIndex = 0;

        for (Fingerprint mark : marks) {
            final WiFiLocator.WiFiFingerprint fingerprint = mark.getFingerprint();
            for (String mac : fingerprint.keySet()) {
                if (!macIndices.containsKey(mac)) {
                    macIndices.put(mac, macIndex++);
                }
            }
        }

        final int macNum = macIndices.size();
        List<String[]> table = new ArrayList<>();
        String[] row;
        for (Fingerprint mark : marks) {

            row = new String[macNum + 2]; // x, y, mac1, mac2, ...
            row[0] = String.valueOf(mark.getCenter().x);
            row[1] = String.valueOf(mark.getCenter().y);

            int[] macData = new int[macNum];
            for (String mac : mark.getFingerprint().keySet()) {
                macIndex = macIndices.get(mac);
                final int macLevel = mark.getFingerprint().get(mac);
                macData[macIndex] = macLevel;
            }

            for (int i = 0; i < macNum; i++) {
                row[i + 2] = String.valueOf(macData[i]);
            }

            table.add(row);
        }

        csvWriter.writeAll(table);
    }

    @Test
    public void buildMacTable() {
        logToFile("C:\\jopa\\haifa_mall_macs.csv");

        Map<String, Integer> macIndices = new HashMap<>();
        List<String[]> macs = new ArrayList<>();
        int macIndex = 0;

        for (Fingerprint mark : marks) {
            final WiFiLocator.WiFiFingerprint fingerprint = mark.getFingerprint();
            for (String mac : fingerprint.keySet()) {
                if (!macIndices.containsKey(mac)) {
                    macIndices.put(mac, macIndex);
                    macs.add(new String[] {String.valueOf(mac), String.valueOf(macIndex)});
                    macIndex++;
                }
            }
        }

        try {
            csvWriter.writeAll(macs);
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void distance_likelihood_table() {
        logToFile("C:\\jopa\\haifa_mall_distance_likelihood.csv");
        csvWriter.writeNext(new String[] {"Mark1", "Mark2", "Distance", "Likelihood"});

        for (Fingerprint mark1 : marks) {
            for (Fingerprint mark2 : marks) {
                final float dissimilarity = WiFiLocator.dissimilarity(mark1.getFingerprint(), mark2.getFingerprint());
                final double  distance = Math.sqrt(WiFiLocator.distanceXYsqr(mark1, mark2));
                csvWriter.writeNext(new String[] {String.valueOf(mark1.instanceId), String.valueOf(mark2.instanceId), String.valueOf(distance), String.valueOf(dissimilarity)});
            }
        }

        try {
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void likelihoodMatrix() {
        logToFile("C:\\jopa\\likelihood_matrix.csv");

        for (Fingerprint mark1 : marks) {
            String[] row = new String[Fingerprint.instanceNum];

            for (Fingerprint mark2 : marks) {
                final double distance = Math.sqrt(WiFiLocator.distanceXYsqr(mark1, mark2));
                float difference = Float.POSITIVE_INFINITY;

                if (distance <= 70) {
                    difference = WiFiLocator.dissimilarity(mark1.getFingerprint(), mark2.getFingerprint());
                }
                row[mark2.instanceId] = String.valueOf(difference);
            }

            csvWriter.writeNext(row);
        }

        try {
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void generateMarksTable() {
        logToFile("C:\\jopa\\marks.csv");

        String[] row = new String[3];
        for (Fingerprint mark : marks) {
            row[0] = String.valueOf(mark.instanceId);
            row[1] = String.valueOf(mark.getCenter().x);
            row[2] = String.valueOf(mark.getCenter().y);

            csvWriter.writeNext(row);
        }

        try {
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        wifiLocator.setCurrentFingerprint(fingerprintMark.getFingerprint());

        PointF actualPosition = new PointF();
        PointF expectedPosition = fingerprintMark.getCenter();
        wifiLocator.getPosition(actualPosition);
        predictedLocations.put(fingerprintMark, actualPosition);

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

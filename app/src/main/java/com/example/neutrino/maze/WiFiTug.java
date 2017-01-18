package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.WifiMark;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.TreeMap;
import org.apache.commons.math3.distribution.TDistribution;

/**
 * Created by Greg Stein on 9/25/2016.
 */
/* DR TODO:
 *  Implement currentFingerPrintList - like MovingAverageQueue - DONE
 *  Implement addtoFingerPrintList() API - DONE
   *  Implement getMostProbablyTrajectory() API for last MovingAverage based on
   *  algorithm as was discussed:
   *  for first scan find K most likely marks (based on distance)
   *  for each next scan - try to append K most likely to the previous list and discard
   *  the impossible ones (based on maximum distance requirement). Keep at most the top K.
   *  Continue until full trajectory exists.
   *  Play with: K, MaxDistThr, ListLength...
 */
public class WiFiTug implements TugOfWar.ITugger {

    private static final double CORR_THRESHOLD = 0.9;

    public static List<WifiMark> centroidMarks = null;
    // 20% of total marks
    public static final float CLOSEST_MARKS_PERCENTAGE = 0.2f;
    private static final int CENTROID_OPT_ITERATIONS = 3;
    private static final int FINGERPRINT_HISTORY_LENGTH = 10;

    private float mClosestMarksPercentage = CLOSEST_MARKS_PERCENTAGE;
    public void setClosestMarksPercentage(float percentage) {
        mClosestMarksPercentage = percentage;
    }

    public static final int MINIMUM_WIFI_MARKS = 10;
    private int mMinWifiMarks = MINIMUM_WIFI_MARKS;
    public void setMinimumWifiMarks(int minWifiMarks) {
        mMinWifiMarks = minWifiMarks;
    }

    // Yeah, fake class is so antipattern...
    public static class Fingerprint extends HashMap<String, Integer> {}
    public static class FingerprintHistory {
        private Queue<Fingerprint> mQueue;
        private int mLength;

        FingerprintHistory(int historyLength) {
            mQueue = new ArrayDeque<>(this.mLength = historyLength);
        }

        public void add(Fingerprint fingerprint) {
            if (mQueue.size() == mLength) {
                mQueue.remove();
            }

            if (!mQueue.add(fingerprint)) {
                throw new RuntimeException("Unable to add new fingerpting to queue.");
            }
        }

        public void clear() {
            mQueue.clear();
        }

    }

    public List<WifiMark> marks; //TODO: no encapsulation!
    public List<Wall> walls; //TODO: no encapsulation!
    public Fingerprint currentFingerprint = null;
    private FingerprintHistory currentHistory = null;

    void addToFingerprintHistory (Fingerprint fingerprint) {
        currentHistory.add(fingerprint);
    }

    public String buildWifiTable() {
        StringBuilder table = new StringBuilder(10 * marks.size() * marks.size());

        for(WifiMark outerMark : marks) {
            for (WifiMark innerMark : marks) {
                boolean visible = true;
                if (walls != null) {
                    for (Wall wall : walls) {
                        if (VectorHelper.linesIntersect(wall.getA(), wall.getB(), outerMark.getCenter(), innerMark.getCenter())) {
                            visible = false;
                            break;
                        }
                    }
                }

                if (visible) {
                    float likelihood = distance(outerMark.getFingerprint(), innerMark.getFingerprint());
                    float distance = (float) Math.sqrt(
                            Math.pow(outerMark.getCenter().x - innerMark.getCenter().x, 2) +
                                    Math.pow(outerMark.getCenter().y - innerMark.getCenter().y, 2));
                    table.append(likelihood).append(',').append(distance).append('\n');
                }
            }
        }

        return table.toString();
    }

    public String buildFingerprintTable() {
        StringBuilder table = new StringBuilder();

        for(WifiMark mark : marks) {
            for(Map.Entry<String, Integer> entry : mark.getFingerprint().entrySet()) {
                table.append(entry.getKey()).append(", ") // MAC
                        .append(mark.getCenter().x).append(", ")
                        .append(mark.getCenter().y).append(", ")
                        .append(entry.getValue()).append('\n'); // in decibel
            }
        }

        return table.toString();
    }

    public String buildWallsTable() {
        StringBuilder table = new StringBuilder();

        for (Wall wall : walls) {
            table.append(wall.getA().x).append(", ")
                    .append(wall.getA().y).append(", ")
                    .append(wall.getB().x).append(", ")
                    .append(wall.getB().y).append('\n');
        }

        return table.toString();
    }

    // Calculates euclidean distance in Decibell space
    public static float distance(Fingerprint actual, Fingerprint reference) {
        float distance = 0.0f;
        int bssidLevelDiff;

        if (actual == null || reference == null) return Float.MAX_VALUE;

        // Calculate difference between signal strengths
        for (String mac : actual.keySet()) {
            if (reference.containsKey(mac)) {
                bssidLevelDiff = actual.get(mac) - reference.get(mac);
            } else {
                bssidLevelDiff = actual.get(mac);
            }

            distance += Math.pow(bssidLevelDiff, 2);
        }

        for (String mac : reference.keySet()) {
            if (!actual.containsKey(mac)) {
                distance += Math.pow(reference.get(mac), 2);
            }
        }

        distance = (float) Math.sqrt(distance);
        // division by zero handling:
        if (distance == 0.0f) distance = Float.MIN_VALUE;
        return distance;
    }

    public static double correlation(Fingerprint actual, Fingerprint reference) {
        double sum = 0;
        double lenX = 0;
        double lenY = 0;
        int counter = 0;

        for (String mac : actual.keySet()) {
            int x = actual.get(mac);
            int y = -1000;
            if (reference.containsKey(mac)) {
                y = reference.get(mac);
                counter++;
            }
            double xD = Math.pow(10, x/20f);
            double yD = Math.pow(10, y/20f);
            System.out.println(String.format("x=%d y=%d", x, y));
            sum += xD * yD;
            lenX += xD * xD;
            lenY += yD * yD;
        }

        System.out.println(String.format("COUNTER: %d", counter));
        final double lenProduct = lenX * lenY;
        if (lenProduct == 0) return 0;
        return sum / Math.sqrt(lenProduct);
    }

    public static List<WifiMark> getSimilarMarks(List<WifiMark> wifiMarks, Fingerprint fingerprint, float percentage) {
        NavigableMap<Float, List<WifiMark>> sortedMarks = new TreeMap<>(); // sorted by distance to current fingerprint
        List<WifiMark> result = new ArrayList<>();

        for(WifiMark mark: wifiMarks) {
            Fingerprint markFingerprint = mark.getFingerprint();
            float distance = distance(fingerprint, markFingerprint);

            List<WifiMark> sameDistanceMarks = sortedMarks.get(distance);
            if (sameDistanceMarks == null) {
                sameDistanceMarks = new ArrayList<>();
                sortedMarks.put(distance, sameDistanceMarks);
            }
            sameDistanceMarks.add(mark);
        }

        int marksNum = (int) Math.ceil(wifiMarks.size() * percentage);
        int availableMinimumMarks = Math.min(MINIMUM_WIFI_MARKS, wifiMarks.size());
        marksNum = Math.max(marksNum, availableMinimumMarks);

        Map.Entry<Float, List<WifiMark>> entry = sortedMarks.firstEntry();
        while(result.size() < marksNum) {
            final int remainingMarks = marksNum - result.size();
            final List<WifiMark> marks = entry.getValue();

            if (marks.size() >= remainingMarks)
                result.addAll(marks.subList(0, remainingMarks));
            else
                result.addAll(marks);

            entry = sortedMarks.higherEntry(entry.getKey());
        }

        return result;
    }

    // Returns centroid
    public static boolean eliminateOutliers(List<WifiMark> wifiMarks, PointF mean) {
        final float ALPHA = 0.05f;

        int n = wifiMarks.size();

        if (n < 3) return false;

        TDistribution t = new TDistribution(n-2);
        float confidence = ALPHA / (2 * n);
        float criticalValue = (float) -t.inverseCumulativeProbability(confidence);

        // Centroid calculation
        mean.set(0, 0);
        for (WifiMark mark : wifiMarks) {
            mean.x += mark.getCenter().x;
            mean.y += mark.getCenter().y;
        }
        mean.x /= n;
        mean.y /= n;

        // Find standard deviation
        float standardDeviation = 0;
        for (WifiMark mark : wifiMarks) {
            standardDeviation += Math.pow(mark.getCenter().x - mean.x, 2) + Math.pow(mark.getCenter().y - mean.y, 2);
        }
        standardDeviation = (float) Math.sqrt(standardDeviation / n);

        // Grubb's test (https://en.wikipedia.org/wiki/Grubbs%27_test_for_outliers)
        boolean outlierFound = false;
        float grubbsTestThreshold = (float) ((n-1)/Math.sqrt(n) * criticalValue / Math.sqrt(n - 2 + Math.pow(criticalValue, 2)));
        for(Iterator<WifiMark> i = wifiMarks.iterator(); i.hasNext();) {
            WifiMark mark = i.next();
            PointF center = mark.getCenter();
            float g = (float) (Math.sqrt(Math.pow(center.x - mean.x, 2) + Math.pow(center.y - mean.y, 2))/standardDeviation);
            if (g > grubbsTestThreshold) {
                i.remove();
                outlierFound = true;
            }
        }

        return outlierFound;
    }

    public static void eliminateInvisibles(PointF currentPos, List<WifiMark> marks, List<Wall> walls) {
        for(Iterator<WifiMark> i = marks.iterator(); i.hasNext();) {
            WifiMark mark = i.next();

            for (Wall wall : walls) {
                if (VectorHelper.linesIntersect(wall.getA(), wall.getB(), currentPos, mark.getCenter())) {
                    i.remove();
                    break;
                }
            }
        }
    }

    // Get list of wifi marks with the same APs as a given fingerprint, by iteratively reducing the
    // full set of wifi marks until only those containing all APs in the fingerprint remain.
    // If at some point we get to an empty list, we assume a 'rogue' mark and re-use the list from
    // the previous step.
    public static List<WifiMark> getMarksWithSameAps(List<WifiMark> wifiMarks, Fingerprint fingerprint) {
        List<WifiMark> previousMarks = wifiMarks;
        List<WifiMark> relevantMarks = new ArrayList<>();

        for (String mac : fingerprint.keySet()) {
            for (WifiMark mark : previousMarks) {
                Fingerprint markFingerprint = mark.getFingerprint();
                if (markFingerprint.containsKey(mac)) {
                    relevantMarks.add(mark);
                }
            }
            if (!relevantMarks.isEmpty()) {  // If new list is non-empty, use it for next step
                previousMarks = relevantMarks;
                relevantMarks = new ArrayList<>();
            }
        }

        return previousMarks;
    }

    public static List<WifiMark> getMostCorrelatedMarks(Fingerprint fingerprint, List<WifiMark> marks) {
        List<WifiMark> correlatedMarks = new ArrayList<>();

        for(Iterator<WifiMark> i = marks.iterator(); i.hasNext();) {
            WifiMark mark = i.next();

            Fingerprint markFingerprint = mark.getFingerprint();
            double correlation = correlation(fingerprint, markFingerprint);
            if (correlation < CORR_THRESHOLD) {
                correlatedMarks.add(mark);
                i.remove();
            }
        }

        return correlatedMarks;
    }

    @Override
    public void getPosition(PointF position) {
        float x = 0, y = 0;
        float weight;
        float weightSum = 0;

        List<WifiMark> wifiMarks = getMarksWithSameAps(marks, currentFingerprint);
//        List<WifiMark> wifiMarks = getMostCorrelatedMarks(currentFingerprint, marks);
        PointF centroid = new PointF();
        boolean outlierFound;
        do {
            outlierFound = eliminateOutliers(wifiMarks, centroid);
        } while (outlierFound);

//        for (int i = 0; i < CENTROID_OPT_ITERATIONS; i++) {
//            if (centroid != null) {
//                eliminateInvisibles(centroid, wifiMarks, walls);
//            }

            for (WifiMark mark : wifiMarks) {
                Fingerprint markFingerprint = mark.getFingerprint();
                float distance = distance(currentFingerprint, markFingerprint);
                weight = 1 / distance;

                x += weight * mark.getCenter().x;
                y += weight * mark.getCenter().y;
                weightSum += weight;
            }

            position.set(x / weightSum, y / weightSum);
//            centroid = position;
//        }
//        String table = buildWifiTable();
//        String fingerprintTable = buildFingerprintTable();
//        String wallsTable = buildWallsTable();

        // For highlighting wifi marks used in location calculation
        centroidMarks = wifiMarks;
    }

    @Override
    public float getForce() {
        // TODO: calculate error
        return 0.5f;
    }
}

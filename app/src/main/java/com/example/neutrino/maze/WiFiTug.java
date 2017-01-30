package com.example.neutrino.maze;

import android.graphics.PointF;
import android.util.Pair;

import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.WifiMark;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;

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
    private static final float WIFI_DISTANCE_CANDIDATE_PERCENTAGE = 0.1f;
    private static final int MAX_NUM_CANDIDATES = 5;
    private static final float MAX_SQRDISTANCE_TWO_WIFIMARKS = 100;   // maximum allowed sqrdistance

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
    public static class FingerprintHistory implements Iterable<Fingerprint> {
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

        public int size() {
            return mLength;
        }

        @Override
        public Iterator<Fingerprint> iterator() {
            return (mQueue.iterator());
        }
    }

    public List<WifiMark> marks; //TODO: no encapsulation!
    public List<Wall> walls; //TODO: no encapsulation!
    public Fingerprint currentFingerprint = null;
    public FingerprintHistory currentHistory = null;

    void addToFingerprintHistory (Fingerprint fingerprint) {
        currentHistory.add(fingerprint);
    }

    // Mars
    public float getAverageDistanceTo(Fingerprint fingerprint) {
        int nMarks = marks.size();
        float distance = 0f;
        for (WifiMark mark: marks) {
            distance += distance(mark.getFingerprint(),fingerprint);
        }
        return (distance /= nMarks);
    }

    public float[] getSortedDistanceArray(Fingerprint fingerprint, Float avg) {
        int nMarks = marks.size();
        float distance = 0f;
        float[] distanceArray = new float[nMarks];
        for (int i = 0 ; i < nMarks; i++) {
            distanceArray[i] = distance(marks.get(i).getFingerprint(),fingerprint);
            distance += distanceArray[i];
        }
        Arrays.sort(distanceArray);
        if (avg != null) {
            avg = (distance / nMarks);
        }
        return distanceArray;
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

    /* Returns the most probable trajectory of locations, given the current stored history of
     * Wifi fingerprints.
     */
    public void getMostProbableTrajectory(List<PointF> trajectory)
    {
        float minDistance, maxDistance, distanceRange, maxViableDistance;
        TreeSet<LinkableWifiMark> candidatesThisRound = new TreeSet<>();
        TreeSet<LinkableWifiMark> candidatesList = new TreeSet<>();
        TreeSet<LinkableWifiMark> candidatePairs = new TreeSet<>();
        boolean firstRound = true;
        int i, j;
        LinkableWifiMark wmark, lwmark;

        for (Fingerprint fingerprint: currentHistory) {
            List<WifiMark> wifiMarks = getMarksWithSameAps(marks, fingerprint);
            minDistance = 0; maxDistance = Float.MAX_VALUE;

            // First phase - get min and max distance
            for (WifiMark mark: wifiMarks) {
                float distance = distance(fingerprint, mark.getFingerprint());
                if (distance < minDistance)
                    minDistance = distance;
                if (distance > maxDistance)
                    maxDistance = distance;
            }
            distanceRange = maxDistance - minDistance;
            maxViableDistance = minDistance + WIFI_DISTANCE_CANDIDATE_PERCENTAGE * distanceRange;

            // Second phase - take only viable candidates (from top CANDIDATE_PERCENTAGE of lowest distance)
            for (WifiMark mark: wifiMarks) {
                float distance = distance(fingerprint, mark.getFingerprint());
                if (distance <= maxViableDistance) {
                    candidatesThisRound.add(new LinkableWifiMark(mark, distance));
                }
            }

            // Third phase - of all viable candidates take only the top MAX_NUM_CANDIDATES
            // and try merging each of them with existing candidate lists (except for first round)
            for (i = 0; i < MAX_NUM_CANDIDATES; i++) {
                wmark = candidatesThisRound.pollFirst();
                if (wmark == null)
                    break;
                if (firstRound) {   // No previous list - make list out of top candidates
                    candidatesList.add(wmark);
                } else {    // Previous list of candidate chains exists
                    for (LinkableWifiMark storedmark : candidatesList) {
                        if (distanceXYsqr(wmark.mark, storedmark.mark) <= MAX_SQRDISTANCE_TWO_WIFIMARKS) {
                            lwmark = new LinkableWifiMark(wmark.mark);
                            lwmark.parent = storedmark;
                            lwmark.totalCost = storedmark.totalCost + wmark.totalCost;
                            candidatePairs.add(lwmark); // Add only possible chain continuations
                        }
                    }
                }
            }

            // Fourth phase - keep only top MAX_NUM_CANDIDATES of the newly formed chains (except first round)
            if (!firstRound) {
                candidatesList.clear();
                for (j = 0; j < MAX_NUM_CANDIDATES; j++) {  // Take top candidates from new chains
                    lwmark = candidatePairs.pollFirst();
                    if (lwmark == null)
                        break;
                    candidatesList.add(lwmark); // Add to candidates list
                }
            }
            firstRound = false;
        }

        trajectory.clear();
        lwmark = candidatesList.first();

        while (lwmark != null) {    // Add points to list in reverse order
            PointF point = lwmark.mark.getCenter();
            trajectory.add(0, point);
            lwmark = lwmark.parent;
        }

        candidatesList.clear(); // NAHUA ???
    }

    @Override
    public float getForce() {
        // TODO: calculate error
        return 0.5f;
    }

    /* A WifiMark wrapper with the following capabilities:
     * - Link to another LinkableWifiMark to form chains of marks
     * - Keep total cost function (for evaluating quality of chains)
     */
    public static class LinkableWifiMark implements Comparable<LinkableWifiMark>{
        public LinkableWifiMark parent = null;
        public WifiMark mark = null;
        public float totalCost = 0;

        @Override
        public int compareTo(LinkableWifiMark another) {
            if (totalCost < another.totalCost)
                return -1;
            if (totalCost > another.totalCost)
                return 1;
            return 0;
        }

        LinkableWifiMark(WifiMark mark) {
            this.mark = mark;
        }

        LinkableWifiMark(WifiMark mark, float cost) {
            this.mark = mark;
            totalCost = cost;
        }
    }

    public static float distanceXYsqr (Footprint a, Footprint b) {
        return (float)( Math.pow((a.getCenter().x - b.getCenter().x), 2) + Math.pow((a.getCenter().y - b.getCenter().y), 2) );
    }
}

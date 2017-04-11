package com.example.neutrino.maze;

import android.graphics.PointF;
import android.support.v4.util.Pair;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.rendering.VectorHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Created by Greg Stein on 9/25/2016.
 */
/* DR TODO:
 *  Implement currentFingerPrintList - like MovingAverageQueue - DONE
 *  Implement addtoFingerPrintList() API - DONE
 *  Implement addtoFingerPrintList() API - DONE
   *  Implement getMostProbablyTrajectory() API for last MovingAverage based on
   *  algorithm as was discussed:
   *  for first scan find K most likely marks (based on dissimilarity)
   *  for each next scan - try to append K most likely to the previous list and discard
   *  the impossible ones (based on maximum dissimilarity requirement). Keep at most the top K.
   *  Continue until full trajectory exists.
   *  Play with: K, MaxDistThr, ListLength...
 */
public class WiFiTug implements TugOfWar.ITugger {

    private static final double CORR_THRESHOLD = 0.9;
    private static final float WIFI_DISTANCE_CANDIDATE_PERCENTAGE = 0.5f;
    private static final int MAX_NUM_CANDIDATES = 5;
    private static final float MAX_SQRDISTANCE_TWO_WIFIMARKS = 100;   // maximum allowed sqrdistance
    private static final int RSS_OFFSET = 100;
    public static final int MAX_REGRESSION_DISTANCE_BETWEEN_MARKS = 70;
    public static final double MINIMUM_CORRELATION_COEFF = 0.2;
    public static final int MIN_RSS_TO_COUNT = -75;
    public static final double NEIGHBOUR_MIN_SCORE = 0;

    private static WiFiTug instance = new WiFiTug();
    public static WiFiTug getInstance() {
        return instance;
    }
    private WiFiTug() {}

    public static List<Fingerprint> centroidMarks = null;
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

    public void setCurrentFingerprint(WiFiFingerprint fingerprint) {
        this.currentWiFiFingerprint = fingerprint;
    }

    // Yeah, fake class is so antipattern...
    public static class WiFiFingerprint extends HashMap<String, Integer> {}
    public static class FingerprintHistory implements Iterable<WiFiFingerprint> {
        private Queue<WiFiFingerprint> mQueue;
        private int mLength;

        FingerprintHistory(int historyLength) {
            mQueue = new ArrayDeque<>(this.mLength = historyLength);
        }

        public void add(WiFiFingerprint fingerprint) {
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
        public Iterator<WiFiFingerprint> iterator() {
            return (mQueue.iterator());
        }
    }

    public List<Fingerprint> marks; //TODO: no encapsulation!
    public List<Wall> walls; //TODO: no encapsulation!
    private WiFiFingerprint currentWiFiFingerprint = null;
    public FingerprintHistory currentHistory = null;

    void addToFingerprintHistory (WiFiFingerprint fingerprint) {
        currentHistory.add(fingerprint);
    }

    // Mars - ахтыссука
    public float getAverageDistanceTo(WiFiFingerprint fingerprint) {
        int nMarks = marks.size();
        float distance = 0f;
        for (Fingerprint mark: marks) {
            distance += dissimilarity(mark.getFingerprint(),fingerprint);
        }
        return (distance /= nMarks);
    }

    public float[] getSortedDistanceArray(WiFiFingerprint fingerprint, Float avg) {
        int nMarks = marks.size();
        float distance = 0f;
        float[] distanceArray = new float[nMarks];
        for (int i = 0 ; i < nMarks; i++) {
            distanceArray[i] = dissimilarity(marks.get(i).getFingerprint(),fingerprint);
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

        for(Fingerprint outerMark : marks) {
            for (Fingerprint innerMark : marks) {
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
                    float difference = dissimilarity(outerMark.getFingerprint(), innerMark.getFingerprint());
                    float distance = (float) Math.sqrt(
                            Math.pow(outerMark.getCenter().x - innerMark.getCenter().x, 2) +
                                    Math.pow(outerMark.getCenter().y - innerMark.getCenter().y, 2));
                    table.append(difference).append(',').append(distance).append('\n');
                }
            }
        }

        return table.toString();
    }

    public String buildFingerprintTable() {
        StringBuilder table = new StringBuilder();

        for(Fingerprint mark : marks) {
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

    // Calculates euclidean distance in Decibel space
    public static float dissimilarity(WiFiFingerprint actual, WiFiFingerprint reference) {
        float difference = 0.0f;
        int distanceSq = 0;
        int bssidLevelDiff;

        if (actual == null || reference == null) return Float.MAX_VALUE;

        // Calculate dissimilarity between signal strengths
        for (String mac : actual.keySet()) {
            if (reference.containsKey(mac)) {
                bssidLevelDiff = actual.get(mac) - reference.get(mac);
            } else {
                bssidLevelDiff = actual.get(mac) + RSS_OFFSET;
            }

            distanceSq += bssidLevelDiff * bssidLevelDiff;
        }

        for (String mac : reference.keySet()) {
            if (!actual.containsKey(mac)) {
                bssidLevelDiff = reference.get(mac) + RSS_OFFSET;
                distanceSq += bssidLevelDiff * bssidLevelDiff;
            }
        }

        difference = (float) Math.sqrt(distanceSq);
        // division by zero handling:
        if (difference == 0.0f) difference = Float.MIN_VALUE;
        return difference;
    }

    public static double correlation(WiFiFingerprint actual, WiFiFingerprint reference) {
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

    public static List<Fingerprint> getSimilarMarks(List<Fingerprint> fingerprints, WiFiFingerprint fingerprint, float percentage) {
        NavigableMap<Float, List<Fingerprint>> sortedMarks = new TreeMap<>(); // sorted by distance to current fingerprint
        List<Fingerprint> result = new ArrayList<>();

        for(Fingerprint mark: fingerprints) {
            WiFiFingerprint markWiFiFingerprint = mark.getFingerprint();
            float distance = dissimilarity(fingerprint, markWiFiFingerprint);

            List<Fingerprint> sameDistanceMarks = sortedMarks.get(distance);
            if (sameDistanceMarks == null) {
                sameDistanceMarks = new ArrayList<>();
                sortedMarks.put(distance, sameDistanceMarks);
            }
            sameDistanceMarks.add(mark);
        }

        int marksNum = (int) Math.ceil(fingerprints.size() * percentage);
        int availableMinimumMarks = Math.min(MINIMUM_WIFI_MARKS, fingerprints.size());
        marksNum = Math.max(marksNum, availableMinimumMarks);

        Map.Entry<Float, List<Fingerprint>> entry = sortedMarks.firstEntry();
        while(result.size() < marksNum) {
            final int remainingMarks = marksNum - result.size();
            final List<Fingerprint> marks = entry.getValue();

            if (marks.size() >= remainingMarks)
                result.addAll(marks.subList(0, remainingMarks));
            else
                result.addAll(marks);

            entry = sortedMarks.higherEntry(entry.getKey());
        }

        return result;
    }

    // Returns centroid
    public static boolean eliminateOutliers(List<Fingerprint> fingerprints, PointF mean) {
        final float ALPHA = 0.05f;

        int n = fingerprints.size();

        if (n < 3) return false;

        TDistribution t = new TDistribution(n-2);
        float confidence = ALPHA / (2 * n);
        float criticalValue = (float) -t.inverseCumulativeProbability(confidence);

        // Centroid calculation
        mean.set(0, 0);
        for (Fingerprint mark : fingerprints) {
            mean.x += mark.getCenter().x;
            mean.y += mark.getCenter().y;
        }
        mean.x /= n;
        mean.y /= n;

        // Find standard deviation
        float standardDeviation = 0;
        for (Fingerprint mark : fingerprints) {
            standardDeviation += Math.pow(mark.getCenter().x - mean.x, 2) + Math.pow(mark.getCenter().y - mean.y, 2);
        }
        standardDeviation = (float) Math.sqrt(standardDeviation / n);

        // Grubb's test (https://en.wikipedia.org/wiki/Grubbs%27_test_for_outliers)
        boolean outlierFound = false;
        float grubbsTestThreshold = (float) ((n-1)/Math.sqrt(n) * criticalValue / Math.sqrt(n - 2 + Math.pow(criticalValue, 2)));
        for(Iterator<Fingerprint> i = fingerprints.iterator(); i.hasNext();) {
            Fingerprint mark = i.next();
            PointF center = mark.getCenter();
            float g = (float) (Math.sqrt(Math.pow(center.x - mean.x, 2) + Math.pow(center.y - mean.y, 2))/standardDeviation);
            if (g > grubbsTestThreshold) {
                i.remove();
                outlierFound = true;
            }
        }

        return outlierFound;
    }

    public static void eliminateInvisibles(PointF currentPos, List<Fingerprint> marks, List<Wall> walls) {
        for(Iterator<Fingerprint> i = marks.iterator(); i.hasNext();) {
            Fingerprint mark = i.next();

            for (Wall wall : walls) {
                if (VectorHelper.linesIntersect(wall.getA(), wall.getB(), currentPos, mark.getCenter())) {
                    i.remove();
                    break;
                }
            }
        }
    }

    public static Set<String> getApsWithMinRSS(WiFiFingerprint fp, int minRSS) {
        Set<String> strongAps = new HashSet<>();
        if (fp == null)
            return strongAps;

        for (String mac : fp.keySet()) {
            if (fp.get(mac) > minRSS) {
                strongAps.add(mac);
            }
        }

        return strongAps;
    }

    public static int score(WiFiFingerprint fingerprint, Fingerprint refFp) {
        Set<String> fingerprintAps = getApsWithMinRSS(fingerprint, MIN_RSS_TO_COUNT);
        Set<String> refFpAps = getApsWithMinRSS(refFp.getFingerprint(), MIN_RSS_TO_COUNT);

        Set<String> intersection = new HashSet<>(fingerprintAps); // use the copy constructor
        intersection.retainAll(refFpAps);

        fingerprintAps.removeAll(intersection);
        refFpAps.removeAll(intersection);

        return intersection.size() * 2 - fingerprintAps.size() - refFpAps.size();
    }

    public static List<Fingerprint> getMarksWithSameAps2(List<Fingerprint> fingerprints, WiFiFingerprint fingerprint) {
        List<Fingerprint> result = new ArrayList<>();

        for (Fingerprint f : fingerprints) {
            final double score = score(fingerprint, f);
//            System.out.println("score: " + score);
            if (score > NEIGHBOUR_MIN_SCORE) {
                result.add(f);
            }
        }

        return result;
    }

    // Get list of wifi marks with the same APs as a given fingerprint, by iteratively reducing the
    // full set of wifi marks until only those containing all APs in the fingerprint remain.
    // If at some point we get to an empty list, we assume a 'rogue' mark and re-use the list from
    // the previous step.
    public static List<Fingerprint> getMarksWithSameAps(List<Fingerprint> fingerprints, WiFiFingerprint fingerprint) {
        List<Fingerprint> previousMarks = fingerprints;
        List<Fingerprint> relevantMarks = new ArrayList<>();

        for (String mac : fingerprint.keySet()) {
            for (Fingerprint mark : previousMarks) {
                WiFiFingerprint wiFiFingerprint = mark.getFingerprint();
                if (wiFiFingerprint.containsKey(mac)) {
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

    public static List<Fingerprint> getMostCorrelatedMarks(WiFiFingerprint fingerprint, List<Fingerprint> marks) {
        List<Fingerprint> correlatedMarks = new ArrayList<>();

        for(Iterator<Fingerprint> i = marks.iterator(); i.hasNext();) {
            Fingerprint mark = i.next();

            WiFiFingerprint wiFiFingerprint = mark.getFingerprint();
            double correlation = correlation(fingerprint, wiFiFingerprint);
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

        List<Fingerprint> fingerprints = getMarksWithSameAps2(marks, currentWiFiFingerprint);
//        List<Fingerprint> fingerprints = getMostCorrelatedMarks(currentWiFiFingerprint, marks);
//        PointF centroid = new PointF();
//        boolean outlierFound;
//        do {
//            outlierFound = eliminateOutliers(fingerprints, centroid);
//        } while (outlierFound);

//        for (int i = 0; i < CENTROID_OPT_ITERATIONS; i++) {
//            if (centroid != null) {
//                eliminateInvisibles(centroid, fingerprints, walls);
//            }

            for (Fingerprint mark : fingerprints) {
                WiFiFingerprint wiFiFingerprint = mark.getFingerprint();
                float distance = dissimilarity(currentWiFiFingerprint, wiFiFingerprint);
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
        centroidMarks = fingerprints;
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

        for (WiFiFingerprint fingerprint: currentHistory) {
            List<Fingerprint> fingerprints = getMarksWithSameAps(marks, fingerprint);
            maxDistance = 0; minDistance = Float.MAX_VALUE;

            // First phase - get min and max dissimilarity
            for (Fingerprint mark: fingerprints) {
                float difference = dissimilarity(fingerprint, mark.getFingerprint());
                if (difference < minDistance)
                    minDistance = difference;
                if (difference > maxDistance)
                    maxDistance = difference;
            }
            distanceRange = maxDistance - minDistance;
            maxViableDistance = minDistance + WIFI_DISTANCE_CANDIDATE_PERCENTAGE * distanceRange;

            candidatesThisRound.clear();
            candidatePairs.clear();

            // Second phase - take only viable candidates (from top CANDIDATE_PERCENTAGE of lowest dissimilarity)
            for (Fingerprint mark: fingerprints) {
                float difference = dissimilarity(fingerprint, mark.getFingerprint());
                if (difference <= maxViableDistance) {
                    candidatesThisRound.add(new LinkableWifiMark(mark, difference));
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
        lwmark = candidatesList.pollFirst();

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

    /* A Fingerprint wrapper with the following capabilities:
     * - Link to another LinkableWifiMark to form chains of marks
     * - Keep total cost function (for evaluating quality of chains)
     */
    public static class LinkableWifiMark implements Comparable<LinkableWifiMark>{
        public LinkableWifiMark parent = null;
        public Fingerprint mark = null;
        public float totalCost = 0;

        @Override
        public int compareTo(LinkableWifiMark another) {
            if (totalCost < another.totalCost)
                return -1;
            if (totalCost > another.totalCost)
                return 1;
            return 0;
        }

        LinkableWifiMark(Fingerprint mark) {
            this.mark = mark;
        }

        LinkableWifiMark(Fingerprint mark, float cost) {
            this.mark = mark;
            totalCost = cost;
        }
    }

    public static float distanceXYsqr (Footprint a, Footprint b) {
        return (float)( Math.pow((a.getCenter().x - b.getCenter().x), 2) + Math.pow((a.getCenter().y - b.getCenter().y), 2) );
    }

    public void setFingerprints(Iterable<Fingerprint> fingerprints) {

    }

    public int lessThen10marks = 0;
    public void getPositionStat(PointF pos) {
        List<Fingerprint> fingerprints = getMarksWithSameAps2(marks, currentWiFiFingerprint);
        int fingerprintsNum = fingerprints.size();
        // Disable this method.
        if (fingerprintsNum < 5) {
            getPosition(pos);
            lessThen10marks++;
            return;
        }

        Pair<Float, Float> regressionCoeffs = getTrendlineCoeffs(fingerprints);
        float slope = regressionCoeffs.first;
        float intercept = regressionCoeffs.second;


        float[] distances = new float[fingerprintsNum];
        for (int i = 0; i < fingerprintsNum; i++) {
            distances[i] = dissimilarity(fingerprints.get(i).getFingerprint(), currentWiFiFingerprint) * slope + intercept;
        }

        final float x1 = fingerprints.get(0).getCenter().x;
        final float y1 = fingerprints.get(0).getCenter().y;
        final float d1 = distances[0];
        double[][] matrixData = new double[fingerprintsNum-1][2];
        double[] vectorData = new double[fingerprintsNum-1];

        for (int i = 1; i < fingerprintsNum; i++) {
            final float xi = fingerprints.get(i).getCenter().x;
            final float yi = fingerprints.get(i).getCenter().y;
            final float di = distances[i];
            matrixData[i-1][0] = 2 * (xi - x1);
            matrixData[i-1][1] = 2 * (yi - y1);
            vectorData[i-1] =  xi*xi - x1*x1 + yi*yi - y1*y1 + d1*d1 - di*di;
        }
        RealMatrix A = MatrixUtils.createRealMatrix(matrixData);
        RealVector b = MatrixUtils.createRealVector(vectorData);
        DecompositionSolver solver = new QRDecomposition(A).getSolver();
        RealVector solution = solver.solve(b);
        pos.set((float)solution.getEntry(0), (float)solution.getEntry(1));
    }

    private Pair<Float, Float> getTrendlineCoeffs(Iterable<Fingerprint> fingerprints) {
        Pair<Float, Float> regressionCoefficients = null;

        SimpleRegression regression = new SimpleRegression();

        for (Fingerprint markA : fingerprints) {
            for (Fingerprint markB : fingerprints) {
                final float difference = dissimilarity(markA.getFingerprint(), markB.getFingerprint());
                final float distance = (float) Math.sqrt(distanceXYsqr(markA, markB));
                if (distance < MAX_REGRESSION_DISTANCE_BETWEEN_MARKS) {
//                    System.out.println(String.format("%.4f, %.4f", distance, dissimilarity));
                    regression.addData(difference, distance);
                }
            }
        }

        double R2 = regression.getRSquare();
//        if (R2 > MINIMUM_CORRELATION_COEFF) {
            float interceptionPoint = (float) regression.getIntercept();
            float slope = (float) regression.getSlope();
            regressionCoefficients = new Pair<>(slope, interceptionPoint);
//        }

        return regressionCoefficients;
    }
}

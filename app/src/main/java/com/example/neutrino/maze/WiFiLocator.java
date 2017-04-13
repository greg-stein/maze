package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.Footprint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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

/**
 * Created by Greg Stein on 9/25/2016.
 */
public class WiFiLocator {

    private static final float WIFI_DISTANCE_CANDIDATE_PERCENTAGE = 0.5f;
    private static final int MAX_NUM_CANDIDATES = 5;
    private static final float MAX_SQRDISTANCE_TWO_WIFIMARKS = 100;   // maximum allowed sqrdistance
    private static final int RSS_OFFSET = 100;
    public static final int MIN_RSS_TO_COUNT = -75;
    public static final double NEIGHBOUR_MIN_SCORE = -1;

    private static WiFiLocator instance = new WiFiLocator();
    public static WiFiLocator getInstance() {
        return instance;
    }
    private WiFiLocator() {}

    public static List<Fingerprint> centroidMarks = null;

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

    private List<Fingerprint> mFingerprints;
    private WiFiFingerprint currentWiFiFingerprint = null;
    public FingerprintHistory currentHistory = null; //TODO: no encapsulation!

    void addToFingerprintHistory(WiFiFingerprint fingerprint) {
        currentHistory.add(fingerprint);
    }

    public void setFingerprintsMap(List<Fingerprint> map) {
        mFingerprints = map;
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

    public static Set<String> getApsWithMinRSS(WiFiFingerprint fp, int minRSS) {
        Set<String> strongAps = new HashSet<>();
        Set<String> weakAps = new HashSet<>();

        if (fp == null)
            return strongAps;

        for (String mac : fp.keySet()) {
            if (fp.get(mac) > minRSS) {
                strongAps.add(mac);
            } else {
                weakAps.add(mac);
            }
        }

        // If not enough strong Aps, use weak as well
        if (strongAps.size() < 3) {
            strongAps.addAll(weakAps);
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
        NavigableMap<Integer, List<Fingerprint>> fingerprintsByScore = new TreeMap<>(Collections.<Integer>reverseOrder());

        for (Fingerprint f : fingerprints) {
            final int score = score(fingerprint, f);

            if (score > NEIGHBOUR_MIN_SCORE) {
                List<Fingerprint> list = fingerprintsByScore.get(score);
                if (list == null) {
                    list = new ArrayList<>();
                    fingerprintsByScore.put(score, list);
                }
                list.add(f);
            }
        }

        List<Fingerprint> bestFingerprints = new ArrayList<>();
        for (List<Fingerprint> goodFingerprints : fingerprintsByScore.values()) {
            bestFingerprints.addAll(goodFingerprints);
            if (bestFingerprints.size() > 0) break;
        }

        return bestFingerprints;
    }

    // Get list of wifi mFingerprints with the same APs as a given fingerprint, by iteratively reducing the
    // full set of wifi mFingerprints until only those containing all APs in the fingerprint remain.
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

    // TODO: Wipe out the deprecated method
    public PointF getLocation(WiFiFingerprint fingerprint) {
        currentWiFiFingerprint = fingerprint;
        PointF location = new PointF();
        getPosition(location);

        return location;
    }

    /**
     * This method is deprecated, use getLocation(WiFiFingerprint) instead
     * @param position
     */
    @Deprecated
    public void getPosition(PointF position) {
        float x = 0, y = 0;
        float weight;
        float weightSum = 0;

        List<Fingerprint> fingerprints = getMarksWithSameAps2(mFingerprints, currentWiFiFingerprint);

        for (Fingerprint mark : fingerprints) {
            WiFiFingerprint wiFiFingerprint = mark.getFingerprint();
            float distance = dissimilarity(currentWiFiFingerprint, wiFiFingerprint);
            weight = 1 / distance;

            x += weight * mark.getCenter().x;
            y += weight * mark.getCenter().y;
            weightSum += weight;
        }

        position.set(x / weightSum, y / weightSum);

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

        for (WiFiFingerprint fingerprint : currentHistory) {
            List<Fingerprint> fingerprints = getMarksWithSameAps(mFingerprints, fingerprint);
            maxDistance = 0;
            minDistance = Float.MAX_VALUE;

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

    public static float distanceXYsqr(Footprint a, Footprint b) {
        return (float) (Math.pow((a.getCenter().x - b.getCenter().x), 2) + Math.pow((a.getCenter().y - b.getCenter().y), 2));
    }
}
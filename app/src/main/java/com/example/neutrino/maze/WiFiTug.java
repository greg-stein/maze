package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.WifiMark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.apache.commons.math3.distribution.TDistribution;

/**
 * Created by Greg Stein on 9/25/2016.
 */
public class WiFiTug implements TugOfWar.ITugger {

    // 20% of total marks
    public static final float CLOSEST_MARKS_PERCENTAGE = 0.2f;
    private float mClosestMarksPercentage = CLOSEST_MARKS_PERCENTAGE;
    public void setClosestMarksPercentage(float percentage) {
        mClosestMarksPercentage = percentage;
    }

    // Yeah, fake class is so antipattern...
    public static class Fingerprint extends HashMap<String, Integer> {}
    public List<WifiMark> marks; //TODO: no encapsulation!
    public Fingerprint currentFingerprint = null;

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

        // Return squared distance as used in calculations
        distance = (float) Math.sqrt(distance);
        // division by zero handling:
        if (distance == 0.0f) distance = Float.MIN_VALUE;
        return distance;
    }

    public static List<WifiMark> getSimilarMarks(List<WifiMark> wifiMarks, Fingerprint fingerprint, float percentage) {
        NavigableMap<Float, WifiMark> sortedMarks = new TreeMap<>(); // sorted by distance to current fingerprint
        List<WifiMark> result = new ArrayList<>();

        for(WifiMark mark: wifiMarks) {
            Fingerprint markFingerprint = mark.getFingerprint();
            float distance = distance(fingerprint, markFingerprint);
            sortedMarks.put(distance, mark);
        }

        int marksNum = (int) Math.ceil(wifiMarks.size() * percentage);
        Map.Entry<Float, WifiMark> entry = sortedMarks.firstEntry();
        for(int markCount = 0; markCount < marksNum; markCount++) {
            result.add(entry.getValue());
            entry = sortedMarks.higherEntry(entry.getKey());
        }

        return result;
    }

    public static void eliminateOutliers(List<WifiMark> wifiMarks) {
        final float ALPHA = 0.05f;

        int n = wifiMarks.size();

        if (n < 3) return;

        TDistribution t = new TDistribution(n-2);
        float confidence = ALPHA / (2 * n);
        float criticalValue = (float) -t.inverseCumulativeProbability(confidence);

        // Centroid calculation
        PointF mean = new PointF();
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
        float grubbsTestThreshold = (float) ((n-1)/Math.sqrt(n) * criticalValue / Math.sqrt(n - 2 + Math.pow(criticalValue, 2)));
        for(Iterator<WifiMark> i = wifiMarks.iterator(); i.hasNext();) {
            WifiMark mark = i.next();
            PointF center = mark.getCenter();
            float g = (float) (Math.sqrt(Math.pow(center.x - mean.x, 2) + Math.pow(center.y - mean.y, 2))/standardDeviation);
            if (g > grubbsTestThreshold) {
                i.remove();
            }
        }
    }

    @Override
    public void getPosition(PointF position) {
        float x = 0, y = 0;
        float weight;
        float weightSum = 0;

        List<WifiMark> wifiMarks = getSimilarMarks(marks, currentFingerprint, mClosestMarksPercentage);
        eliminateOutliers(wifiMarks);

        for(WifiMark mark: wifiMarks) {
            Fingerprint markFingerprint = mark.getFingerprint();
            float distance = distance(currentFingerprint, markFingerprint);
            weight = 1/distance;

            x += weight * mark.getCenter().x;
            y += weight * mark.getCenter().y;
            weightSum += weight;
        }

        position.set(x / weightSum, y / weightSum);
    }

    @Override
    public float getForce() {
        // TODO: calculate error
        return 0.5f;
    }
}

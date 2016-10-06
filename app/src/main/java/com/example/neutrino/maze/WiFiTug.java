package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.WifiMark;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Greg Stein on 9/25/2016.
 */
public class WiFiTug implements TugOfWar.ITugger {
    // Yeah, fake class is so antipattern...
    public static class Fingerprint extends HashMap<String, Integer> {}
    public List<WifiMark> marks; //TODO: no encapsulation!
    public Fingerprint currentFingerprint = null;

    // Calculates euclidean distance in DB space
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

        if (distance == 0.0f) distance = Float.MIN_VALUE;
        return distance;
    }

    @Override
    public void getPosition(PointF position) {
        float x = 0, y = 0;
        float weight;
        float weightSum = 0;

        for(WifiMark mark: marks) {
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

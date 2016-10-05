package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.WifiMark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg Stein on 9/25/2016.
 */
public class WiFiTug implements TugOfWar.ITugger {
    // Yeah, fake class is so antipattern...
    public static class Fingerprint extends HashMap<String, Integer> {}
    public List<WifiMark> marks; //TODO: no encapsulation!

    // Calculates euclidean distance in DB space
    public static float distance(Fingerprint actual, Fingerprint reference) {
        float distance = 0.0f;
        int bssidLevelDiff;

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

        return (float) Math.sqrt(distance);
    }

    @Override
    public void getPosition(PointF position) {
        // STUB: TODO
    }

    @Override
    public float getForce() {
        // STUB: TODO
        return 0;
    }
}

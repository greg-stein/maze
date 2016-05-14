package com.example.neutrino.maze;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by neutrino on 4/21/2016.
 */
public class Cell implements Serializable {
    public static final int MAX_SIGNALS = 100;

    private Map<String, Integer> mSignals;

    public Cell() {
        mSignals = new HashMap<String, Integer>(MAX_SIGNALS);
    }

    public boolean hasData() {
        return !mSignals.isEmpty();
    }

    public void addSignal(String macAddress, int level) {
        mSignals.put(macAddress, level);
    }

    public void clear() {
        mSignals.clear();
    }

    public int compareTo(Cell another) {
        int result = 0;

        if (another == null) {
            return Integer.MAX_VALUE; // indicate "very different"
        }

        // Calculate difference between signal strengths
        for (String key : mSignals.keySet()) {
            if (another.mSignals.containsKey(key)) {
                result += Math.abs(mSignals.get(key) - another.mSignals.get(key));
            } else {
                result += mSignals.get(key);
            }
        }
        for (String key : another.mSignals.keySet()) {
            if (!mSignals.containsKey(key)) {
                result += another.mSignals.get(key);
            }
        }
        return result;    }
}

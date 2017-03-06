 package com.example.neutrino.maze;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg Stein on 10/25/2016.
 */
public class MovingAverageQueue extends MovingAverageQueueBase<List<ScanResult>> {
    private WiFiTug.Fingerprint mSumFingerprint = new WiFiTug.Fingerprint();
    // Maintain map of counters per MAC, counter++ in case scanResult.level present
    private Map<String, Integer> counters = new HashMap<>();

    @Deprecated
    private final List<ScanResult> mAverageScans = new ArrayList<>();

    public MovingAverageQueue(int windowSize) {
        super(windowSize);
    }

    @Override
    protected void addToSum(List<ScanResult> scanResults) {
        for(ScanResult scanResult : scanResults) {
            Integer oSum = mSumFingerprint.get(scanResult.BSSID);

            if (oSum != null) {
                int iSum = oSum; // unbox
                iSum += scanResult.level;
                mSumFingerprint.put(scanResult.BSSID, iSum);
                int counter = counters.get(scanResult.BSSID);
                counters.put(scanResult.BSSID, counter + 1);
            } else {
                mSumFingerprint.put(scanResult.BSSID, scanResult.level);
                counters.put(scanResult.BSSID, 1);
            }
        }
    }

    @Override
    protected void subtractFromSum(List<ScanResult> scanResults) {
        for(ScanResult scanResult : scanResults) {
            // Scan result with this`` MAC should exist
            int sum = mSumFingerprint.get(scanResult.BSSID);
            int counter = counters.get(scanResult.BSSID);
            mSumFingerprint.put(scanResult.BSSID, sum - scanResult.level);
            counters.put(scanResult.BSSID, counter - 1);
        }
    }

    public WiFiTug.Fingerprint getSumFingerprint() {
        return mSumFingerprint;
    }
    public Map<String, Integer> getCounters() { return counters; }
}

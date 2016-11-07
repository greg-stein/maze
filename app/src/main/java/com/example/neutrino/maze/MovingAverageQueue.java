 package com.example.neutrino.maze;

import android.net.wifi.ScanResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Created by Greg Stein on 10/25/2016.
 */
public class MovingAverageQueue {
    public static final int MAX_SCANS_TO_AVERAGE = 3;
    private int mMaxScans = MAX_SCANS_TO_AVERAGE;
    private WiFiTug.Fingerprint mSumFingerprint = new WiFiTug.Fingerprint();
    private Queue<List<ScanResult>> mQueue = new ArrayDeque<>(mMaxScans);

    @Deprecated
    private final List<ScanResult> mAverageScans = new ArrayList<>();

    public void add(List<ScanResult> results) {
        if (mQueue.size() == mMaxScans) {
            subtractFromSum(mQueue.remove());
        }

        if (mQueue.add(results)) {
            addToSum(results);
        }
        else {
            throw new RuntimeException("Unable to add new scan results to queue.");
        }
    }

    private void addToSum(List<ScanResult> scanResults) {
        for(ScanResult scanResult : scanResults) {
            Integer oSum = mSumFingerprint.get(scanResult.BSSID);

            if (oSum != null) {
                int iSum = oSum; // unbox
                iSum += scanResult.level;
                mSumFingerprint.put(scanResult.BSSID, iSum);
            }
            else {
                mSumFingerprint.put(scanResult.BSSID, scanResult.level);
            }
        }
    }

    private void subtractFromSum(List<ScanResult> scanResults) {
        for(ScanResult scanResult : scanResults) {
            // Scan result with this`` MAC should exist
            int sum = mSumFingerprint.get(scanResult.BSSID);
            mSumFingerprint.put(scanResult.BSSID, sum - scanResult.level);
        }
    }

    @Deprecated
    public List<ScanResult> getMovingAverage() {
        return mAverageScans;
    }

    // Use these two methods instead to calculate average later
    public WiFiTug.Fingerprint getSumFingerprint() {
        return mSumFingerprint;
    }

    public int getNumScans() {
        return mQueue.size();
    }
}

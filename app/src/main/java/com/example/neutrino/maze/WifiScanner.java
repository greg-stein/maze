package com.example.neutrino.maze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;
import java.util.Map;

/**
 * Created by Greg Stein on 8/10/2016.
 */
public class WifiScanner extends BroadcastReceiver {
    private static final int MAX_WIFI_LEVEL = 100; // Percent of signal reception
    public static final int MOVING_AVERAGE_WINDOW_SIZE = 1;

    private MovingAverageQueue mQueue = new MovingAverageQueue(MOVING_AVERAGE_WINDOW_SIZE);
    private List<ScanResult> mLastScan;
    private WifiManager mWifiManager;
    private boolean mIsEnabled = false;
    private int mScanId = 0;

    public WifiScanner(WifiManager wifiManager) {
        mWifiManager = wifiManager;
    }

    public void enable() {
        mWifiManager.startScan();
        mIsEnabled = true;
    }

    public void disable() {
        mIsEnabled = false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
            mLastScan = mWifiManager.getScanResults();
            if (mIsEnabled) mWifiManager.startScan();

            mQueue.add(mLastScan);

            emitWiFiFingerprintAvailableEvent(mQueue.getSumFingerprint(), mQueue.getCounters());
            mScanId++;
        }
    }

    public interface IFingerprintAvailableListener {
        void onFingerprintAvailable(WiFiTug.Fingerprint fingerprint);
    }
    private IFingerprintAvailableListener mFingerprintAvailableListener = null;
    public void setFingerprintAvailableListener(IFingerprintAvailableListener listener) {
        this.mFingerprintAvailableListener = listener;
    }
    private void emitWiFiFingerprintAvailableEvent(WiFiTug.Fingerprint scanResultsSums, Map<String, Integer> numScans) {
        if (mFingerprintAvailableListener != null) {
            WiFiTug.Fingerprint fingerprint = new WiFiTug.Fingerprint();

            for (Map.Entry<String, Integer> entry : scanResultsSums.entrySet()) {
                final Integer scansCounter = numScans.get(entry.getKey());
                if (scansCounter > 0) {
                    fingerprint.put(entry.getKey(), entry.getValue() / scansCounter /*level*/);
                }
            }

            mFingerprintAvailableListener.onFingerprintAvailable(fingerprint);
        }
    }
}

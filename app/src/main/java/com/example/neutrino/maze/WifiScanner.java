package com.example.neutrino.maze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg Stein on 8/10/2016.
 */
public class WifiScanner extends BroadcastReceiver {
    private static final int MAX_WIFI_LEVEL = 100; // Percent of signal reception

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

            emitWiFiFingerprintAvailableEvent(mLastScan);
            mScanId++;
        }
    }

    public interface IFingerprintAvailableListener {
        void onFingerprintAvailable(Map<String, Integer> fingerprint);
    }
    private IFingerprintAvailableListener mFingerprintAvailableListener = null;
    public void setFingerprintAvailableListener(IFingerprintAvailableListener listener) {
        this.mFingerprintAvailableListener = listener;
    }
    private void emitWiFiFingerprintAvailableEvent(List<ScanResult> scanResults) {
        if (mFingerprintAvailableListener != null) {
            Map<String, Integer> fingerprint = new HashMap<>();

            for (ScanResult scan : scanResults) {
                int level = WifiManager.calculateSignalLevel(scan.level, MAX_WIFI_LEVEL);
                fingerprint.put(scan.BSSID, level);
            }

            mFingerprintAvailableListener.onFingerprintAvailable(fingerprint);
        }
    }
}

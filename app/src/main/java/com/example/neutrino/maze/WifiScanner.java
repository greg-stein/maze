package com.example.neutrino.maze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.example.neutrino.maze.WiFiTug.WiFiFingerprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg Stein on 8/10/2016.
 */
public class WifiScanner extends BroadcastReceiver {
    private static final int MAX_WIFI_LEVEL = 100; // Percent of signal reception
    public static final int MOVING_AVERAGE_WINDOW_SIZE = 1;

    private static WifiScanner instance = new WifiScanner();
    public static WifiScanner getInstance() {return instance;}

    private MovingAverageScanResultsQueue mQueue = new MovingAverageScanResultsQueue(MOVING_AVERAGE_WINDOW_SIZE);
    private List<ScanResult> mLastScan;
    private WifiManager mWifiManager;
    private boolean mIsEnabled = false;
    private int mScanId = 0;

    protected WifiScanner() {
        mWifiManager = (WifiManager) AppSettings.appActivity.getSystemService(Context.WIFI_SERVICE);
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
        void onFingerprintAvailable(WiFiFingerprint fingerprint);
    }
    private List<IFingerprintAvailableListener> mFingerprintAvailableListeners = new ArrayList<>();
    public void addFingerprintAvailableListener(IFingerprintAvailableListener listener) {
        this.mFingerprintAvailableListeners.add(listener);
    }
    private void emitWiFiFingerprintAvailableEvent(WiFiFingerprint scanResultsSums, Map<String, Integer> numScans) {
        if (mFingerprintAvailableListeners.size() > 0) {
            WiFiFingerprint fingerprint = new WiFiFingerprint();

            for (Map.Entry<String, Integer> entry : scanResultsSums.entrySet()) {
                final Integer scansCounter = numScans.get(entry.getKey());
                if (scansCounter > 0) {
                    fingerprint.put(entry.getKey(), entry.getValue() / scansCounter /*level*/);
                }
            }

            for (IFingerprintAvailableListener listener : mFingerprintAvailableListeners) {
                if (listener != null) {
                    listener.onFingerprintAvailable(fingerprint);
                }
            }
        }
    }
}

package com.example.neutrino.maze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg Stein on 8/10/2016.
 */
public class WifiScanner extends BroadcastReceiver {
    private static final int MAX_WIFI_LEVEL = 100; // Percent of signal reception
    public static final int MOVING_AVERAGE_WINDOW_SIZE = 1;

    private static WifiScanner instance = null;
    public static WifiScanner getInstance() {
        if (instance == null) {
            instance = new WifiScanner();
        }
        return instance;
    }

    private List<IFingerprintAvailableListener> mFingerprintAvailableListeners = new ArrayList<>();
    private MovingAverageScanResultsQueue mQueue = new MovingAverageScanResultsQueue(MOVING_AVERAGE_WINDOW_SIZE);
    private List<ScanResult> mLastScan;
    private WifiManager mWifiManager;
    private boolean mIsEnabled = false;

    protected WifiScanner() {
        mWifiManager = (WifiManager) AppSettings.appActivity.getSystemService(Context.WIFI_SERVICE);
    }

    public void onActivityResume() {
        AppSettings.appActivity.registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
        mIsEnabled = true;
    }

    public void onActivityPause() {
        AppSettings.appActivity.unregisterReceiver(this);
    }

    public WiFiFingerprint getLastFingerprint() { return WiFiFingerprint.build(mQueue.getLastItem());}

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
            mLastScan = mWifiManager.getScanResults();
            if (mIsEnabled) mWifiManager.startScan();

            mQueue.add(mLastScan);

            emitWiFiFingerprintAvailableEvent(mQueue.getSumFingerprint(), mQueue.getCounters());
        }
    }

    public interface IFingerprintAvailableListener {
        void onFingerprintAvailable(WiFiFingerprint fingerprint);
    }

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

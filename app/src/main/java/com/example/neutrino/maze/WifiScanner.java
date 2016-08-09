package com.example.neutrino.maze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;

/**
 * Created by Greg Stein on 8/10/2016.
 */
public class WifiScanner extends BroadcastReceiver {
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
            mScanId++;
        }
    }
}

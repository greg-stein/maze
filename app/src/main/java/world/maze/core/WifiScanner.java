package world.maze.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import world.maze.ui.IUserNotifier;
import world.maze.util.MovingAverageScanResultsQueue;
import world.maze.util.PermissionsHelper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Greg Stein on 8/10/2016.
 */
public class WifiScanner extends BroadcastReceiver {
    private static final int MAX_WIFI_LEVEL = 100; // Percent of signal reception
    public static final int MOVING_AVERAGE_WINDOW_SIZE = 1;

    private static WifiScanner instance = null;
    private static final Object mutex = new Object();
    public static WifiScanner getInstance(Context context) {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new WifiScanner(context);
                    if (context instanceof IUserNotifier) { // hack
                        instance.setUserNotifier((IUserNotifier) context);
                    }
                }
            }
        }
        return instance;
    }

    private List<IFingerprintAvailableListener> mFingerprintAvailableListeners = new CopyOnWriteArrayList<>();
    private MovingAverageScanResultsQueue mQueue = new MovingAverageScanResultsQueue(MOVING_AVERAGE_WINDOW_SIZE);
    private List<ScanResult> mLastScan;
    private WifiManager mWifiManager;
    private boolean mIsEnabled = true;

    public void setUserNotifier(IUserNotifier userNotifier) {
        mUserNotifier = userNotifier;
    }

    private IUserNotifier mUserNotifier;

    protected WifiScanner(Context context) {
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void onResume(Context context) {
        context.registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
    }

    public void onPause(Context context) {
        context.unregisterReceiver(this);
    }

    public void setEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public WiFiLocator.WiFiFingerprint getLastFingerprint() { return WiFiLocator.WiFiFingerprint.build(mQueue.getLastItem());}

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {

            if (PermissionsHelper.coarseLocationPermissionsGranted(context)) {
                enqueueScan();
            } else {
                if (PermissionsHelper.requestCoarseLocationPermission(context)) {
                    AsyncTask<Void, Void, Boolean> permissionsWaiter = new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... voids) {
                            return PermissionsHelper.waitForCoarseLocationPermission(context);
                        }

                        // Runs on UI thread
                        @Override
                        protected void onPostExecute(Boolean result) {
                            if (true == result) {
                                enqueueScan();
                            } else {
                                // Permission was denied
                                mUserNotifier.displayTragicError("Critical permission denied!",
                                        "You have denied critical permission (WRITE_EXTERNAL_PERMISSION)." +
                                                " This time the app will quit. Please consider giving the permission when the app starts next time.");
                            }
                        }
                    };
                    permissionsWaiter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                } else {
                    // Permission was disabled
                    mUserNotifier.displayTragicError("Critical permission disabled!",
                            "You have disabled us from asking for WRITE_EXTERNAL_PERMISSION." +
                                    " The app will be closed now. Please consider giving it manually.");
                }
            }
        }
    }

    public void enqueueScan() {
        mLastScan = mWifiManager.getScanResults();
        if (mIsEnabled) mWifiManager.startScan();

        mQueue.add(mLastScan);

        emitWiFiFingerprintAvailableEvent(mQueue.getSumFingerprint(), mQueue.getCounters());
    }

    public interface IFingerprintAvailableListener {
        void onFingerprintAvailable(WiFiLocator.WiFiFingerprint fingerprint);
    }

    public void removeFingerprintAvailableListener(IFingerprintAvailableListener listener) {
        this.mFingerprintAvailableListeners.remove(listener);
    }

    public void addFingerprintAvailableListener(IFingerprintAvailableListener listener) {
        this.mFingerprintAvailableListeners.add(listener);
    }

    private void emitWiFiFingerprintAvailableEvent(WiFiLocator.WiFiFingerprint scanResultsSums, Map<String, Integer> numScans) {
        if (mIsEnabled && mFingerprintAvailableListeners.size() > 0) {
            WiFiLocator.WiFiFingerprint fingerprint = new WiFiLocator.WiFiFingerprint();

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

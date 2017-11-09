package com.example.neutrino.maze.core;

import android.content.Context;
import android.support.v4.util.Pair;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;
import com.example.neutrino.maze.util.PermissionsHelper;

import java.util.List;

/**
 * Created by Greg Stein on 10/31/2017.
 */

public class MazeClient implements IMazePresenter {
    private Context mContext;
    private final IMainView mMainView;
    private WifiScanner mWifiScanner = null;
    private IMazeServer mMazeServer;
    private FloorPlan mFloorPlan;
    private List<WiFiLocator.WiFiFingerprint> mRadioMapTile;

    private WifiScanner.IFingerprintAvailableListener mFirstFingerprintAvailableListener
            = new WifiScanner.IFingerprintAvailableListener() {

        public boolean mBuildingReceived = false;
        public boolean mFloorPlanReceived = false;
        public boolean mRadioTileReceived = false;

        private IFuckingSimpleGenericCallback<List<WiFiLocator.WiFiFingerprint>> mOnRadioTileReceived =
                new IFuckingSimpleGenericCallback<List<WiFiLocator.WiFiFingerprint>>() {
            @Override
            public void onNotify(List<WiFiLocator.WiFiFingerprint> wiFiFingerprints) {
                MazeClient.this.mRadioMapTile = wiFiFingerprints;
                mRadioTileReceived = true;
                renderOnComplete();
            }
        };
        private IFuckingSimpleGenericCallback<FloorPlan> mOnFloorPlanReceived = new IFuckingSimpleGenericCallback<FloorPlan>() {
            @Override
            public void onNotify(FloorPlan floorPlan) {
                MazeClient.this.mFloorPlan = floorPlan;
                mFloorPlanReceived = true;
                renderOnComplete();
            }
        };
        private IFuckingSimpleGenericCallback<Building> mOnBuildingReceived = new IFuckingSimpleGenericCallback<Building>() {
            @Override
            public void onNotify(Building building) {
                Building.current = building;
                mBuildingReceived = true;
                renderOnComplete();
            }
        };

        private void renderOnComplete() {
            if (mRadioTileReceived && mFloorPlanReceived && mBuildingReceived) {
                // Render the floor plan
            }
        }

        public boolean mFloorToBeUpdated = false;

        @Override
        public void onFingerprintAvailable(final WiFiLocator.WiFiFingerprint fingerprint) {
            // Unsubscribe from recieving more fingerprints. We need only first fingerprint to find
            // building and floor
            mWifiScanner.removeFingerprintAvailableListener(mFirstFingerprintAvailableListener);
            // TODO: check whether it makes sense to acquire several fingerprints and average them
            mMazeServer.findCurrentBuildingAndFloorAsync(fingerprint, new IFuckingSimpleGenericCallback<Pair<String, String>>() {
                @Override
                public void onNotify(Pair<String, String> buildingAndFloorIds) {
                    String buildingId = buildingAndFloorIds.first;
                    String floorId = buildingAndFloorIds.second;

                    // Do we need to update building struct?
                    if (Building.current == null || !Building.current.getID().equals(buildingId)) {
                        mMazeServer.getBuildingAsync(buildingId, mOnBuildingReceived);
                        mFloorToBeUpdated = true;
                    }

                    if (mFloorToBeUpdated || !Building.current.getCurrentFloor().getId().equals(floorId)) {
                        mMazeServer.downloadFloorPlanAsync(floorId, mOnFloorPlanReceived);
                        mMazeServer.downloadRadioMapTileAsync(floorId, fingerprint, mOnRadioTileReceived);
                    }
                }
            });
        }
    };

    public MazeClient(Context context, IMainView mainView) {
        mContext = context;
        mMainView = mainView; // UI
    }

    @Override
    public void onCreate() {
        AppSettings.init(mContext);

        // TODO: instead of just killing the app, consider reloading activity when the permission is granted.
        if (!PermissionsHelper.requestPermissions(mContext)) return;

        mMazeServer = MazeServerMock.getInstance(mContext);

        if (mWifiScanner == null) {
            mWifiScanner = WifiScanner.getInstance(mContext);
            mWifiScanner.addFingerprintAvailableListener(mFirstFingerprintAvailableListener);
        }
    }

    @Override
    public void onResume() {
        mWifiScanner.onResume(mContext);
    }

    @Override
    public void onPause() {
        mWifiScanner.onPause(mContext);
    }

    @Override
    public void onDestroy() {
        mWifiScanner.removeFingerprintAvailableListener(mFirstFingerprintAvailableListener);
    }

}

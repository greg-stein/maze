package com.example.neutrino.maze.core;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
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

public class MazeClient implements IMazePresenter, Locator.ILocationUpdatedListener {
    private Context mContext;
    private final IMainView mMainView;
    private WifiScanner mWifiScanner = null;
    private IMazeServer mMazeServer;
    private Locator mLocator;
    private FloorPlan mFloorPlan;

    private StepCalibratorService mStepCalibratorService;
    private Intent mStepCalibratorServiceIntent;

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

// THIS IS HERE FOR REFERENCE ONLY! After successful construction of floor plan remove it
//        mFloorChangedHandler = new IFloorChangedHandler() {
//            @Override
//            public void onFloorChanged(Floor floor) {
//                if (Building.current.getCurrentFloor().getId() != floor.getId()) {
//                    final IMazeServer mazeServer = MazeServerMock.getInstance(MainActivity.this);
//                    String jsonString = mazeServer.downloadFloorPlanJson(floor.getId());
//                    // TODO: load new floor plan, tags, teleports, ...
//
//                    new LoadFloorPlanTask(MainActivity.this).onFinish(new LoadFloorPlanTask.AsyncResponse() {
//                        @Override
//                        public void onFinish(FloorPlan floorPlan) {
//                            mFloorPlan = floorPlan;
//// TODO:  mvp_refactor: Fix this!!!
////                            mLocator.setFloorPlan(mFloorPlan);
//                            mMapper.setFloorPlan(mFloorPlan);
//
//                            mAdapter.updateListData(floorPlan.getTags());
//
//                            // Find point that should be visible after the floorplan is loaded
//                            PointF pointToShow = null;
//
//                            List<IFloorPlanPrimitive> sketch = mFloorPlan.getSketch();
//                            for (IFloorPlanPrimitive primitive : sketch) {
//                                if (primitive instanceof Wall) {
//                                    pointToShow = ((Wall)primitive).getStart();
//                                    break;
//                                }
//                            }
//                            // The main work is done on GL thread!
//                            uiFloorPlanView.plot(floorPlan, pointToShow);
//                        }
//                    }).execute(jsonString);
//                }
//            }
//        };

    public MazeClient(Context context, IMainView mainView) {
        mContext = context;
        mMainView = mainView; // UI
    }

    @Override
    public void onCreate() {
        AppSettings.init(mContext);

        // TODO: instead of just killing the app, consider reloading activity when the permission is granted.
        if (!PermissionsHelper.requestPermissions(mContext)) return;

        mStepCalibratorService = new StepCalibratorService(mContext);
        mStepCalibratorServiceIntent = new Intent(mContext, mStepCalibratorService.getClass());

        if (!StepCalibratorService.isRunning(mContext)) {
            mContext.startService(mStepCalibratorServiceIntent);
        }

        mMazeServer = MazeServerMock.getInstance(mContext);

        if (mWifiScanner == null) {
            mWifiScanner = WifiScanner.getInstance(mContext);
            mWifiScanner.addFingerprintAvailableListener(mFirstFingerprintAvailableListener);
        }

        mLocator = Locator.getInstance(mContext);
        mLocator.addLocationUpdatedListener(this);
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
        // In case the permissions were granted previous time the app was running, we can
        // start the service. The service will start by stopping it :) Because this forces restart.
        if (PermissionsHelper.permissionsWereAlreadyGranted) {
            mContext.stopService(mStepCalibratorServiceIntent); // This will resurrect the service
        }
        mWifiScanner.removeFingerprintAvailableListener(mFirstFingerprintAvailableListener);
        if (mLocator != null) {
            mLocator.onDestroy();
        }
    }

    // TODO: These methods should probably be implemented with Observer pattern rather than direct
    // TODO: call from MainActivity

    @Override
    public void setMapNorth(float mapNorth) {
        mLocator.setNorth(mapNorth);
    }

    @Override
    public void setLocationByUser(PointF location) {
        mLocator.resetLocationTo(location);
    }

    @Override
    public void onLocationUpdated(PointF location) {
        mMainView.updateLocation(location); // draw new location on map
    }

}

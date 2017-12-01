package com.example.neutrino.maze.core;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.support.v4.util.Pair;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.core.SensorListener.IDeviceRotationListener;
import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.RadioMapFragment;
import com.example.neutrino.maze.rendering.RenderGroup;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;
import com.example.neutrino.maze.util.PermissionsHelper;

import static com.example.neutrino.maze.core.Locator.*;

/**
 * Created by Greg Stein on 10/31/2017.
 */

public class MazeClient implements IMazePresenter, ILocationUpdatedListener, IDeviceRotationListener, IFuckingSimpleGenericCallback<Fingerprint> {
    private Context mContext;
    private final IMainView mMainView;
    private WifiScanner mWifiScanner = null;
    private IMazeServer mMazeServer;
    private Locator mLocator;
    private Mapper mMapper;
    private boolean mMapperLastState;
    private SensorListener mSensorListener;
    private FloorPlan mFloorPlan;

    private RenderGroup mFloorPlanRenderGroup;

    private StepCalibratorService mStepCalibratorService;
    private Intent mStepCalibratorServiceIntent;
    private RadioMapFragment mRadioMapFragment;
    private WifiScanner.IFingerprintAvailableListener mFirstFingerprintAvailableListener
            = new WifiScanner.IFingerprintAvailableListener() {

        public boolean mBuildingReceived = false;
        public boolean mFloorPlanReceived = false;
        public boolean mRadioTileReceived = false;

        private IFuckingSimpleGenericCallback<RadioMapFragment> mOnRadioTileReceived =
                new IFuckingSimpleGenericCallback<RadioMapFragment>() {
            @Override
            public void onNotify(RadioMapFragment wiFiFingerprints) {
                MazeClient.this.mRadioMapFragment = wiFiFingerprints;
                mRadioTileReceived = true;
                onCompleteDataReceive();
            }
        };
        private IFuckingSimpleGenericCallback<FloorPlan> mOnFloorPlanReceived = new IFuckingSimpleGenericCallback<FloorPlan>() {
            @Override
            public void onNotify(FloorPlan floorPlan) {
                MazeClient.this.mFloorPlan = floorPlan;
                mFloorPlanReceived = true;
                onCompleteDataReceive();
            }
        };
        private IFuckingSimpleGenericCallback<Building> mOnBuildingReceived = new IFuckingSimpleGenericCallback<Building>() {
            @Override
            public void onNotify(Building building) {
                Building.current = building;
                mBuildingReceived = true;
                onCompleteDataReceive();
            }
        };

        private void onCompleteDataReceive() {
            if (mRadioTileReceived && mFloorPlanReceived && mBuildingReceived) {
                // Render the floor plan
                mFloorPlanRenderGroup = mMainView.render(MazeClient.this.mFloorPlan);
                // Locator uses floor plan for collision recognition
                mLocator.setFloorPlan(MazeClient.this.mFloorPlan);
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
                    if (Building.current == null || !Building.current.getId().equals(buildingId)) {
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

        mLocator = getInstance(mContext);
        mMapper = Mapper.getInstance(mContext);
        mMapperLastState = mMapper.isEnabled();
        mSensorListener = SensorListener.getInstance(mContext);

        setUiHandlers();
    }

    private void setUiHandlers() {
        mMainView.setMapperEnabledChangedListener(new IFuckingSimpleGenericCallback<Boolean>() {
            @Override
            public void onNotify(Boolean enabled) {
                mMapper.setEnabled(enabled);
            }
        });
    }

    @Override
    public void onResume() {
        if (PermissionsHelper.locationPermissionsGranted(mContext)) {
            mWifiScanner.onResume(mContext);
        }
        mLocator.addLocationUpdatedListener(this);
        mSensorListener.addDeviceRotationListener(this);
        mMapper.setOnNewFingerprintListener(this);
        mMapper.setEnabled(mMapperLastState);
    }

    @Override
    public void onPause() {
        // to stop the listener and save battery
        if (PermissionsHelper.locationPermissionsGranted(mContext)) {
            mWifiScanner.onPause(mContext);
        }

        mLocator.removeLocationUpdatedListener(this);
        mSensorListener.removeDeviceRotationListener(this);
        mMapperLastState = mMapper.isEnabled();
        mMapper.setEnabled(false);
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

    @Override
    public void onDeviceRotated(double degree) {
        mMainView.setMapRotation(degree);
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

    @Override
    public void onNotify(Fingerprint fingerprint) {
        mMainView.renderFingeprint(fingerprint);
        // TODO: It should not be the case that any newly created fingerprint is added to the radiomap
        // TODO: Instead, we should examine fingerprint's quality and only after that add it to SEPARATE
        // TODO: collection which later will be upoaded to server.
        mRadioMapFragment.addFingerprint(fingerprint);
    }
}

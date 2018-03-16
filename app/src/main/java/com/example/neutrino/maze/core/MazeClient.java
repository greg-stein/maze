package com.example.neutrino.maze.core;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.support.v4.util.Pair;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.core.SensorListener.IDeviceRotationListener;
import com.example.neutrino.maze.floorplan.Building;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.Floor;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.IMoveable;
import com.example.neutrino.maze.floorplan.RadioMapFragment;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.rendering.ElementsRenderGroup;
import com.example.neutrino.maze.rendering.TextRenderGroup;
import com.example.neutrino.maze.util.IFuckingSimpleCallback;
import com.example.neutrino.maze.util.IFuckingSimpleGenericCallback;
import com.example.neutrino.maze.util.PermissionsHelper;

import java.util.ArrayList;
import java.util.List;

import static com.example.neutrino.maze.core.Locator.*;

/**
 * Created by Greg Stein on 10/31/2017.
 */

public class MazeClient implements IMazePresenter, ILocationUpdatedListener, IDeviceRotationListener, IFuckingSimpleGenericCallback<Fingerprint>,IFloorChangedHandler {
    private Context mContext;
    private final IMainView mMainView;
    private WifiScanner mWifiScanner = null;
    private IMazeServer mMazeServer;
    private Locator mLocator;
    private Mapper mMapper;
    private boolean mMapperLastState;
    private SensorListener mSensorListener;
    private FloorWatcher mFloorWatcher;
    private FloorPlan mFloorPlan;

    private ElementsRenderGroup mFloorPlanRenderGroup;
    private ElementsRenderGroup mRadioMapRenderGroup;
    // This is for newly created fingerprints during scan (mapping)
    private ElementsRenderGroup mAugmentedRadioMapRenderGroup;
    private List<Fingerprint> mAugmentedRadioMap = new ArrayList<>();
    private TextRenderGroup mTagsRenderGroup;

    private StepCalibratorService mStepCalibratorService;
    private boolean mStepCalibratorEnabled = false; // temporary change to disable StepCalibratorService
    private Intent mStepCalibratorServiceIntent;
    private RadioMapFragment mRadioMapFragment;
    private List<Tag> mTags;
    private WifiScanner.IFingerprintAvailableListener mFirstFingerprintAvailableListener
            = new WifiScanner.IFingerprintAvailableListener() {

        public String mFloorId;
        public String mBuildingId;
        public boolean mBuildingReceived = false;
        public boolean mFloorPlanReceived = false;
        public boolean mRadioTileReceived = false;

        private IFuckingSimpleGenericCallback<RadioMapFragment> mOnRadioTileReceived =
                new IFuckingSimpleGenericCallback<RadioMapFragment>() {
            @Override
            public void onNotify(RadioMapFragment wiFiFingerprints) {
                mRadioMapFragment = wiFiFingerprints;
                mRadioMapRenderGroup = mMainView.createElementsRenderGroup(mRadioMapFragment.getFingerprintsAsIFloorPlanElements());
                mRadioTileReceived = true;
                onCompleteDataReceive();
            }
        };
        private IFuckingSimpleGenericCallback<FloorPlan> mOnFloorPlanReceived = new IFuckingSimpleGenericCallback<FloorPlan>() {
            @Override
            public void onNotify(FloorPlan floorPlan) {
                mFloorPlan = floorPlan;
                mFloorPlanRenderGroup = mMainView.createElementsRenderGroup(mFloorPlan.getSketch());
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

        private synchronized void onCompleteDataReceive() {
            if (mRadioTileReceived && mFloorPlanReceived && mBuildingReceived) {
                onFloorChanged(Building.current.getFloor(mFloorId));
                Building.current.setCurrentFloor(mFloorId);
                mTagsRenderGroup = mMainView.createTextRenderGroup(Building.current.getCurrentFloor().getTags());
                mTagsRenderGroup.setChangedListener(mTagsChangedListener);
                mFloorPlanRenderGroup.setChangedListener(mFlorPlanChangedListener);
                // Render the floor plan
                mFloorPlanRenderGroup.setVisible(true);
                mTagsRenderGroup.setVisible(true);
                mMainView.centerMapView(mFloorPlan.getCenter());
                // Locator uses floor plan for collision recognition
                mLocator.setFloorPlan(mFloorPlan);
                mFloorWatcher.setBuilding(Building.current);
                if (Building.current != null) mFloorWatcher.enable(mContext);
            }
        }

        public boolean mFloorToBeUpdated = false;

        @Override
        public void onFingerprintAvailable(final WiFiLocator.WiFiFingerprint fingerprint) {
            if (fingerprint == null || fingerprint.isEmpty()) return;
            // Unsubscribe from recieving more fingerprints. We need only first fingerprint to find
            // building and floor
            mWifiScanner.removeFingerprintAvailableListener(mFirstFingerprintAvailableListener);
            // TODO: check whether it makes sense to acquire several fingerprints and average them
            mMazeServer.findCurrentBuildingAndFloorAsync(fingerprint, new IFuckingSimpleGenericCallback<Pair<String, String>>() {
                @Override
                public void onNotify(Pair<String, String> buildingAndFloorIds) {
                    mBuildingId = buildingAndFloorIds.first;
                    mFloorId = buildingAndFloorIds.second;

                    // If we failed to find a building based on fingerprint (i.e. new building)
                    if (mBuildingId.isEmpty()) {
                        mMainView.askUserToCreateBuilding(new IFuckingSimpleGenericCallback<Boolean>() {
                            @Override
                            public void onNotify(Boolean agreed) {
                                if (agreed) {
                                    mMainView.showBuildingEditDialog();
                                }
                            }
                        });
                        return;
                    }

                    // Do we need to update building struct?
                    if (Building.current == null || !Building.current.getId().equals(mBuildingId)) {
                        mMazeServer.getBuildingAsync(mBuildingId, mOnBuildingReceived);
                        mFloorToBeUpdated = true;
                    }

                    // TODO: what if this condition is NOT met? What will happen to mRadioTileReceived && mFloorPlanReceived?
                    if (mFloorToBeUpdated || !Building.current.getCurrentFloor().getId().equals(mFloorId)) {
                        mMazeServer.downloadFloorPlanAsync(mFloorId, mOnFloorPlanReceived);
                        mMazeServer.downloadRadioMapTileAsync(mFloorId, fingerprint, mOnRadioTileReceived);
                    }
                }
            });
        }
    };

    private IMainView.IRenderGroupChangedListener mFlorPlanChangedListener = new IMainView.IRenderGroupChangedListener() {
        @Override
        public void onElementAdd(IMoveable element) {
            mFloorPlan.addElement((IFloorPlanPrimitive) element); // add to floor plan container
            mMainView.setUploadButtonVisibility(true);
        }

        @Override
        public void onElementChange(IMoveable element) {
            mFloorPlan.setSketchDirty(true); // NOTE: assume mFloorPlan is not null
            mMainView.setUploadButtonVisibility(true);
        }

        @Override
        public void onElementRemoved(IMoveable element) {
            mFloorPlan.removeElement((IFloorPlanPrimitive) element);
            mMainView.setUploadButtonVisibility(true);
        }
    };


    private IMainView.IRenderGroupChangedListener mTagsChangedListener = new IMainView.IRenderGroupChangedListener() {
        @Override
        public void onElementAdd(IMoveable element) {
            Building.current.getCurrentFloor().addTag((Tag) element); // add new tag to floor
            Building.current.setDirty(true); // mark building to upload the tag
            mMainView.setUploadButtonVisibility(true);
        }

        @Override
        public void onElementChange(IMoveable element) {
            Building.current.setDirty(true);
            mMainView.setUploadButtonVisibility(true);
        }

        @Override
        public void onElementRemoved(IMoveable element) {
            Building.current.getCurrentFloor().removeTag((Tag) element);
            Building.current.setDirty(true);
            mMainView.setUploadButtonVisibility(true);
        }
    };

    private IMainView.IElementFactory mElementFactory = new IMainView.IElementFactory() {
        @Override
        public IMoveable createElement(IMainView.MapOperand elementType, PointF location, Object... params) {
            switch (elementType) {
                case WALL:
                    Wall newWall = new Wall(location, location);
                    mFloorPlanRenderGroup.addElement(newWall); // render new wall
                    return newWall;
                case SHORT_WALL:
                    break;
                case BOUNDARIES:
                    break;
                case TELEPORT:
                    break;
                case LOCATION_TAG:
                    if (params != null && params.length > 0 && params[0] instanceof String) {
                        String label = (String) params[0];
                        Tag tag = new Tag(location, label);
                        mTagsRenderGroup.addItem(tag); // render tag
                        return tag;
                    }
                    break;
                default: return null;
            }

            return null;
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

        if (mStepCalibratorEnabled) {
            mStepCalibratorService = new StepCalibratorService(mContext);
            mStepCalibratorServiceIntent = new Intent(mContext, mStepCalibratorService.getClass());

            if (!StepCalibratorService.isRunning(mContext)) {
                mContext.startService(mStepCalibratorServiceIntent);
            }
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
        mFloorWatcher = FloorWatcher.getInstance(mContext);
        // Occurs when floor change is recognized
        mFloorWatcher.addOnFloorChangedListener(this);

        mMainView.setElementFactory(mElementFactory);
        setUiHandlers();
    }

    private void setUiHandlers() {
        // Occurs when user explicitly sets current floor
        mMainView.setFloorChangedHandler(this);

        mMainView.setUploadButtonClickListener(new IFuckingSimpleCallback() {
            @Override
            public void onNotified() {
                // TODO: Upload changes in floor plan, radio map, tags, teleports, ...
            }
        });

        mMainView.setOnLocateMeEnabledChangedListener(new IFuckingSimpleGenericCallback<Boolean>() {
            @Override
            public void onNotify(Boolean locatorEnabled) {
                mLocator.useWifiScanner(locatorEnabled);
            }
        });

        mMainView.setMapperEnabledChangedListener(new IFuckingSimpleGenericCallback<Boolean>() {
            @Override
            public void onNotify(Boolean enabled) {
                mMapper.setEnabled(enabled);
            }
        });

        mMainView.setUiModeChangedListener(new IFuckingSimpleGenericCallback<IMainView.UiMode>() {
            @Override
            public void onNotify(IMainView.UiMode uiMode) {
                if (mRadioMapRenderGroup != null) {
                    switch (uiMode) {
                        case MAP_VIEW_MODE:
                            mRadioMapRenderGroup.setVisible(false);
                            break;
                        case MAP_EDIT_MODE:
                            mRadioMapRenderGroup.setVisible(true);
                            break;
                    }
                }
            }
        });

        mMainView.setBuildingIdProvider(new IMainView.IAsyncIdProvider() {
            @Override
            public void generateId(IFuckingSimpleGenericCallback<String> idGeneratedCallback) {
                mMazeServer.createBuildingAsync(idGeneratedCallback);
            }
        });

        mMainView.setFloorIdProvider(new IMainView.IAsyncIdProvider() {
            @Override
            public void generateId(IFuckingSimpleGenericCallback<String> idGeneratedCallback) {
                mMazeServer.createFloorAsync(idGeneratedCallback);
            }
        });
        
        mMainView.setSimilarBuildingsFinder(new IMainView.IAsyncSimilarBuildingsFinder() {
            @Override
            public void findBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {
                mMazeServer.findSimilarBuildings(pattern, buildingsAcquiredCallback);
            }
        });

        mMainView.setBuildingCreator(new IMainView.IAsyncBuildingCreator() {
          @Override
          public void createBuilding(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback) {
                mMazeServer.createBuildingAsync(name, type, address, buildingCreatedCallback);

                mFloorPlanRenderGroup = mMainView.createElementsRenderGroup(null);
                mFloorPlanRenderGroup.setVisible(true);
                mFloorPlanRenderGroup.setChangedListener(mFlorPlanChangedListener);

                mTagsRenderGroup = mMainView.createTextRenderGroup(null);
                mTagsRenderGroup.setVisible(true);
                mTagsRenderGroup.setChangedListener(mTagsChangedListener);

                mFloorPlan = FloorPlan.build();
          }
        });

        mMainView.setBuildingUpdater(new IFuckingSimpleGenericCallback<Building>() {
            @Override
            public void onNotify(Building building) {
                mMazeServer.upload(building, new IFuckingSimpleCallback() {
                    @Override
                    public void onNotified() {
                        // Do nothing. Maybe indicate in UI that building has been updated on server?
                    }
                });
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
        if (mStepCalibratorEnabled) {
            // In case the permissions were granted previous time the app was running, we can
            // start the service. The service will start by stopping it :) Because this forces restart.
            if (PermissionsHelper.permissionsWereAlreadyGranted) {
                mContext.stopService(mStepCalibratorServiceIntent); // This will resurrect the service
            }
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

    // This callback is executed when mapper is active and it obtained a new fingerprint
    @Override
    public void onNotify(Fingerprint fingerprint) {
        // TODO: It should not be the case that any newly created fingerprint is added to the radiomap
        // TODO: Instead, we should examine fingerprint's quality and only after that add it to SEPARATE
        // TODO: collection which later will be upoaded to server.
        mAugmentedRadioMap.add(fingerprint);
        mAugmentedRadioMapRenderGroup.addElement(fingerprint);
    }

    @Override
    public void onFloorChanged(Floor newFloor) {
        final Floor currentFloor = Building.current.getCurrentFloor();

        // If on the same floor - it wasn't really changed
        if (currentFloor != null && currentFloor.getId().equals(newFloor.getId())) return;

        // Are there scans to upload to the server?
        if (!mAugmentedRadioMap.isEmpty()) {
            RadioMapFragment radioMapFragment = new RadioMapFragment(mAugmentedRadioMap, currentFloor.getId());
            mMazeServer.upload(radioMapFragment, new IFuckingSimpleCallback() {
                @Override
                public void onNotified() {
                    mAugmentedRadioMap.clear(); // clear to store scans on another floor
                    // TODO: notify UI maybe?
                }
            });
        }

        if (mAugmentedRadioMapRenderGroup == null) {
            mAugmentedRadioMapRenderGroup = new ElementsRenderGroup(new ArrayList<IFloorPlanPrimitive>());
        } else {
            mAugmentedRadioMapRenderGroup.clear();
        }
    }
}

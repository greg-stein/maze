package world.maze.core;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import world.maze.AppSettings;
import world.maze.core.SensorListener.IDeviceRotationListener;
import world.maze.data.AssetsDataProvider;
import world.maze.data.DataAggregator;
import world.maze.data.IDataProvider;
import world.maze.data.LocalStore;
import world.maze.floorplan.Building;
import world.maze.floorplan.Fingerprint;
import world.maze.floorplan.Floor;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.IFloorPlanPrimitive;
import world.maze.floorplan.IMoveable;
import world.maze.floorplan.LocationMark;
import world.maze.floorplan.RadioMapFragment;
import world.maze.floorplan.Ring;
import world.maze.floorplan.Tag;
import world.maze.floorplan.Wall;
import world.maze.floorplan.transitions.Teleport;
import world.maze.rendering.ElementsRenderGroup;
import world.maze.rendering.TextRenderGroup;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;
import world.maze.util.PermissionsHelper;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static world.maze.core.Locator.ILocationUpdatedListener;
import static world.maze.core.Locator.getInstance;

/**
 * Created by Greg Stein on 10/31/2017.
 */

public class MazeClient implements IMazePresenter, ILocationUpdatedListener, IDeviceRotationListener, IFuckingSimpleGenericCallback<Fingerprint>, IFloorChangedHandler {
    private Context mContext;
    private final IMainView mMainView;
    private WifiScanner mWifiScanner = null;
    private DataAggregator mDataAggregator;
    private List<IDataProvider> mDataProviders = new ArrayList<>();
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
    private TextRenderGroup mTeleportsLabelsRenderGroup;
    private ElementsRenderGroup mTeleportsElementsRenderGroup;
    // This group holds elements that are not part of data model. Like user location marker.
    private ElementsRenderGroup mAuxiliaryRenderGroup;
    private Ring mLocationMark = null;

    private StepCalibratorService mStepCalibratorService;
    private boolean mStepCalibratorEnabled = false; // temporary change to disable StepCalibratorService
    private Intent mStepCalibratorServiceIntent;
    private RadioMapFragment mRadioMapFragment;

    private WifiScanner.IFingerprintAvailableListener mFirstFingerprintAvailableListener = new WifiScanner.IFingerprintAvailableListener() {
        @Override
        public void onFingerprintAvailable(final WiFiLocator.WiFiFingerprint fingerprint) {
            if (fingerprint == null || fingerprint.isEmpty()) return;
            // Unsubscribe from recieving more fingerprints. We need only first fingerprint to find
            // building and floor
            mWifiScanner.removeFingerprintAvailableListener(mFirstFingerprintAvailableListener);
            // TODO: check whether it makes sense to acquire several fingerprints and average them
            mDataAggregator.findCurrentBuildingAndFloorAsync(fingerprint, new IFuckingSimpleGenericCallback<Pair<String, String>>() {

                @Override
                public void onNotify(Pair<String, String> buildingAndFloorIds) {
                    BuildingUpdater buildingAndFloorUpdater = new BuildingUpdater(fingerprint);
                    buildingAndFloorUpdater.update(buildingAndFloorIds);
                }
            });
        }
    };

    private class FloorUpdater {
        protected String mFloorId;

        protected boolean mFloorPlanReceived = false;
        protected boolean mRadioTileReceived = false;
        protected WiFiLocator.WiFiFingerprint mFingerprint;

        public FloorUpdater(WiFiLocator.WiFiFingerprint fingerprint) {
            mFingerprint = fingerprint;
        }

        private IFuckingSimpleGenericCallback<RadioMapFragment> mOnRadioTileReceived =
                new IFuckingSimpleGenericCallback<RadioMapFragment>() {
                    @Override
                    public void onNotify(RadioMapFragment wiFiFingerprints) {
                        mRadioMapFragment = wiFiFingerprints;
                        mLocator.setRadioMapFragment(wiFiFingerprints);
                        mRadioMapRenderGroup = mMainView.createElementsRenderGroup(mRadioMapFragment.getFingerprintsAsIFloorPlanElements());
                        // TODO: Ask user if he/she wants to discard currently added fingerprints
                        // TODO: Remove previous group and deallocate it
                        mAugmentedRadioMapRenderGroup = mMainView.createElementsRenderGroup(null);
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

        private synchronized void onCompleteDataReceive() {
            if (mRadioTileReceived && mFloorPlanReceived) {
                Building.current.setCurrentFloor(mFloorId);
                mTagsRenderGroup = mMainView.createTextRenderGroup(Building.current.getCurrentFloor().getTags());
                mTeleportsLabelsRenderGroup = mMainView.createTextRenderGroup(Building.current.getCurrentFloor().getTeleports());
                mTeleportsLabelsRenderGroup.setVisible(false);
                mTeleportsElementsRenderGroup = mMainView.createElementsRenderGroup(Building.current.getCurrentFloor().getTeleports());
                mTeleportsElementsRenderGroup.setChangedListener(mTeleportsChangedListener);
                mTeleportsElementsRenderGroup.setVisible(false);
                mTagsRenderGroup.setChangedListener(mTagsChangedListener);
                mFloorPlanRenderGroup.setChangedListener(mFloorPlanChangedListener);
                // Only augmented render group is being changed.
                // TODO: What about removing existing fingerprints from current radio map?
                mAugmentedRadioMapRenderGroup.setChangedListener(mFingerprintsChangedListener);
                // Render the floor plan
                mFloorPlanRenderGroup.setVisible(true);
                mTagsRenderGroup.setVisible(true);
                updateRenderGroupsVisibility(mMainView.getUiMode());

                mMainView.centerMapView(mFloorPlan.getCenter());
                // Locator uses floor plan for collision detection
                mLocator.setFloorPlan(mFloorPlan);
                mMainView.setEditingFloorPlanEnabled(true);
            }
        }

        public void update(Pair<String, String> buildingAndFloorIds) {
            mFloorId = buildingAndFloorIds.second;

            // TODO: what if this condition is NOT met? What will happen to mRadioTileReceived && mFloorPlanReceived?
            if (Building.current.getCurrentFloor() == null || !Building.current.getCurrentFloor().getId().equals(mFloorId)) {
                mDataAggregator.downloadFloorPlanAsync(mFloorId, mOnFloorPlanReceived);
                mDataAggregator.downloadRadioMapTileAsync(mFloorId, mFingerprint, mOnRadioTileReceived);
            }
        }
    }

    private class BuildingUpdater extends FloorUpdater {
        protected String mBuildingId;
        protected boolean mBuildingReceived = false;

        public BuildingUpdater(WiFiLocator.WiFiFingerprint fingerprint) {
            super(fingerprint);
        }

        private IFuckingSimpleGenericCallback<Building> mOnBuildingReceived = new IFuckingSimpleGenericCallback<Building>() {
            @Override
            public void onNotify(Building building) {
                Building.current = building;
                mBuildingReceived = true;
                onCompleteBuildingReceive();
            }
        };

        private synchronized void onCompleteBuildingReceive() {
            if (mBuildingReceived) {
                mFloorWatcher.setBuilding(Building.current);
                if (Building.current != null) mFloorWatcher.enable(mContext);
            }
        }

        public void update(Pair<String, String> buildingAndFloorIds) {
            mBuildingId = buildingAndFloorIds.first;

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
                mDataAggregator.getBuildingAsync(mBuildingId, mOnBuildingReceived);
            }

            super.update(buildingAndFloorIds);
        }
    }

    private IMainView.IRenderGroupChangedListener mFloorPlanChangedListener = new IMainView.IRenderGroupChangedListener() {
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

    private IMainView.IRenderGroupChangedListener mTeleportsChangedListener = new IMainView.IRenderGroupChangedListener() {
        @Override
        public void onElementAdd(IMoveable element) {
            Building.current.getCurrentFloor().addTeleport((Teleport) element);
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
            Building.current.getCurrentFloor().removeTeleport((Teleport) element);
            Building.current.setDirty(true);
            mMainView.setUploadButtonVisibility(true);
            mTeleportsLabelsRenderGroup.removeElement(element);
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

    private IMainView.IRenderGroupChangedListener mFingerprintsChangedListener = new IMainView.IRenderGroupChangedListener() {
        @Override
        public void onElementAdd(IMoveable element) {
            final Fingerprint fingerprint = (Fingerprint) element;
            mAugmentedRadioMap.add(fingerprint);
            Building.current.getCurrentFloor().addMacs(fingerprint.getFingerprint().keySet());
            Building.current.setDirty(true);
            mMainView.setUploadButtonVisibility(true);
        }

        @Override
        public void onElementChange(IMoveable element) {
            mMainView.setUploadButtonVisibility(true);
        }

        @Override
        public void onElementRemoved(IMoveable element) {
            mAugmentedRadioMap.remove(element);
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
                    if (params != null && params.length > 0 && params[0] instanceof String) {
                        String teleportId = (String) params[0];
                        Teleport teleport = new Teleport(location, teleportId);
                        mTeleportsLabelsRenderGroup.addItem(teleport); // this will render teleportId as text
                        mTeleportsElementsRenderGroup.addElement(teleport); // this will render teleport shape
                        return teleport;
                    }
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
//                    final IDataKeeper mazeServer = MazeServerMock.getInstance(MainActivity.this);
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

//        testLocalStorage();

        if (mStepCalibratorEnabled) {
            PermissionsHelper.handlePermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION, true,
                    "Please grant us fine location permission so we can calibrate our methods for indoor positioning.", mMainView, new IFuckingSimpleCallback() {
                        @Override
                        public void onNotified() {
                            mStepCalibratorService = new StepCalibratorService(mContext);
                            mStepCalibratorServiceIntent = new Intent(mContext, mStepCalibratorService.getClass());

                            if (!StepCalibratorService.isRunning(mContext)) {
                                mContext.startService(mStepCalibratorServiceIntent);
                            }
                        }
                    });
        }

        PermissionsHelper.handlePermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE, true,
                "Please grant us access to external storage. We will store there newly created maps and data.", mMainView, new IFuckingSimpleCallback() {
                    @Override
                    public void onNotified() {
                        initDataAggregator();                    }
                });

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
        // On start, disable possibility to change floor plan (create new walls etc)
        mMainView.setEditingFloorPlanEnabled(false);
    }

    public void initDataAggregator() {
        try {
            mDataAggregator = DataAggregator.getInstance(mContext);

            final AssetsDataProvider assetsDataProvider = new AssetsDataProvider(mContext);
            assetsDataProvider.init();
            mDataAggregator.addDataProvider(assetsDataProvider);

            final LocalStore localStore = new LocalStore();
            localStore.init(mContext);
            mDataAggregator.addDataProvider(localStore);
            mDataAggregator.setDataKeeper(localStore);
        } catch (IOException e) {
            mMainView.displayError("Failed to initialize local store (possibly no access to external storage).", true);
        }
    }

    private void testLocalStorage() {
        try {
            File storageLoc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM); //context.getExternalFilesDir(null);
            File newDir = new File(storageLoc, mContext.getApplicationContext().getApplicationInfo().loadLabel(
                    mContext.getApplicationContext().getPackageManager()) + "/");
            if (newDir.isDirectory()) {
                //direcoty exist
            } else {
                Log.d("MAKE DIR", newDir.mkdirs() + "");
            }

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) return;
            File testDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/test/");
            final String externalStorageState = Environment.getExternalStorageState();
            Log.d("STATE", externalStorageState);
            if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
                Log.d("PATH", testDir.getAbsolutePath());
                if (!testDir.exists()) {
                    Log.d("MAKE DIRS", testDir.mkdirs() + "");
                    Log.d("MAKE DIR", testDir.mkdir() + "");
                }
                File aFile = new File(testDir, "somefile");
                FileOutputStream fos = new FileOutputStream(aFile);
                fos.write("data".getBytes());
                fos.close();
            }
        } catch (IOException e){
            String msg = e.getMessage();
            Log.d("ERROR", msg);
        }
    }

    private void setUiHandlers() {
        // Occurs when user explicitly sets current floor
        mMainView.setFloorChangedHandler(this);

        mMainView.setUploadButtonClickListener(new IFuckingSimpleCallback() {
            private boolean mFloorPlanUploadRequested = false;
            private boolean mFloorPlanUploadDone = false;
            private boolean mBuildingUploadRequested = false;
            private boolean mBuildingUploadDone = false;
            private boolean mRadioMapUploadRequested = false;
            private boolean mRadioMapUploadDone = false;

            private IFuckingSimpleCallback mFloorPlanOnUploadDone = new IFuckingSimpleCallback() {
                @Override
                public void onNotified() {
                    mFloorPlanUploadDone = true;
                    allUploadDone();
                }
            };

            private IFuckingSimpleCallback mBuildingOnUploadDone = new IFuckingSimpleCallback() {
                @Override
                public void onNotified() {
                    mBuildingUploadDone = true;
                    allUploadDone();
                }
            };

            private IFuckingSimpleCallback mRadioMapOnUploadDone = new IFuckingSimpleCallback() {
                @Override
                public void onNotified() {
                    mRadioMapUploadDone = true;
                    allUploadDone();
                }
            };

            private synchronized void allUploadDone() {
                final boolean floorPlanDone = !mFloorPlanUploadRequested || mFloorPlanUploadDone;
                final boolean buildingDone = !mBuildingUploadRequested || mBuildingUploadDone;
                final boolean radioMapDone = !mRadioMapUploadRequested || mRadioMapUploadDone;

                if (floorPlanDone && buildingDone && radioMapDone) {
                    mMainView.setUploadButtonVisibility(false);
                }
            }

            @Override
            public void onNotified() {
                // TODO: Upload changes in floor plan, radio map, tags, teleports, ...
                if (mFloorPlan.isSketchDirty()) {
                    mFloorPlanUploadRequested = true;
                    mDataAggregator.upload(mFloorPlan, mFloorPlanOnUploadDone);
                    mFloorPlan.setSketchDirty(false);
                }


                if (Building.current.isDirty()) {
                    mBuildingUploadRequested = true;
                    mDataAggregator.upload(Building.current, mBuildingOnUploadDone);
                    Building.current.setDirty(false);
                }

                // New fingerprints were added?
                if (!mAugmentedRadioMap.isEmpty()) {
                    mRadioMapUploadRequested = true;
                    Floor currentFloor = Building.current.getCurrentFloor();
                    RadioMapFragment newRadioMapFragment = new RadioMapFragment(mAugmentedRadioMap, currentFloor.getId());
                    mDataAggregator.upload(newRadioMapFragment, mRadioMapOnUploadDone);
                }
            }
        });

        mMainView.setOnLocateMeEnabledChangedListener(new IFuckingSimpleGenericCallback<Boolean>() {
            @Override
            public void onNotify(Boolean locatorEnabled) {
                // TODO: If locatorEnabled, we should "track" user movement on the map, i.e. on new
                // TODO: location - center it
                // TODO: If not, don't center on new loacation
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
                updateRenderGroupsVisibility(uiMode);
            }
        });

        mMainView.setBuildingIdProvider(new IMainView.IAsyncIdProvider() {
            @Override
            public void generateId(IFuckingSimpleGenericCallback<String> idGeneratedCallback) {
                mDataAggregator.createBuildingAsync(idGeneratedCallback);
            }
        });

        mMainView.setFloorIdProvider(new IMainView.IAsyncIdProvider() {
            @Override
            public void generateId(IFuckingSimpleGenericCallback<String> idGeneratedCallback) {
                mDataAggregator.createFloorAsync(Building.current.getId(), idGeneratedCallback);
            }
        });
        
        mMainView.setSimilarBuildingsFinder(new IMainView.IAsyncSimilarBuildingsFinder() {
            @Override
            public void findBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {
                mDataAggregator.findSimilarBuildings(pattern, buildingsAcquiredCallback);
            }
        });

        mMainView.setBuildingCreator(new IMainView.IAsyncBuildingCreator() {
            @Override
            public void createBuilding(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback) {
                mDataAggregator.createBuildingAsync(name, type, address, buildingCreatedCallback);
            }
        });

        mMainView.setBuildingUpdater(new IFuckingSimpleGenericCallback<Building>() {
            @Override
            public void onNotify(Building building) {
                mDataAggregator.upload(building, new IFuckingSimpleCallback() {
                    @Override
                    public void onNotified() {
                        // Do nothing. Maybe indicate in UI that building has been updated on server?
                    }
                });
            }
        });
    }

    public void updateRenderGroupsVisibility(IMainView.UiMode uiMode) {
        switch (uiMode) {
            case MAP_VIEW_MODE:
                if (mRadioMapRenderGroup != null) mRadioMapRenderGroup.setVisible(false);
                if (mAugmentedRadioMapRenderGroup != null) mAugmentedRadioMapRenderGroup.setVisible(false);
                if (mTeleportsElementsRenderGroup != null) mTeleportsElementsRenderGroup.setVisible(false);
                if (mTeleportsLabelsRenderGroup!= null) mTeleportsLabelsRenderGroup.setVisible(false);
                break;
            case MAP_EDIT_MODE:
                if (mRadioMapRenderGroup != null) mRadioMapRenderGroup.setVisible(true);
                if (mAugmentedRadioMapRenderGroup != null) mAugmentedRadioMapRenderGroup.setVisible(true);
                if (mTeleportsElementsRenderGroup != null) mTeleportsElementsRenderGroup.setVisible(true);
                if (mTeleportsLabelsRenderGroup!= null) mTeleportsLabelsRenderGroup.setVisible(true);
                break;
        }
    }

    @Override
    public void onResume() {
        mWifiScanner.onResume(mContext);
        mLocator.addLocationUpdatedListener(this);
        mSensorListener.addDeviceRotationListener(this);
        mMapper.setOnNewFingerprintListener(this);
        mMapper.setEnabled(mMapperLastState);
    }

    @Override
    public void onPause() {
        // to stop the listener and save battery
        mWifiScanner.onPause(mContext);

        mLocator.removeLocationUpdatedListener(this);
        mSensorListener.removeDeviceRotationListener(this);
        mMapperLastState = mMapper.isEnabled();
        mMapper.setEnabled(false);
    }

    @Override
    public void onDestroy() {
        if (mStepCalibratorEnabled) {
            if (StepCalibratorService.isRunning(mContext)) {
                // In case the permissions were granted previous time the app was running, we can
                // start the service. The service will start by stopping it :) Because this forces restart.
                mContext.stopService(mStepCalibratorServiceIntent); // This will resurrect the service
            }
        }
        mWifiScanner.removeFingerprintAvailableListener(mFirstFingerprintAvailableListener);
        if (mLocator != null) {
            mLocator.onDestroy();
            mLocator = null;
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
        drawUserLocation(location);
    }

    // This callback is executed when mapper is active and it obtained a new fingerprint
    @Override
    public void onNotify(Fingerprint fingerprint) {
        // TODO: It should not be the case that any newly created fingerprint is added to the radiomap
        // TODO: Instead, we should examine fingerprint's quality and only after that add it to SEPARATE
        // TODO: collection which later will be uploaded to server.

        // This call will trigger event that will put new fingerprint to mAugmentedRadioMap
        mAugmentedRadioMapRenderGroup.addElement(fingerprint);
    }

    @Override
    public void onFloorChanged(Floor newFloor) {
        final Floor currentFloor = Building.current.getCurrentFloor();
        if (currentFloor != null && currentFloor.getId().equals(newFloor.getId())) return;

        // Clean up all floor-related data like radio map, floor plan, tags, ...
        mMainView.clearRenderedElements();
        if (mFloorPlan != null) mFloorPlan.clear();
        if (mRadioMapFragment != null) mRadioMapFragment.clear();
        if (mAugmentedRadioMap != null) mAugmentedRadioMap.clear();

        createNewRenderGroups(newFloor.getId());

        // Load new data from newFloor, render
        WiFiLocator.WiFiFingerprint lastFingerprint = mWifiScanner.getLastFingerprint();
        FloorUpdater floorUpdater = new FloorUpdater(lastFingerprint);
        floorUpdater.update(new Pair<String, String>(null, newFloor.getId()));
    }

    private void createNewRenderGroups(String floorId) {
        mFloorPlanRenderGroup = mMainView.createElementsRenderGroup(null);
        mFloorPlanRenderGroup.setVisible(true);
        mFloorPlanRenderGroup.setChangedListener(mFloorPlanChangedListener);

        mTagsRenderGroup = mMainView.createTextRenderGroup(null);
        mTagsRenderGroup.setVisible(true);
        mTagsRenderGroup.setChangedListener(mTagsChangedListener);

        // These groups are visible in Edit Mode only
        mTeleportsLabelsRenderGroup = mMainView.createTextRenderGroup(null);
        mTeleportsElementsRenderGroup = mMainView.createElementsRenderGroup(null);
        mTeleportsElementsRenderGroup.setChangedListener(mTeleportsChangedListener);
        mRadioMapRenderGroup = mMainView.createElementsRenderGroup(null);
        mAugmentedRadioMapRenderGroup = mMainView.createElementsRenderGroup(null);
        mAugmentedRadioMapRenderGroup.setChangedListener(mFingerprintsChangedListener);
        updateRenderGroupsVisibility(mMainView.getUiMode());

        mFloorPlan = new FloorPlan(floorId);
        mLocator.setFloorPlan(mFloorPlan);
        mMainView.setEditingFloorPlanEnabled(true);
    }

    private void drawUserLocation(PointF location) {
        // This render group can be common for all floors and buildings
        if (null == mAuxiliaryRenderGroup) {
            mAuxiliaryRenderGroup = mMainView.createElementsRenderGroup(null);
            mAuxiliaryRenderGroup.setVisible(true);
        }
        if (null == mLocationMark) {
            mLocationMark = new Ring(location.x, location.y, 1f, 3f, 24);
            mLocationMark.setColor(Color.RED);
            mAuxiliaryRenderGroup.addElement(mLocationMark);
        }

        mLocationMark.setCenter(location.x, location.y);
        mMainView.updateElement(mLocationMark);
    }
}

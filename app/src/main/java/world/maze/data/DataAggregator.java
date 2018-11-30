package world.maze.data;

import android.content.Context;
import android.support.v4.util.Pair;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Building;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;

/**
 * Created by Greg Stein on 8/31/2018.
 */

public class DataAggregator implements IDataProvider, IDataKeeper {
    public static final String FLORPLANS_SUBDIR = "floorplans";
    public static final String RADIOMAPS_SUBDIR = "radiomaps";
    public static final String BUILDINGS_SUBDIR = "buildings";
    public static final String JSON_EXT = ".json";
    private final Context mContext;

    private List<IDataProvider> mDataProviders = new ArrayList<>();
    private IDataProvider mCurrentDataProvider = null;
    private Map<String, IDataProvider> mBuildingToDataProviderMap = new HashMap<>();
    private IDataKeeper mDataKeeper;

    private static volatile DataAggregator instance = null;
    private static final Object mutex = new Object();
    public static DataAggregator getInstance(Context context) {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new DataAggregator(context);
                }
            }
        }
        return instance;
    }

    private DataAggregator(Context context) {
        mContext = context;
    }

    public void addDataProvider(IDataProvider dataProvider) {
        mDataProviders.add(dataProvider);

        Iterable<String> providerBuildingIds = dataProvider.getBuildingIds();

        if (providerBuildingIds != null) {
            for (String buildingId : providerBuildingIds) {
                if (!mBuildingToDataProviderMap.containsKey(buildingId)) {
                    mBuildingToDataProviderMap.put(buildingId, dataProvider);
                }
            }
        }
    }

    public void setDataKeeper(IDataKeeper dataKeeper) {
        mDataKeeper = dataKeeper;
    }

    @Override
    public void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated) {
        mDataKeeper.createBuildingAsync(onBuildingCreated);
    }

    @Override
    public void createBuildingAsync(String name, String type, String address, final IFuckingSimpleGenericCallback<Building> buildingCreatedCallback) {
        mDataKeeper.createBuildingAsync(name, type, address, new IFuckingSimpleGenericCallback<Building>() {
            @Override
            public void onNotify(Building building) {
                // If data keeper is also data provider, it will hold the building
                if (mDataKeeper instanceof IDataProvider) {
                    mCurrentDataProvider = (IDataProvider) mDataKeeper;
                } else {
                    // Find who holds newly created building
                    for (IDataProvider dataProvider : mDataProviders) {
                        if (dataProvider.hasId(building.getId())) {
                            mCurrentDataProvider = dataProvider;
                            break;
                        }
                    }
                }
                buildingCreatedCallback.onNotify(building);
            }
        });
    }

    @Override
    public void createFloorAsync(String buildingId, IFuckingSimpleGenericCallback<String> onFloorCreated) {
        mDataKeeper.createFloorAsync(buildingId, onFloorCreated);
    }

    @Override
    public void upload(Building building, IFuckingSimpleCallback onDone) {
        mDataKeeper.upload(building, onDone);
    }

    @Override
    public void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone) {
        mDataKeeper.upload(floorPlan, onDone);
    }

    @Override
    public void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone) {
        mDataKeeper.upload(radioMap, onDone);
    }

    @Override
    public void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {
        for (IDataProvider dataProvider : mDataProviders) {
            // TODO
        }
    }

    @Override
    public void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived) {
        // This data provider was set previously during search for building and floor.
        if (mCurrentDataProvider == null) {
            onBuildingReceived.onNotify(null);
            return;
        }

        mCurrentDataProvider.getBuildingAsync(buildingId, onBuildingReceived);
    }

    private Pair<String, String> mFoundBuildingAndFloor = null;
    private int providersReturnedEmptyBuildingAndFloor;

    @Override
    public void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, final IFuckingSimpleGenericCallback<Pair<String, String>> callback) {
        final Object lock = new Object();
        mFoundBuildingAndFloor = null;
        providersReturnedEmptyBuildingAndFloor = 0;

        // Get data provider that holds the building
        for (final IDataProvider dataProvider : mDataProviders) {
            dataProvider.findCurrentBuildingAndFloorAsync(fingerprint, new IFuckingSimpleGenericCallback<Pair<String, String>>() {
                @Override
                public void onNotify(Pair<String, String> foundBuildingAndFloor) {
                    synchronized (lock) {

                        if (buildingAndFloorAreEmpty(foundBuildingAndFloor)) {
                            providersReturnedEmptyBuildingAndFloor++;
                            if (mDataProviders.size() == providersReturnedEmptyBuildingAndFloor) {
                                callback.onNotify(foundBuildingAndFloor);
                            }
                            return;
                        }
                        // Not empty building and floor
                        if (null == mFoundBuildingAndFloor) {               // first update?
                            mFoundBuildingAndFloor = foundBuildingAndFloor;
                            callback.onNotify(mFoundBuildingAndFloor);
                            mCurrentDataProvider = dataProvider; // remember data provider that holds current building
                        }                                        // it will hold the floor data as well
                    }
                }

                public boolean buildingAndFloorAreEmpty(Pair<String, String> foundBuildingAndFloor) {
                    return null == foundBuildingAndFloor ||
                            StringUtils.isEmpty(foundBuildingAndFloor.first) ||
                            StringUtils.isEmpty(foundBuildingAndFloor.second);
                }
            });
        }
    }

    @Override
    public void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived) {
        // This data provider was set previously during search for building and floor.
        if (mCurrentDataProvider == null) {
            onFloorPlanReceived.onNotify(null);
            return;
        }

        mCurrentDataProvider.downloadFloorPlanAsync(floorId, onFloorPlanReceived);
    }

    @Override
    public void downloadRadioMapTileAsync(String floorId, WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived) {
        // This data provider was set previously during search for building and floor.
        if (mCurrentDataProvider == null) {
            onRadioTileReceived.onNotify(null);
            return;
        }

        mCurrentDataProvider.downloadRadioMapTileAsync(floorId, fingerprint, onRadioTileReceived);
    }

    @Override
    public Iterable<String> getBuildingIds() {
        return mBuildingToDataProviderMap.keySet();
    }

    @Override
    public boolean hasId(String id) {
        for (IDataProvider dataProvider : mDataProviders) {
            if (dataProvider.hasId(id)) return true;
        }

        return false;
    }
}

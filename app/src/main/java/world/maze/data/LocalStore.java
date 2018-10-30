package world.maze.data;

import android.content.Context;
import android.os.Environment;
import android.support.v4.util.Pair;
import android.widget.Toast;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Building;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
import world.maze.util.IFuckingSimpleCallback;
import world.maze.util.IFuckingSimpleGenericCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by Greg Stein on 8/11/2018.
 */

public class LocalStore implements IDataProvider, IDataKeeper {

    public static final String DATA_ROOT = "maze";

    private String[] mFloorplanIds;
    private String[] mRadioMapIds;
    private String[] mBuildingIds;

    private File mFloorplansDir;
    private File mRadiomapsDir;
    private File mBuildingsDir;

    public void init(Context context) throws IOException {
        final String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
            final String dataRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + DATA_ROOT + "/";
            mFloorplansDir = new File(dataRoot + DataAggregator.FLORPLANS_SUBDIR);
            mFloorplanIds = listInitDirectory(mFloorplansDir);
            mRadiomapsDir = new File(dataRoot + DataAggregator.RADIOMAPS_SUBDIR);
            mRadioMapIds = listInitDirectory(mRadiomapsDir);
            mBuildingsDir = new File(dataRoot + DataAggregator.BUILDINGS_SUBDIR);
            mBuildingIds = listInitDirectory(mBuildingsDir);
        }
    }

    private static String[] listInitDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) throw new IOException("Failed to create directory path.");
            return new String[0];
        }
        return dir.list();
    }

    public static void save(Context context, String data, String directory, String filename) {
        FileOutputStream fos;
        ObjectOutputStream os;

        try {
            File aFile = new File(directory, filename);
            fos = new FileOutputStream(aFile, false);
            os = new ObjectOutputStream(fos);
            os.writeObject(data);
            os.close();
            fos.close();
        } catch (IOException e) {
            Toast.makeText(context, "Error saving data locally.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static String load(Context context, String store) {
        String data = null;
        FileInputStream fis;
        ObjectInputStream is;

        try {
//            if (aFile.exists() && aFile.isFile()) {
//            }
            fis = context.openFileInput(store);
            is = new ObjectInputStream(fis);
            data = (String) is.readObject();
            is.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            Toast.makeText(context, "Error loading data from store.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return data;
    }

    @Override
    public void findSimilarBuildings(String pattern, IFuckingSimpleGenericCallback<List<Building>> buildingsAcquiredCallback) {

    }

    @Override
    public void createBuildingAsync(IFuckingSimpleGenericCallback<String> onBuildingCreated) {

    }

    @Override
    public void createBuildingAsync(String name, String type, String address, IFuckingSimpleGenericCallback<Building> buildingCreatedCallback) {

    }

    @Override
    public void createFloorAsync(IFuckingSimpleGenericCallback<String> onFloorCreated) {

    }

    @Override
    public void upload(Building building, IFuckingSimpleCallback onDone) {

    }

    @Override
    public void upload(FloorPlan floorPlan, IFuckingSimpleCallback onDone) {

    }

    @Override
    public void upload(RadioMapFragment radioMap, IFuckingSimpleCallback onDone) {

    }

    @Override
    public void getBuildingAsync(String buildingId, IFuckingSimpleGenericCallback<Building> onBuildingReceived) {

    }

    @Override
    public void findCurrentBuildingAndFloorAsync(WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<Pair<String, String>> callback) {

    }

    @Override
    public void downloadFloorPlanAsync(String floorId, IFuckingSimpleGenericCallback<FloorPlan> onFloorPlanReceived) {

    }

    @Override
    public void downloadRadioMapTileAsync(String floorId, WiFiLocator.WiFiFingerprint fingerprint, IFuckingSimpleGenericCallback<RadioMapFragment> onRadioTileReceived) {

    }

    @Override
    public Collection<String> getBuildingIds() {
        return Arrays.asList(mBuildingIds);
    }
}

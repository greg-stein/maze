package world.maze.floorplan;

import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Greg Stein on 9/27/2016.
 */
public class PersistenceLayer {
    private static final String LOCAL_FLOOR_PLAN_STORE = "floorplan.wad";

    public static void save(Context context, String data, String store) {
        FileOutputStream fos;
        ObjectOutputStream os;

        try {
            fos = context.openFileOutput(store, Context.MODE_PRIVATE);
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

    public static void saveFloorPlan(Context context, String serializedFloorPlan) {
        FileOutputStream fos;

        try {
            fos = context.openFileOutput(LOCAL_FLOOR_PLAN_STORE, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(serializedFloorPlan);
            os.close();
            fos.close();
        } catch (IOException e) {
            Toast.makeText(context, "Error saving floor plan locally.", Toast.LENGTH_SHORT);
            e.printStackTrace();
        }
    }

    @Nullable
    public static String loadFloorPlan(Context context) {
        String floorplan = null;
        FileInputStream fis;

        try {
            fis = context.openFileInput(LOCAL_FLOOR_PLAN_STORE);
            ObjectInputStream is = new ObjectInputStream(fis);
            floorplan = (String) is.readObject();
            is.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            Toast.makeText(context, "Error loading floor plan from local store.", Toast.LENGTH_SHORT);
            e.printStackTrace();
        }

        return floorplan;
    }
}

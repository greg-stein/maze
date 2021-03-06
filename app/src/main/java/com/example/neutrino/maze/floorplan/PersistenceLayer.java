package com.example.neutrino.maze.floorplan;

import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.example.neutrino.maze.AppSettings;

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

    public static void saveFloorPlan(String serializedFloorPlan) {
        Context context = AppSettings.appActivity;
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
    public static String loadFloorPlan() {
        Context context = AppSettings.appActivity;
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

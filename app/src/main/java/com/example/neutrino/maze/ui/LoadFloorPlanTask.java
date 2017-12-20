package com.example.neutrino.maze.ui;

import android.content.Context;
import android.os.AsyncTask;

import com.example.neutrino.maze.core.Locator;
import com.example.neutrino.maze.core.WiFiLocator;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;

import java.util.List;

/**
 * Created by Greg Stein on 5/2/2017.
 */

public class LoadFloorPlanTask extends AsyncTask<String, Void, FloorPlan> {
    private final Context mContext;
    private AsyncResponse onFinishHandler;

    public LoadFloorPlanTask(Context context) {
        this.mContext = context;
    }

    @Override
    protected FloorPlan doInBackground(String... strings) {
        String jsonString = strings[0];

        FloorPlan floorPlan = null;
        if (jsonString != null) {
            List<Object> floorplan = FloorPlanSerializer.deserializeFloorPlan(jsonString);
            floorPlan = FloorPlan.build(floorplan);
            // All this class is dead code
            // TODO: remove it
//            WiFiLocator.getInstance().setFingerprintsMap(floorPlan.getFingerprints());
            Locator.getInstance(mContext).setFloorPlan(floorPlan);
        }
        return floorPlan;
    }

    public interface AsyncResponse {
        void onFinish(FloorPlan floorPlan);
    }

    public LoadFloorPlanTask onFinish(AsyncResponse asyncResponse) {
        this.onFinishHandler = asyncResponse;
        return this;
    }

    @Override
    // Runs on UI thread
    protected void onPostExecute(FloorPlan floorPlan) {
        if (onFinishHandler != null) {
            onFinishHandler.onFinish(floorPlan);
        }
    }
}

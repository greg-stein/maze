package com.example.neutrino.maze;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.rendering.FloorPlanView;

import java.util.List;

/**
 * Created by Greg Stein on 5/2/2017.
 */

public class LoadFloorPlanTask extends AsyncTask<String, Void, FloorPlan> {
    private AsyncResponse onFinishHandler;

    @Override
    protected FloorPlan doInBackground(String... strings) {
        String jsonString = strings[0];

        FloorPlan floorPlan = null;
        if (jsonString != null) {
            List<Object> floorplan = FloorPlanSerializer.deserializeFloorPlan(jsonString);
            floorPlan = FloorPlan.build(floorplan);
            WiFiLocator.getInstance().setFingerprintsMap(floorPlan.getFingerprints());
            Locator.getInstance().setFloorPlan(floorPlan);
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

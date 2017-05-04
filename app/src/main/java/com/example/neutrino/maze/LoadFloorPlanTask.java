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
    private FloorPlanView mView;
    private RecyclerView mRecView;

    public LoadFloorPlanTask(FloorPlanView view, RecyclerView recView) {
        mView = view;
        mRecView = recView;
    }

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

    @Override
    // Runs on UI thread
    protected void onPostExecute(FloorPlan floorPlan) {
        TagsAdapter adapter = new TagsAdapter(floorPlan.getTags(), AppSettings.appActivity);
        mRecView.setAdapter(adapter);

        // The main work is done on GL thread!
        mView.plot(floorPlan);
    }
}

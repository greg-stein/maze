package world.maze.ui;

import android.content.Context;
import android.os.AsyncTask;

import world.maze.core.IFloorChangedHandler;
import world.maze.core.Locator;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.FloorPlanSerializer;
import world.maze.floorplan.IFloorPlanPrimitive;
import world.maze.util.JsonSerializer;

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
            floorPlan = JsonSerializer.deserialize(jsonString, FloorPlan.class);
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

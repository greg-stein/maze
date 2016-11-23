package com.example.neutrino.maze;

import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 11/23/2016.
 */
public class CommonHelper {
    public static final <T extends IFloorPlanPrimitive> List<T> getPrimitives(Class<T> klazz, List<IFloorPlanPrimitive> floorPlan) {
        List<T> result = new ArrayList<>();
        for(IFloorPlanPrimitive primitive : floorPlan) {
            if (primitive.getClass().equals(klazz)) {
                result.add(klazz.cast(primitive));
            }
        }

        return result;
    }
}

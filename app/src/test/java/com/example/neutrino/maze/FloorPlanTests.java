package com.example.neutrino.maze;

/**
 * Created by Greg Stein on 5/9/2017.
 */

import android.graphics.RectF;

import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.LocationMark;
import com.example.neutrino.maze.floorplan.Wall;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 5/9/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class FloorPlanTests {
    @Test
    public void getBoundariesTest() {
        FloorPlan floorPlan = FloorPlan.build();
        List<IFloorPlanPrimitive> sketch = floorPlan.getSketch();
        sketch.add(new Wall(1, 4, 3, 2));
        sketch.add(new LocationMark(1, 4, 3, 5));
        sketch.add(new Footprint(9, 4, 1));

        RectF boundaries = floorPlan.getBoundaries();
        assertThat(boundaries.left, is(equalTo(-4f)));
        assertThat(boundaries.top, is(equalTo(-1f)));
        assertThat(boundaries.right, is(equalTo(10f)));
        assertThat(boundaries.bottom, is(equalTo(9f)));
    }
}

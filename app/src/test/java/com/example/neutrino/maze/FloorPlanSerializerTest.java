package com.example.neutrino.maze;

import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.LocationMark;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.WifiMark;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 9/26/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class FloorPlanSerializerTest {

    @Test
    public void CommonSerializationUnitTest() {
        List<IFloorPlanPrimitive> originalList = new ArrayList<>();
        originalList.add(new Wall(0.1f, 0.2f, 0.3f, 0.4f));
        originalList.add(new Footprint(0.5f, 0.6f));
        originalList.add(new WifiMark(0.7f, 0.8f, null));
        originalList.add(new LocationMark(0.9f, 1.1f, 1.2f, 1.3f));

        String jsonString = FloorPlanSerializer.serializeFloorPlan(originalList);

        List<IFloorPlanPrimitive> deserializedList = FloorPlanSerializer.deserializeFloorPlan(jsonString);

        assertThat(deserializedList.size(), is(originalList.size()));

        assertThat(deserializedList.get(0), is(instanceOf(Wall.class)));
        assertThat(deserializedList.get(0), is(equalTo(originalList.get(0))));

        assertThat(deserializedList.get(1), is(instanceOf(Footprint.class)));
        assertThat(deserializedList.get(1), is(equalTo(originalList.get(1))));

        assertThat(deserializedList.get(2), is(instanceOf(WifiMark.class)));
        assertThat(deserializedList.get(2), is(equalTo(originalList.get(2))));

        assertThat(deserializedList.get(3), is(instanceOf(LocationMark.class)));
        assertThat(deserializedList.get(3), is(equalTo(originalList.get(3))));
    }

    @Test
    public void deserializeFromResourcesTest() {
        String jsonString = null;

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            // Get json file from test resources: app/src/test/resources
            InputStream in_s = classLoader.getResourceAsStream("floorplan_greg_home_2nd_floor.json");

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            jsonString = new String(b);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<IFloorPlanPrimitive> deserializedList = FloorPlanSerializer.deserializeFloorPlan(jsonString);

        assertNotNull(deserializedList);
        assertThat(deserializedList, hasSize(70));
    }
}

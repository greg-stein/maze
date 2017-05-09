package com.example.neutrino.maze;

import com.example.neutrino.maze.floorplan.FloorPlanDescriptor;
import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.LocationMark;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.Fingerprint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
@Config(manifest=Config.NONE, sdk = 23)
public class FloorPlanSerializerTest {

    @Test
    public void FloorplanDescriptorSerializationTest() {
        FloorPlanDescriptor descriptor = new FloorPlanDescriptor();
        descriptor.setFloorPlanName("קרקע");
        List<String> buildingNames = new ArrayList<>();
        buildingNames.add("קניון חדש");
        buildingNames.add("קניון לב כרמיאל");
        descriptor.setBuildingNames(buildingNames);

        List<Object> descriptors = new ArrayList<>();
        descriptors.add(descriptor);
        String jsonString = FloorPlanSerializer.serializeFloorPlan(descriptors);
    }

    @Test
    public void CommonSerializationUnitTest() {
        List<Object> originalList = new ArrayList<>();
        originalList.add(new Wall(0.1f, 0.2f, 0.3f, 0.4f));
        originalList.add(new Footprint(0.5f, 0.6f));
        originalList.add(new Fingerprint(0.7f, 0.8f, null));
        originalList.add(new LocationMark(0.9f, 1.1f, 1.2f, 1.3f));

        String jsonString = FloorPlanSerializer.serializeFloorPlan(originalList);

        List<Object> deserializedList = FloorPlanSerializer.deserializeFloorPlan(jsonString);

        assertThat(deserializedList.size(), is(originalList.size()));

        assertThat(deserializedList.get(0), is(instanceOf(Wall.class)));
        assertThat(deserializedList.get(0), is(equalTo(originalList.get(0))));

        assertThat(deserializedList.get(1), is(instanceOf(Footprint.class)));
        assertThat(deserializedList.get(1), is(equalTo(originalList.get(1))));

        assertThat(deserializedList.get(2), is(instanceOf(Fingerprint.class)));
        assertThat(deserializedList.get(2), is(equalTo(originalList.get(2))));
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

        List<Object> deserializedList = FloorPlanSerializer.deserializeFloorPlan(jsonString);

        assertNotNull(deserializedList);
        assertThat(deserializedList, hasSize(70));
    }
}

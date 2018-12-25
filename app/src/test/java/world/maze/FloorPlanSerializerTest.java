package world.maze;

import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.FloorPlanSerializer;
import world.maze.floorplan.LocationMark;
import world.maze.floorplan.Wall;
import world.maze.floorplan.Fingerprint;
import world.maze.util.JsonSerializer;

import org.junit.Assert;
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
@Config(manifest=Config.NONE, sdk = 23)
public class FloorPlanSerializerTest {

    @Test
    public void emptyFloorPlanDeserializationTest() {
        String jsonString = "{\"mFloorId\":\"0225fcd1-8c41-4600-849a-1c8c4af2c825\",\"mIsSketchDirty\":false}";
        FloorPlan floorPlan = JsonSerializer.deserialize(jsonString, FloorPlan.class);

        assertNotNull(floorPlan);
        assertThat(floorPlan.getFloorId(), equalTo("0225fcd1-8c41-4600-849a-1c8c4af2c825"));
    }

    @Test
    public void emptyFloorPlanSerializationTest() {
        FloorPlan floorPlan = new FloorPlan("0225fcd1-8c41-4600-849a-1c8c4af2c825");
        String jsonString = JsonSerializer.serialize(floorPlan);

        assertNotNull(jsonString);
        assertThat(jsonString, equalTo("{\"mSketch\":[],\"mFloorId\":\"0225fcd1-8c41-4600-849a-1c8c4af2c825\"}"));
    }

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
        originalList.add(new Fingerprint(0.7f, 0.8f, null));
        originalList.add(new LocationMark(0.9f, 1.1f, 1.2f, 1.3f));

        String jsonString = FloorPlanSerializer.serializeFloorPlan(originalList);

        List<Object> deserializedList = FloorPlanSerializer.deserializeFloorPlan(jsonString);

        assertThat(deserializedList.size(), is(originalList.size()));

        assertThat(deserializedList.get(0), is(instanceOf(Wall.class)));
        assertThat(deserializedList.get(0), is(equalTo(originalList.get(0))));

        assertThat(deserializedList.get(1), is(instanceOf(Fingerprint.class)));
        assertThat(deserializedList.get(1), is(equalTo(originalList.get(1))));
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

    /**
     * Created by Greg Stein on 4/4/2017.
     */

    public static class FloorPlanDescriptor {
        private String mFloorPlanName;
        private List<String> mBuildingNames;

        public String getFloorPlanName() {
            return mFloorPlanName;
        }

        public void setFloorPlanName(String mFloorPlanName) {
            this.mFloorPlanName = mFloorPlanName;
        }

        public List<String> getBuildingNames() {
            return mBuildingNames;
        }

        public void setBuildingNames(List<String> mBuildingNames) {
            this.mBuildingNames = mBuildingNames;
        }
    }
}

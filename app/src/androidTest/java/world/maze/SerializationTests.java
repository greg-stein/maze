package world.maze;

import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.Wall;
import world.maze.util.JsonSerializer;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 12/25/2018.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SerializationTests {
    @Test
    public void singleElementSerializationTest() {
        FloorPlan floorPlan = new FloorPlan("0225fcd1-8c41-4600-849a-1c8c4af2c825");
        floorPlan.getSketch().add(new Wall(0.1f, 0.2f, 0.3f, 0.4f));
        String jsonString = JsonSerializer.serialize(floorPlan);
        String expectedJson = "{\"mFloorId\":\"0225fcd1-8c41-4600-849a-1c8c4af2c825\",\"mSketch\":[{\"CLASSNAME\":\"Wall\",\"INSTANCE\":{\"mEnd\":{\"x\":0.3,\"y\":0.4},\"mStart\":{\"x\":0.1,\"y\":0.2},\"mThickness\":0.2}}]}";

        assertNotNull(jsonString);
        assertThat(jsonString, equalTo(expectedJson));
    }

    @Test
    public void singleElementDeserializationTest() {
        String jsonString = "{\"mFloorId\":\"0225fcd1-8c41-4600-849a-1c8c4af2c825\",\"mSketch\":[{\"CLASSNAME\":\"Wall\",\"INSTANCE\":{\"mEnd\":{\"x\":0.3,\"y\":0.4},\"mStart\":{\"x\":0.1,\"y\":0.2},\"mThickness\":0.2}}]}";
        FloorPlan floorPlan = JsonSerializer.deserialize(jsonString, FloorPlan.class);

        assertNotNull(floorPlan);
        assertThat(floorPlan.getFloorId(), equalTo("0225fcd1-8c41-4600-849a-1c8c4af2c825"));
        assertNotNull(floorPlan.getSketch());
        assertThat(floorPlan.getSketch().size(), equalTo(1));
        assertThat(floorPlan.getSketch().get(0), instanceOf(Wall.class));
        Wall wall = (Wall)floorPlan.getSketch().get(0);
        assertThat(wall.getStart().x, equalTo(0.1f));
        assertThat(wall.getStart().y, equalTo(0.2f));
        assertThat(wall.getEnd().x, equalTo(0.3f));
        assertThat(wall.getEnd().y, equalTo(0.4f));
        assertThat(wall.getThickness(), equalTo(0.2f));
    }
}

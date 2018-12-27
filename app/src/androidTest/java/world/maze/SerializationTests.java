package world.maze;

import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import world.maze.core.WiFiLocator;
import world.maze.floorplan.Fingerprint;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.RadioMapFragment;
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

    @Test
    public void radioMapSingleFingerprintSerializationTest() {
        WiFiLocator.WiFiFingerprint wiFiFingerprint1 = new WiFiLocator.WiFiFingerprint();
        wiFiFingerprint1.put("01:02:03:04:05:06", 75);
        wiFiFingerprint1.put("0a:0b:0c:0d:0e:0f", 87);
        Fingerprint fingerprint1 = new Fingerprint(1.2f, 3.4f, wiFiFingerprint1);
        WiFiLocator.WiFiFingerprint wiFiFingerprint2 = new WiFiLocator.WiFiFingerprint();
        wiFiFingerprint2.put("07:08:09:0a:0b:0c", 53);
        wiFiFingerprint2.put("0f:0e:0d:0c:0b:0a", 91);
        Fingerprint fingerprint2 = new Fingerprint(5.6f, 7.8f, wiFiFingerprint2);
        List<Fingerprint> fingerprints = new ArrayList<>();
        fingerprints.add(fingerprint1);
        fingerprints.add(fingerprint2);
        String floorId = "0225fcd1-8c41-4600-849a-1c8c4af2c825";
        RadioMapFragment radioMapFragment = new RadioMapFragment(fingerprints, floorId);

        String json = JsonSerializer.serialize(radioMapFragment);
        String expectedJson =
                "{\"mFingerprints\":[" +
                    "{" +
                        "\"mWiFiFingerprint\":{" +
                            "\"0f:0e:0d:0c:0b:0a\":91," +
                            "\"07:08:09:0a:0b:0c\":53}," +
                        "\"mCenter\":{\"x\":5.6,\"y\":7.8}" +
                    "}," +
                    "{" +
                        "\"mWiFiFingerprint\":{" +
                            "\"0a:0b:0c:0d:0e:0f\":87," +
                            "\"01:02:03:04:05:06\":75}," +
                        "\"mCenter\":{\"x\":1.2,\"y\":3.4}" +
                    "}" +
                "]," +
                "\"mFloorId\":\"0225fcd1-8c41-4600-849a-1c8c4af2c825\"}";
        assertNotNull(json);
        assertThat(json, equalTo(expectedJson));
    }

    @Test
    public void radioMapSingleFingerprintDeserializationTest() {
        String json =
                "{\"mFingerprints\":[" +
                    "{" +
                        "\"mWiFiFingerprint\":{" +
                            "\"0a:0b:0c:0d:0e:0f\":87," +
                            "\"01:02:03:04:05:06\":75}," +
                        "\"mCenter\":{\"x\":1.2,\"y\":3.4}" +
                    "}," +
                    "{" +
                        "\"mWiFiFingerprint\":{" +
                            "\"0f:0e:0d:0c:0b:0a\":91," +
                            "\"07:08:09:0a:0b:0c\":53}," +
                        "\"mCenter\":{\"x\":5.6,\"y\":7.8}" +
                    "}" +
                "]," +
                "\"mFloorId\":\"0225fcd1-8c41-4600-849a-1c8c4af2c825\"}";

        RadioMapFragment fragment = JsonSerializer.deserialize(json, RadioMapFragment.class);

        assertNotNull(fragment);
        assertThat(fragment.getFloorId(), equalTo("0225fcd1-8c41-4600-849a-1c8c4af2c825"));
        assertNotNull(fragment.getFingerprints());
        assertThat(fragment.getFingerprints().size(), equalTo(2));

        Iterator<Fingerprint> iter = fragment.getFingerprints().iterator();
        Fingerprint fingerprint1Actual = iter.next();
        assertNotNull(fingerprint1Actual);
        assertNotNull(fingerprint1Actual.getFingerprint());
        assertThat(fingerprint1Actual.getFingerprint().size(), equalTo(2));
        assertTrue(fingerprint1Actual.getFingerprint().containsKey("0a:0b:0c:0d:0e:0f"));
        assertThat(fingerprint1Actual.getFingerprint().get("0a:0b:0c:0d:0e:0f"), equalTo(87));
        assertTrue(fingerprint1Actual.getFingerprint().containsKey("01:02:03:04:05:06"));
        assertThat(fingerprint1Actual.getFingerprint().get("01:02:03:04:05:06"), equalTo(75));
        assertThat(fingerprint1Actual.getCenter().x, equalTo(1.2f));
        assertThat(fingerprint1Actual.getCenter().y, equalTo(3.4f));

        Fingerprint fingerprint2Actual = iter.next();
        assertNotNull(fingerprint2Actual);
        assertNotNull(fingerprint2Actual.getFingerprint());
        assertThat(fingerprint2Actual.getFingerprint().size(), equalTo(2));
        assertTrue(fingerprint2Actual.getFingerprint().containsKey("0f:0e:0d:0c:0b:0a"));
        assertThat(fingerprint2Actual.getFingerprint().get("0f:0e:0d:0c:0b:0a"), equalTo(91));
        assertTrue(fingerprint2Actual.getFingerprint().containsKey("07:08:09:0a:0b:0c"));
        assertThat(fingerprint2Actual.getFingerprint().get("07:08:09:0a:0b:0c"), equalTo(53));
        assertThat(fingerprint2Actual.getCenter().x, equalTo(5.6f));
        assertThat(fingerprint2Actual.getCenter().y, equalTo(7.8f));
    }
}

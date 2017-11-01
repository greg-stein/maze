package com.example.neutrino.maze;

import android.net.wifi.ScanResult;
import android.os.Parcel;

import com.example.neutrino.maze.core.WiFiLocator;
import com.example.neutrino.maze.util.MovingAverageScanResultsQueue;
import com.google.common.collect.Iterables;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 10/25/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class MovingAverageQueueTests {

    private static final Logger LOGGER = Logger.getLogger(MovingAverageQueueTests.class.getName());
    public static final String MAC1 = "01:02:03:04:05:06";
    public static final String MAC2 = "0A:0B:0C:0D:0E:0F";

    @Test
    public void SomeTest() {
        Map<String, Integer> map = new HashMap<>();
        map.put("JOPA", 5);
        Object o = map.get("JOPA");
        int i = (int) o;
        LOGGER.info("i = " + i);
    }

    @Test
    public void parcelTest() {

        ScanResult sr = buildScanResult(MAC1, 70);

        ScanResult clone = cloneScanResult(sr);

        assertThat(clone.BSSID, is(equalTo(sr.BSSID)));
        assertThat(clone.level, is(equalTo(sr.level)));
        assertThat(clone, is(not(sameInstance(sr))));
    }

    @Test
    public void buildVsClonePerformanceTest() {
        ScanResult sr = null;

        long start = System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            sr = buildScanResult(MAC1, 70);
        }
        long elapsedNanos = System.nanoTime() - start;

        LOGGER.info("buildScanResult: " + elapsedNanos);

        start = System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            sr = cloneScanResult(sr);
        }
        elapsedNanos = System.nanoTime() - start;

        LOGGER.info("cloneScanResult: " + elapsedNanos);
    }

    private ScanResult cloneScanResult(ScanResult sr) {
        Parcel parcel = Parcel.obtain();
        parcel.writeValue(sr);
        parcel.setDataPosition(0); // required after unmarshalling
        ScanResult clone = (ScanResult)parcel.readValue(ScanResult.class.getClassLoader());
        parcel.recycle();
        return clone;
    }

    @Test
    public void oneApTest() {
        MovingAverageScanResultsQueue queue = new MovingAverageScanResultsQueue(3);

        List<ScanResult> scanResults1 = new ArrayList<>();
        scanResults1.add(buildScanResult(MAC1, 50));
        queue.add(scanResults1);
        Map.Entry<String, Integer> entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(50)));
        assertThat(queue.getItemsNum(), is(equalTo(1)));

        List<ScanResult> scanResults2 = new ArrayList<>();
        scanResults2.add(buildScanResult(MAC1, 70));
        queue.add(scanResults2);
        entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(50 + 70)));
        assertThat(queue.getItemsNum(), is(equalTo(2)));

        List<ScanResult> scanResults3 = new ArrayList<>();
        scanResults3.add(buildScanResult(MAC1, 30));
        queue.add(scanResults3);
        entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(50 + 70 + 30)));
        assertThat(queue.getItemsNum(), is(equalTo(3)));

        List<ScanResult> scanResults4 = new ArrayList<>();
        scanResults4.add(buildScanResult(MAC1, 90));
        queue.add(scanResults4);
        entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(70 + 30 + 90)));
        assertThat(queue.getItemsNum(), is(equalTo(3)));

        List<ScanResult> scanResults5 = new ArrayList<>();
        scanResults5.add(buildScanResult(MAC1, 60));
        queue.add(scanResults5);
        entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(30 + 90 + 60)));
        assertThat(queue.getItemsNum(), is(equalTo(3)));
    }

    @Test
    public void oneApNegativeTest() {
        MovingAverageScanResultsQueue queue = new MovingAverageScanResultsQueue(3);

        List<ScanResult> scanResults1 = new ArrayList<>();
        scanResults1.add(buildScanResult(MAC1, -50));
        queue.add(scanResults1);
        Map.Entry<String, Integer> entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(-50)));
        assertThat(queue.getItemsNum(), is(equalTo(1)));

        List<ScanResult> scanResults2 = new ArrayList<>();
        scanResults2.add(buildScanResult(MAC1, -70));
        queue.add(scanResults2);
        entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(-50 + -70)));
        assertThat(queue.getItemsNum(), is(equalTo(2)));

        List<ScanResult> scanResults3 = new ArrayList<>();
        scanResults3.add(buildScanResult(MAC1, -30));
        queue.add(scanResults3);
        entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(-50 + -70 + -30)));
        assertThat(queue.getItemsNum(), is(equalTo(3)));

        List<ScanResult> scanResults4 = new ArrayList<>();
        scanResults4.add(buildScanResult(MAC1, -90));
        queue.add(scanResults4);
        entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(-70 + -30 + -90)));
        assertThat(queue.getItemsNum(), is(equalTo(3)));

        List<ScanResult> scanResults5 = new ArrayList<>();
        scanResults5.add(buildScanResult(MAC1, -60));
        queue.add(scanResults5);
        entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);

        assertNotNull(queue.getSumFingerprint());
        assertThat(queue.getSumFingerprint().entrySet(), hasSize(1));
        assertThat(entry.getKey(), is(equalTo(MAC1)));
        assertThat(entry.getValue(), is(equalTo(-30 + -90 + -60)));
        assertThat(queue.getItemsNum(), is(equalTo(3)));
    }


    @Test
    public void twoApTest() {
        MovingAverageScanResultsQueue queue = new MovingAverageScanResultsQueue(3);

        List<ScanResult> scanResults1 = new ArrayList<>();
        scanResults1.add(buildScanResult(MAC1, -50));
        queue.add(scanResults1);

        List<ScanResult> scanResults2 = new ArrayList<>();
        scanResults2.add(buildScanResult(MAC1, -30));
        scanResults2.add(buildScanResult(MAC2, -70));
        queue.add(scanResults2);

        WiFiLocator.WiFiFingerprint sum = queue.getSumFingerprint();
        Map<String, Integer> counters = queue.getCounters();

        assertNotNull(sum);
        assertNotNull(counters);

        Map.Entry<String, Integer> entry = Iterables.get(queue.getSumFingerprint().entrySet(), 0);
        int sumMac1 = sum.get(MAC1);
        int counter1 = counters.get(MAC1);
        assertThat(sumMac1, is(equalTo(-50 + -30)));
        assertThat(counter1, is(equalTo(2)));

        int sumMac2 = sum.get(MAC2);
        int counter2 = counters.get(MAC2);
        assertThat(sumMac2, is(equalTo(-70)));
        assertThat(counter2, is(equalTo(1)));
    }

    // OMG, thanks Google. To mock ScanResult I need to use reflection.
    private ScanResult buildScanResult(String mac, int level) {
        Constructor<ScanResult> ctor = null;
        ScanResult sr = null;

        try {
            ctor = ScanResult.class.getDeclaredConstructor(null);
            ctor.setAccessible(true);
            sr = ctor.newInstance(null);

            sr.BSSID = mac;
            sr.level = level;

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return sr;
    }


}

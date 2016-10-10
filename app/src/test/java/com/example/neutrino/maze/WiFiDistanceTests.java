package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.WifiMark;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

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
 * Created by Greg Stein on 10/5/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class WiFiDistanceTests {
    public static final double FLOAT_ERROR = 0.00005f;

    @Test
    public void oneSsidBothExistingTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 75);
        b.put("44-85-00-11-DA-EC", 57);

        float distance = WiFiTug.distance(a, b);

        assertThat(distance, is(18f));
    }

    @Test
    public void oneSsidExistsInSingleFingerprintTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 75);

        float distanceA_B = WiFiTug.distance(a, b);
        float distanceB_A = WiFiTug.distance(b, a);

        assertThat(distanceA_B, is(75f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void twoSsidExistInSingleFingerprintTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 3);
        a.put("44-85-FF-11-DA-EC", 4);

        float distanceA_B = WiFiTug.distance(a, b);
        float distanceB_A = WiFiTug.distance(b, a);

        assertThat(distanceA_B, is(5f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void twoSsidExistInBothFingerprintTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 78);
        a.put("44-85-FF-11-DA-EC", 64);

        b.put("44-85-00-11-DA-EC", 75); // diff = 3
        b.put("44-85-FF-11-DA-EC", 68); // diff = 4

        float distanceA_B = WiFiTug.distance(a, b);
        float distanceB_A = WiFiTug.distance(b, a);

        assertThat(distanceA_B, is(5f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void wifiTugCommon2AP2refTest() {
        WiFiTug wiFiTug = new WiFiTug();
        wiFiTug.setClosestMarksPercentage(1.00f); // 100% - use all of the given reference points

        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 60);
        a.put("44-85-FF-11-DA-EC", 68);

        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        b.put("44-85-00-11-DA-EC", 78);
        b.put("44-85-FF-11-DA-EC", 64);

        // distance = 5 from b, 15 from a
        WiFiTug.Fingerprint current = new WiFiTug.Fingerprint();
        current.put("44-85-00-11-DA-EC", 75); // diff = 3
        current.put("44-85-FF-11-DA-EC", 68); // diff = 4

        wiFiTug.marks = new ArrayList<>();
        wiFiTug.marks.add(new WifiMark(-1, 0, a));
        wiFiTug.marks.add(new WifiMark(1, 2, b));
        wiFiTug.currentFingerprint = current;

        PointF position = new PointF();
        wiFiTug.getPosition(position);

        assertThat((double)position.x, is(closeTo(0.5d, FLOAT_ERROR)));
        assertThat((double)position.y, is(closeTo(1.5d, FLOAT_ERROR)));
    }

    @Test
    public void getSimilarMarksTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 5);
        a.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        b.put("44-85-00-11-DA-EC", 5);
        b.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.Fingerprint c = new WiFiTug.Fingerprint();
        c.put("44-85-00-11-DA-EC", 4);
        c.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.Fingerprint d = new WiFiTug.Fingerprint();
        d.put("44-85-00-11-DA-EC", 0);
        d.put("44-85-FF-11-DA-EC", 0);

        WiFiTug.Fingerprint e = new WiFiTug.Fingerprint();
        e.put("44-85-00-11-DA-EC", -3);
        e.put("44-85-FF-11-DA-EC", -3);

        WiFiTug.Fingerprint fingerprint = new WiFiTug.Fingerprint();
        fingerprint.put("44-85-00-11-DA-EC", -5); // diff = 3
        fingerprint.put("44-85-FF-11-DA-EC", -5); // diff = 4

        List<WifiMark> marks = new ArrayList<>();
        WifiMark eMark;
        // Real location of marks is not important, so place all of them in (0, 0)
        marks.add(new WifiMark(0, 0, a));
        marks.add(new WifiMark(0, 0, b));
        marks.add(new WifiMark(0, 0, c));
        marks.add(new WifiMark(0, 0, d));
        marks.add(eMark = new WifiMark(0, 0, e));

        // get 20% of total 5 = 1 WiFi mark
        List<WifiMark> similarMarks = WiFiTug.getSimilarMarks(marks, fingerprint, 0.2f);

        assertNotNull(similarMarks);
        assertThat(similarMarks, hasSize(1));
        assertThat(similarMarks.get(0), is(sameInstance(eMark)));
    }

    @Test
    public void eliminateOutliersTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 5);
        a.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        b.put("44-85-00-11-DA-EC", 5);
        b.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.Fingerprint c = new WiFiTug.Fingerprint();
        c.put("44-85-00-11-DA-EC", 4);
        c.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.Fingerprint d = new WiFiTug.Fingerprint();
        d.put("44-85-00-11-DA-EC", 0);
        d.put("44-85-FF-11-DA-EC", 0);

        WiFiTug.Fingerprint e = new WiFiTug.Fingerprint();
        e.put("44-85-00-11-DA-EC", -3);
        e.put("44-85-FF-11-DA-EC", -3);

        List<WifiMark> marks = new ArrayList<>();
        WifiMark aMark, bMark, cMark, dMark, eMark;

        marks.add(aMark = new WifiMark(1, 0, a));
        marks.add(bMark = new WifiMark(0, 1, b));
        marks.add(cMark = new WifiMark(-1, 0, c));
        marks.add(dMark = new WifiMark(0, -1, d));
        marks.add(eMark = new WifiMark(10, 10, e)); // outlier

        WiFiTug.eliminateOutliers(marks);

        assertThat(marks, hasSize(4));
        assertThat(marks, not(hasItem(eMark)));
        assertThat(marks, contains(aMark, bMark, cMark, dMark));
    }

    @Test
    public void eliminateOutliersNegativeTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 5);
        a.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        b.put("44-85-00-11-DA-EC", 5);
        b.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.Fingerprint c = new WiFiTug.Fingerprint();
        c.put("44-85-00-11-DA-EC", 4);
        c.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.Fingerprint d = new WiFiTug.Fingerprint();
        d.put("44-85-00-11-DA-EC", 0);
        d.put("44-85-FF-11-DA-EC", 0);

        WiFiTug.Fingerprint e = new WiFiTug.Fingerprint();
        e.put("44-85-00-11-DA-EC", -3);
        e.put("44-85-FF-11-DA-EC", -3);

        List<WifiMark> marks = new ArrayList<>();
        WifiMark eMark;

        marks.add(new WifiMark(1, 0, a));
        marks.add(new WifiMark(0, 1, b));
        marks.add(new WifiMark(-1, 0, c));
        marks.add(new WifiMark(0, -1, d));
        marks.add(eMark = new WifiMark(2, 2, e)); // NOT an outlier

        List<WifiMark> originalMarks = new ArrayList<>(marks);

        WiFiTug.eliminateOutliers(marks);

        assertThat(marks, hasSize(5));
        assertThat(marks, hasItem(eMark));
        assertEquals(originalMarks, marks);
    }
}

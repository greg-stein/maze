package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.WifiMark;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.hamcrest.core.CombinableMatcher.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
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
public class WiFiTugTests {
    public static final double FLOAT_ERROR = 0.00005f;

    @Test
    public void oneSsidBothExistingTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 75);
        b.put("44-85-00-11-DA-EC", 57);

        float distance = WiFiTug.difference(a, b);

        assertThat(distance, is(18f));
    }

    @Test
    public void oneSsidExistsInSingleFingerprintTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 75-100);

        float distanceA_B = WiFiTug.difference(a, b);
        float distanceB_A = WiFiTug.difference(b, a);

        assertThat(distanceA_B, is(75f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void twoSsidExistInSingleFingerprintTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 3-100);
        a.put("44-85-FF-11-DA-EC", 4-100);

        float distanceA_B = WiFiTug.difference(a, b);
        float distanceB_A = WiFiTug.difference(b, a);

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

        float distanceA_B = WiFiTug.difference(a, b);
        float distanceB_A = WiFiTug.difference(b, a);

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

        // difference = 5 from b, 15 from a
        WiFiTug.Fingerprint current = new WiFiTug.Fingerprint();
        current.put("44-85-00-11-DA-EC", 75); // diff = 3
        current.put("44-85-FF-11-DA-EC", 68); // diff = 4

        wiFiTug.marks = new ArrayList<>();
        wiFiTug.marks.add(new WifiMark(-1, 0, a));
        wiFiTug.marks.add(new WifiMark(1, 2, b));
        wiFiTug.currentFingerprint = current;
        wiFiTug.walls = new ArrayList<>();

        PointF position = new PointF();
        wiFiTug.getPosition(position);

        assertThat((double)position.x, is(closeTo(0.5d, FLOAT_ERROR)));
        assertThat((double)position.y, is(closeTo(1.5d, FLOAT_ERROR)));
    }

    @Test
    public void getSimilarMarksTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 1);
        a.put("44-85-FF-11-DA-EC", 0);

        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        b.put("44-85-00-11-DA-EC", 1);
        b.put("44-85-FF-11-DA-EC", 1);

        WiFiTug.Fingerprint c = new WiFiTug.Fingerprint();
        c.put("44-85-00-11-DA-EC", 1);
        c.put("44-85-FF-11-DA-EC", 2);

        WiFiTug.Fingerprint d = new WiFiTug.Fingerprint();
        d.put("44-85-00-11-DA-EC", 2);
        d.put("44-85-FF-11-DA-EC", 2);

        WiFiTug.Fingerprint e = new WiFiTug.Fingerprint();
        e.put("44-85-00-11-DA-EC", 2);
        e.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.Fingerprint f = new WiFiTug.Fingerprint();
        f.put("44-85-00-11-DA-EC", 2);
        f.put("44-85-FF-11-DA-EC", 4);

        WiFiTug.Fingerprint g = new WiFiTug.Fingerprint();
        g.put("44-85-00-11-DA-EC", 4);
        g.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.Fingerprint h = new WiFiTug.Fingerprint();
        h.put("44-85-00-11-DA-EC", 4);
        h.put("44-85-FF-11-DA-EC", 4);

        WiFiTug.Fingerprint i = new WiFiTug.Fingerprint();
        i.put("44-85-00-11-DA-EC", 4);
        i.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.Fingerprint j = new WiFiTug.Fingerprint();
        j.put("44-85-00-11-DA-EC", 5);
        j.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.Fingerprint fingerprint = new WiFiTug.Fingerprint();
        fingerprint.put("44-85-00-11-DA-EC", 0); // diff = 3
        fingerprint.put("44-85-FF-11-DA-EC", 0); // diff = 4

        List<WifiMark> marks = new ArrayList<>();
        WifiMark aMark, bMark, cMark, dMark, eMark, fMark, gMark, hMark, iMark, jMark;
        // Real location of marks is not important, so place all of them in (0, 0)
        marks.add(aMark = new WifiMark(0, 0, a));
        marks.add(bMark = new WifiMark(0, 0, b));
        marks.add(cMark = new WifiMark(0, 0, c));
        marks.add(dMark = new WifiMark(0, 0, d));
        marks.add(eMark = new WifiMark(0, 0, e));
        marks.add(fMark = new WifiMark(0, 0, f));
        marks.add(gMark = new WifiMark(0, 0, g));
        marks.add(hMark = new WifiMark(0, 0, h));
        marks.add(iMark = new WifiMark(0, 0, i));
        marks.add(jMark = new WifiMark(0, 0, j));

        // add another 20 marks farther than those ten
        for (int x = 0; x < 20; x++) {
            WiFiTug.Fingerprint fingerprint1 = new WiFiTug.Fingerprint();
            fingerprint1.put("44-85-00-11-DA-EC", (int) (Math.random()*50 + 20));
            fingerprint1.put("44-85-FF-11-DA-EC", (int) (Math.random()*50 + 20));
            marks.add(new WifiMark(0, 0, fingerprint1));
        }
        // get 20% of total 30, but minimum is 10
        List<WifiMark> similarMarks = WiFiTug.getSimilarMarks(marks, fingerprint, 0.2f);

        assertNotNull(similarMarks);
        assertThat(similarMarks, hasSize(10));
        assertThat(similarMarks, contains(aMark, bMark, cMark, dMark, eMark, fMark, gMark, hMark, iMark, jMark));
        assertThat(similarMarks.get(0), is(sameInstance(aMark)));
        assertThat(similarMarks.get(1), is(sameInstance(bMark)));
        assertThat(similarMarks.get(2), is(sameInstance(cMark)));
        assertThat(similarMarks.get(3), is(sameInstance(dMark)));
        assertThat(similarMarks.get(4), is(sameInstance(eMark)));
        assertThat(similarMarks.get(5), is(sameInstance(fMark)));
        assertThat(similarMarks.get(6), is(sameInstance(gMark)));
        assertThat(similarMarks.get(7), is(sameInstance(hMark)));
        assertThat(similarMarks.get(8), is(sameInstance(iMark)));
        assertThat(similarMarks.get(9), is(sameInstance(jMark)));
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

        PointF centroid = new PointF();
        WiFiTug.eliminateOutliers(marks, centroid);

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

        PointF centroid = new PointF();
        WiFiTug.eliminateOutliers(marks, centroid);

        assertThat(marks, hasSize(5));
        assertThat(marks, hasItem(eMark));
        assertEquals(originalMarks, marks);
    }

    @Test
    public void eliminateInvisiblesSimpleTest() {
        ArrayList<WifiMark> marks = new ArrayList<>();
        marks.add(new WifiMark(-1, 0, new WiFiTug.Fingerprint()));
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(0, 1, 0, -1));

        WiFiTug.eliminateInvisibles(new PointF(1, 0),marks ,walls);

        assertThat(marks, is(empty()));
    }

    @Test
    public void eliminateInvisiblesSimpleNegativeTest() {
        ArrayList<WifiMark> marks = new ArrayList<>();
        marks.add(new WifiMark(0.5f, 0, new WiFiTug.Fingerprint()));
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(0, 1, 0, -1));

        WiFiTug.eliminateInvisibles(new PointF(1, 0),marks ,walls);

        assertThat(marks, is(not(empty())));
        assertThat(marks, hasSize(1));
    }

    @Test
    public void eliminateInvisiblesTest() {
        ArrayList<WifiMark> marks = new ArrayList<>();
        marks.add(new WifiMark(-1, 0, new WiFiTug.Fingerprint()));
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(0, 1, 0, -1));
        walls.add(new Wall(0, -0.5f, 2, -0.5f));

        WiFiTug.eliminateInvisibles(new PointF(1, 0),marks ,walls);

        assertThat(marks, is(empty()));
    }

    @Test
    public void eliminateInvisiblesNegativeTest() {
        ArrayList<WifiMark> marks = new ArrayList<>();
        marks.add(new WifiMark(0.5f, 0, new WiFiTug.Fingerprint()));
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(0, 1, 0, -1));
        walls.add(new Wall(0, -0.5f, 2, -0.5f));

        WiFiTug.eliminateInvisibles(new PointF(1, 0),marks ,walls);

        assertThat(marks, is(not(empty())));
        assertThat(marks, hasSize(1));
    }

    @Test
    public void getMarksWithSameApsTest() {
        WiFiTug.Fingerprint a = new WiFiTug.Fingerprint();
        a.put("44-85-00-11-DA-EC", 0); // 00
        a.put("44-85-FF-11-DA-EC", 0); // FF

        WiFiTug.Fingerprint b = new WiFiTug.Fingerprint();
        b.put("44-85-00-11-DA-EC", 0); // 00
        b.put("44-85-FF-11-DA-EC", 0); // FF
        b.put("44-85-AA-11-DA-EC", 0); // AA

        WiFiTug.Fingerprint c = new WiFiTug.Fingerprint();
        c.put("44-85-00-11-DA-EC", 0); // 00
        c.put("44-85-FF-11-DA-EC", 0); // FF
        c.put("44-85-AA-11-DA-EC", 0); // AA

        WiFiTug.Fingerprint d = new WiFiTug.Fingerprint();
        d.put("44-85-00-11-DA-EC", 0); // 00
        d.put("44-85-BB-11-DA-EC", 0); // BB

        WiFiTug.Fingerprint e = new WiFiTug.Fingerprint();
        e.put("44-85-00-11-DA-EC", 0); // 00
        e.put("44-85-BB-11-DA-EC", 0); // BB
        e.put("44-85-FF-11-DA-EC", 0); // FF

        List<WifiMark> marks = new ArrayList<>();
        WifiMark aMark, bMark, cMark, dMark, eMark;

        marks.add(aMark = new WifiMark(0, 0, a));
        marks.add(bMark = new WifiMark(0, 0, b));
        marks.add(cMark = new WifiMark(0, 0, c));
        marks.add(dMark = new WifiMark(0, 0, d));
        marks.add(eMark = new WifiMark(0, 0, e));

        WiFiTug.Fingerprint fingerprint1 = new WiFiTug.Fingerprint();
        fingerprint1.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint1.put("44-85-FF-11-DA-EC", 0); // FF

        List<WifiMark> filteredMarks = WiFiTug.getMarksWithSameAps(marks, fingerprint1);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(4));
        assertThat(filteredMarks, not(contains(dMark)));
        assertThat(filteredMarks, contains(aMark, bMark, cMark, eMark));

// --------------------------------------------------------

        WiFiTug.Fingerprint fingerprint2 = new WiFiTug.Fingerprint();
        fingerprint2.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint2.put("44-85-BB-11-DA-EC", 0); // BB

        filteredMarks = WiFiTug.getMarksWithSameAps(marks, fingerprint2);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(2));
        assertThat(filteredMarks, not(contains(aMark, bMark, cMark)));
        assertThat(filteredMarks, contains(dMark, eMark));

// --------------------------------------------------------

        WiFiTug.Fingerprint fingerprint3 = new WiFiTug.Fingerprint();
        fingerprint3.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint3.put("44-85-BB-11-DA-EC", 0); // BB
        fingerprint3.put("44-85-FF-11-DA-EC", 0); // FF

        filteredMarks = WiFiTug.getMarksWithSameAps(marks, fingerprint3);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(1));
        assertThat(filteredMarks, not(contains(aMark, bMark, cMark, dMark)));
        assertThat(filteredMarks, contains(eMark));

// --------------------------------------------------------

        WiFiTug.Fingerprint fingerprint4 = new WiFiTug.Fingerprint();
        fingerprint4.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint4.put("44-85-BB-11-DA-EC", 0); // BB
        fingerprint4.put("44-85-FF-11-DA-EC", 0); // FF
        fingerprint4.put("44-85-AA-11-DA-EC", 0); // AA

        filteredMarks = WiFiTug.getMarksWithSameAps(marks, fingerprint4);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, either(
                both(not(contains(aMark, eMark, dMark))).and(contains(bMark, cMark)))
        .or(
                both(not(contains(aMark, bMark, cMark, dMark))).and(contains(eMark))
        ));
    }
}

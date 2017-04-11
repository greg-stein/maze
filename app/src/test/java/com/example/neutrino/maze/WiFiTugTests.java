package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.Wall;

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
        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 75);
        b.put("44-85-00-11-DA-EC", 57);

        float distance = WiFiTug.dissimilarity(a, b);

        assertThat(distance, is(18f));
    }

    @Test
    public void oneSsidExistsInSingleFingerprintTest() {
        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 75-100);

        float distanceA_B = WiFiTug.dissimilarity(a, b);
        float distanceB_A = WiFiTug.dissimilarity(b, a);

        assertThat(distanceA_B, is(75f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void twoSsidExistInSingleFingerprintTest() {
        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 3-100);
        a.put("44-85-FF-11-DA-EC", 4-100);

        float distanceA_B = WiFiTug.dissimilarity(a, b);
        float distanceB_A = WiFiTug.dissimilarity(b, a);

        assertThat(distanceA_B, is(5f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void twoSsidExistInBothFingerprintTest() {
        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 78);
        a.put("44-85-FF-11-DA-EC", 64);

        b.put("44-85-00-11-DA-EC", 75); // diff = 3
        b.put("44-85-FF-11-DA-EC", 68); // diff = 4

        float distanceA_B = WiFiTug.dissimilarity(a, b);
        float distanceB_A = WiFiTug.dissimilarity(b, a);

        assertThat(distanceA_B, is(5f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void wifiTugCommon2AP2refTest() {
        WiFiTug wiFiTug = WiFiTug.getInstance();
        wiFiTug.setClosestMarksPercentage(1.00f); // 100% - use all of the given reference points

        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 60);
        a.put("44-85-FF-11-DA-EC", 68);

        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        b.put("44-85-00-11-DA-EC", 78);
        b.put("44-85-FF-11-DA-EC", 64);

        // dissimilarity = 5 from b, 15 from a
        WiFiTug.WiFiFingerprint current = new WiFiTug.WiFiFingerprint();
        current.put("44-85-00-11-DA-EC", 75); // diff = 3
        current.put("44-85-FF-11-DA-EC", 68); // diff = 4

        wiFiTug.marks = new ArrayList<>();
        wiFiTug.marks.add(new Fingerprint(-1, 0, a));
        wiFiTug.marks.add(new Fingerprint(1, 2, b));
        wiFiTug.setCurrentFingerprint(current);
        wiFiTug.walls = new ArrayList<>();

        PointF position = new PointF();
        wiFiTug.getPosition(position);

        assertThat((double)position.x, is(closeTo(0.5d, FLOAT_ERROR)));
        assertThat((double)position.y, is(closeTo(1.5d, FLOAT_ERROR)));
    }

    @Test
    public void getSimilarMarksTest() {
        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 1);
        a.put("44-85-FF-11-DA-EC", 0);

        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        b.put("44-85-00-11-DA-EC", 1);
        b.put("44-85-FF-11-DA-EC", 1);

        WiFiTug.WiFiFingerprint c = new WiFiTug.WiFiFingerprint();
        c.put("44-85-00-11-DA-EC", 1);
        c.put("44-85-FF-11-DA-EC", 2);

        WiFiTug.WiFiFingerprint d = new WiFiTug.WiFiFingerprint();
        d.put("44-85-00-11-DA-EC", 2);
        d.put("44-85-FF-11-DA-EC", 2);

        WiFiTug.WiFiFingerprint e = new WiFiTug.WiFiFingerprint();
        e.put("44-85-00-11-DA-EC", 2);
        e.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.WiFiFingerprint f = new WiFiTug.WiFiFingerprint();
        f.put("44-85-00-11-DA-EC", 2);
        f.put("44-85-FF-11-DA-EC", 4);

        WiFiTug.WiFiFingerprint g = new WiFiTug.WiFiFingerprint();
        g.put("44-85-00-11-DA-EC", 4);
        g.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.WiFiFingerprint h = new WiFiTug.WiFiFingerprint();
        h.put("44-85-00-11-DA-EC", 4);
        h.put("44-85-FF-11-DA-EC", 4);

        WiFiTug.WiFiFingerprint i = new WiFiTug.WiFiFingerprint();
        i.put("44-85-00-11-DA-EC", 4);
        i.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.WiFiFingerprint j = new WiFiTug.WiFiFingerprint();
        j.put("44-85-00-11-DA-EC", 5);
        j.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.WiFiFingerprint fingerprint = new WiFiTug.WiFiFingerprint();
        fingerprint.put("44-85-00-11-DA-EC", 0); // diff = 3
        fingerprint.put("44-85-FF-11-DA-EC", 0); // diff = 4

        List<Fingerprint> marks = new ArrayList<>();
        Fingerprint aMark, bMark, cMark, dMark, eMark, fMark, gMark, hMark, iMark, jMark;
        // Real location of marks is not important, so place all of them in (0, 0)
        marks.add(aMark = new Fingerprint(0, 0, a));
        marks.add(bMark = new Fingerprint(0, 0, b));
        marks.add(cMark = new Fingerprint(0, 0, c));
        marks.add(dMark = new Fingerprint(0, 0, d));
        marks.add(eMark = new Fingerprint(0, 0, e));
        marks.add(fMark = new Fingerprint(0, 0, f));
        marks.add(gMark = new Fingerprint(0, 0, g));
        marks.add(hMark = new Fingerprint(0, 0, h));
        marks.add(iMark = new Fingerprint(0, 0, i));
        marks.add(jMark = new Fingerprint(0, 0, j));

        // add another 20 marks farther than those ten
        for (int x = 0; x < 20; x++) {
            WiFiTug.WiFiFingerprint fingerprint1 = new WiFiTug.WiFiFingerprint();
            fingerprint1.put("44-85-00-11-DA-EC", (int) (Math.random()*50 + 20));
            fingerprint1.put("44-85-FF-11-DA-EC", (int) (Math.random()*50 + 20));
            marks.add(new Fingerprint(0, 0, fingerprint1));
        }
        // get 20% of total 30, but minimum is 10
        List<Fingerprint> similarMarks = WiFiTug.getSimilarMarks(marks, fingerprint, 0.2f);

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
        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 5);
        a.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        b.put("44-85-00-11-DA-EC", 5);
        b.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.WiFiFingerprint c = new WiFiTug.WiFiFingerprint();
        c.put("44-85-00-11-DA-EC", 4);
        c.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.WiFiFingerprint d = new WiFiTug.WiFiFingerprint();
        d.put("44-85-00-11-DA-EC", 0);
        d.put("44-85-FF-11-DA-EC", 0);

        WiFiTug.WiFiFingerprint e = new WiFiTug.WiFiFingerprint();
        e.put("44-85-00-11-DA-EC", -3);
        e.put("44-85-FF-11-DA-EC", -3);

        List<Fingerprint> marks = new ArrayList<>();
        Fingerprint aMark, bMark, cMark, dMark, eMark;

        marks.add(aMark = new Fingerprint(1, 0, a));
        marks.add(bMark = new Fingerprint(0, 1, b));
        marks.add(cMark = new Fingerprint(-1, 0, c));
        marks.add(dMark = new Fingerprint(0, -1, d));
        marks.add(eMark = new Fingerprint(10, 10, e)); // outlier

        PointF centroid = new PointF();
        WiFiTug.eliminateOutliers(marks, centroid);

        assertThat(marks, hasSize(4));
        assertThat(marks, not(hasItem(eMark)));
        assertThat(marks, contains(aMark, bMark, cMark, dMark));
    }

    @Test
    public void eliminateOutliersNegativeTest() {
        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 5);
        a.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        b.put("44-85-00-11-DA-EC", 5);
        b.put("44-85-FF-11-DA-EC", 3);

        WiFiTug.WiFiFingerprint c = new WiFiTug.WiFiFingerprint();
        c.put("44-85-00-11-DA-EC", 4);
        c.put("44-85-FF-11-DA-EC", 5);

        WiFiTug.WiFiFingerprint d = new WiFiTug.WiFiFingerprint();
        d.put("44-85-00-11-DA-EC", 0);
        d.put("44-85-FF-11-DA-EC", 0);

        WiFiTug.WiFiFingerprint e = new WiFiTug.WiFiFingerprint();
        e.put("44-85-00-11-DA-EC", -3);
        e.put("44-85-FF-11-DA-EC", -3);

        List<Fingerprint> marks = new ArrayList<>();
        Fingerprint eMark;

        marks.add(new Fingerprint(1, 0, a));
        marks.add(new Fingerprint(0, 1, b));
        marks.add(new Fingerprint(-1, 0, c));
        marks.add(new Fingerprint(0, -1, d));
        marks.add(eMark = new Fingerprint(2, 2, e)); // NOT an outlier

        List<Fingerprint> originalMarks = new ArrayList<>(marks);

        PointF centroid = new PointF();
        WiFiTug.eliminateOutliers(marks, centroid);

        assertThat(marks, hasSize(5));
        assertThat(marks, hasItem(eMark));
        assertEquals(originalMarks, marks);
    }

    @Test
    public void eliminateInvisiblesSimpleTest() {
        ArrayList<Fingerprint> marks = new ArrayList<>();
        marks.add(new Fingerprint(-1, 0, new WiFiTug.WiFiFingerprint()));
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(0, 1, 0, -1));

        WiFiTug.eliminateInvisibles(new PointF(1, 0),marks ,walls);

        assertThat(marks, is(empty()));
    }

    @Test
    public void eliminateInvisiblesSimpleNegativeTest() {
        ArrayList<Fingerprint> marks = new ArrayList<>();
        marks.add(new Fingerprint(0.5f, 0, new WiFiTug.WiFiFingerprint()));
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(0, 1, 0, -1));

        WiFiTug.eliminateInvisibles(new PointF(1, 0),marks ,walls);

        assertThat(marks, is(not(empty())));
        assertThat(marks, hasSize(1));
    }

    @Test
    public void eliminateInvisiblesTest() {
        ArrayList<Fingerprint> marks = new ArrayList<>();
        marks.add(new Fingerprint(-1, 0, new WiFiTug.WiFiFingerprint()));
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(0, 1, 0, -1));
        walls.add(new Wall(0, -0.5f, 2, -0.5f));

        WiFiTug.eliminateInvisibles(new PointF(1, 0),marks ,walls);

        assertThat(marks, is(empty()));
    }

    @Test
    public void eliminateInvisiblesNegativeTest() {
        ArrayList<Fingerprint> marks = new ArrayList<>();
        marks.add(new Fingerprint(0.5f, 0, new WiFiTug.WiFiFingerprint()));
        ArrayList<Wall> walls = new ArrayList<>();
        walls.add(new Wall(0, 1, 0, -1));
        walls.add(new Wall(0, -0.5f, 2, -0.5f));

        WiFiTug.eliminateInvisibles(new PointF(1, 0),marks ,walls);

        assertThat(marks, is(not(empty())));
        assertThat(marks, hasSize(1));
    }

    @Test
    public void getMarksWithSameApsTest() {
        WiFiTug.WiFiFingerprint a = new WiFiTug.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 0); // 00
        a.put("44-85-FF-11-DA-EC", 0); // FF

        WiFiTug.WiFiFingerprint b = new WiFiTug.WiFiFingerprint();
        b.put("44-85-00-11-DA-EC", 0); // 00
        b.put("44-85-FF-11-DA-EC", 0); // FF
        b.put("44-85-AA-11-DA-EC", 0); // AA

        WiFiTug.WiFiFingerprint c = new WiFiTug.WiFiFingerprint();
        c.put("44-85-00-11-DA-EC", 0); // 00
        c.put("44-85-FF-11-DA-EC", 0); // FF
        c.put("44-85-AA-11-DA-EC", 0); // AA

        WiFiTug.WiFiFingerprint d = new WiFiTug.WiFiFingerprint();
        d.put("44-85-00-11-DA-EC", 0); // 00
        d.put("44-85-BB-11-DA-EC", 0); // BB

        WiFiTug.WiFiFingerprint e = new WiFiTug.WiFiFingerprint();
        e.put("44-85-00-11-DA-EC", 0); // 00
        e.put("44-85-BB-11-DA-EC", 0); // BB
        e.put("44-85-FF-11-DA-EC", 0); // FF

        List<Fingerprint> marks = new ArrayList<>();
        Fingerprint aMark, bMark, cMark, dMark, eMark;

        marks.add(aMark = new Fingerprint(0, 0, a));
        marks.add(bMark = new Fingerprint(0, 0, b));
        marks.add(cMark = new Fingerprint(0, 0, c));
        marks.add(dMark = new Fingerprint(0, 0, d));
        marks.add(eMark = new Fingerprint(0, 0, e));

        WiFiTug.WiFiFingerprint fingerprint1 = new WiFiTug.WiFiFingerprint();
        fingerprint1.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint1.put("44-85-FF-11-DA-EC", 0); // FF

        List<Fingerprint> filteredMarks = WiFiTug.getMarksWithSameAps(marks, fingerprint1);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(4));
        assertThat(filteredMarks, not(contains(dMark)));
        assertThat(filteredMarks, contains(aMark, bMark, cMark, eMark));

// --------------------------------------------------------

        WiFiTug.WiFiFingerprint fingerprint2 = new WiFiTug.WiFiFingerprint();
        fingerprint2.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint2.put("44-85-BB-11-DA-EC", 0); // BB

        filteredMarks = WiFiTug.getMarksWithSameAps(marks, fingerprint2);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(2));
        assertThat(filteredMarks, not(contains(aMark, bMark, cMark)));
        assertThat(filteredMarks, contains(dMark, eMark));

// --------------------------------------------------------

        WiFiTug.WiFiFingerprint fingerprint3 = new WiFiTug.WiFiFingerprint();
        fingerprint3.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint3.put("44-85-BB-11-DA-EC", 0); // BB
        fingerprint3.put("44-85-FF-11-DA-EC", 0); // FF

        filteredMarks = WiFiTug.getMarksWithSameAps(marks, fingerprint3);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(1));
        assertThat(filteredMarks, not(contains(aMark, bMark, cMark, dMark)));
        assertThat(filteredMarks, contains(eMark));

// --------------------------------------------------------

        WiFiTug.WiFiFingerprint fingerprint4 = new WiFiTug.WiFiFingerprint();
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

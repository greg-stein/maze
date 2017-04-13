package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.floorplan.Fingerprint;

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
public class WiFiLocatorTests {
    public static final double FLOAT_ERROR = 0.00005f;

    @Test
    public void oneSsidBothExistingTest() {
        WiFiLocator.WiFiFingerprint a = new WiFiLocator.WiFiFingerprint();
        WiFiLocator.WiFiFingerprint b = new WiFiLocator.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 75);
        b.put("44-85-00-11-DA-EC", 57);

        float distance = WiFiLocator.dissimilarity(a, b);

        assertThat(distance, is(18f));
    }

    @Test
    public void oneSsidExistsInSingleFingerprintTest() {
        WiFiLocator.WiFiFingerprint a = new WiFiLocator.WiFiFingerprint();
        WiFiLocator.WiFiFingerprint b = new WiFiLocator.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 75-100);

        float distanceA_B = WiFiLocator.dissimilarity(a, b);
        float distanceB_A = WiFiLocator.dissimilarity(b, a);

        assertThat(distanceA_B, is(75f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void twoSsidExistInSingleFingerprintTest() {
        WiFiLocator.WiFiFingerprint a = new WiFiLocator.WiFiFingerprint();
        WiFiLocator.WiFiFingerprint b = new WiFiLocator.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 3-100);
        a.put("44-85-FF-11-DA-EC", 4-100);

        float distanceA_B = WiFiLocator.dissimilarity(a, b);
        float distanceB_A = WiFiLocator.dissimilarity(b, a);

        assertThat(distanceA_B, is(5f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void twoSsidExistInBothFingerprintTest() {
        WiFiLocator.WiFiFingerprint a = new WiFiLocator.WiFiFingerprint();
        WiFiLocator.WiFiFingerprint b = new WiFiLocator.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 78);
        a.put("44-85-FF-11-DA-EC", 64);

        b.put("44-85-00-11-DA-EC", 75); // diff = 3
        b.put("44-85-FF-11-DA-EC", 68); // diff = 4

        float distanceA_B = WiFiLocator.dissimilarity(a, b);
        float distanceB_A = WiFiLocator.dissimilarity(b, a);

        assertThat(distanceA_B, is(5f));
        assertThat(distanceB_A, is(equalTo(distanceA_B)));
    }

    @Test
    public void wifiTugCommon2AP2refTest() {
        WiFiLocator wifiLocator = WiFiLocator.getInstance();

        WiFiLocator.WiFiFingerprint a = new WiFiLocator.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 60);
        a.put("44-85-FF-11-DA-EC", 68);

        WiFiLocator.WiFiFingerprint b = new WiFiLocator.WiFiFingerprint();
        b.put("44-85-00-11-DA-EC", 78);
        b.put("44-85-FF-11-DA-EC", 64);

        // dissimilarity = 5 from b, 15 from a
        WiFiLocator.WiFiFingerprint current = new WiFiLocator.WiFiFingerprint();
        current.put("44-85-00-11-DA-EC", 75); // diff = 3
        current.put("44-85-FF-11-DA-EC", 68); // diff = 4

        List<Fingerprint> fingerprints = new ArrayList<>();
        fingerprints.add(new Fingerprint(-1, 0, a));
        fingerprints.add(new Fingerprint(1, 2, b));
        wifiLocator.setFingerprintsMap(fingerprints);
        wifiLocator.setCurrentFingerprint(current);

        PointF position = new PointF();
        wifiLocator.getPosition(position);

        assertThat((double)position.x, is(closeTo(0.5d, FLOAT_ERROR)));
        assertThat((double)position.y, is(closeTo(1.5d, FLOAT_ERROR)));
    }

    @Test
    public void getSimilarMarksTest() {
        WiFiLocator.WiFiFingerprint a = new WiFiLocator.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 1);
        a.put("44-85-FF-11-DA-EC", 0);

        WiFiLocator.WiFiFingerprint b = new WiFiLocator.WiFiFingerprint();
        b.put("44-85-00-11-DA-EC", 1);
        b.put("44-85-FF-11-DA-EC", 1);

        WiFiLocator.WiFiFingerprint c = new WiFiLocator.WiFiFingerprint();
        c.put("44-85-00-11-DA-EC", 1);
        c.put("44-85-FF-11-DA-EC", 2);

        WiFiLocator.WiFiFingerprint d = new WiFiLocator.WiFiFingerprint();
        d.put("44-85-00-11-DA-EC", 2);
        d.put("44-85-FF-11-DA-EC", 2);

        WiFiLocator.WiFiFingerprint e = new WiFiLocator.WiFiFingerprint();
        e.put("44-85-00-11-DA-EC", 2);
        e.put("44-85-FF-11-DA-EC", 3);

        WiFiLocator.WiFiFingerprint f = new WiFiLocator.WiFiFingerprint();
        f.put("44-85-00-11-DA-EC", 2);
        f.put("44-85-FF-11-DA-EC", 4);

        WiFiLocator.WiFiFingerprint g = new WiFiLocator.WiFiFingerprint();
        g.put("44-85-00-11-DA-EC", 4);
        g.put("44-85-FF-11-DA-EC", 3);

        WiFiLocator.WiFiFingerprint h = new WiFiLocator.WiFiFingerprint();
        h.put("44-85-00-11-DA-EC", 4);
        h.put("44-85-FF-11-DA-EC", 4);

        WiFiLocator.WiFiFingerprint i = new WiFiLocator.WiFiFingerprint();
        i.put("44-85-00-11-DA-EC", 4);
        i.put("44-85-FF-11-DA-EC", 5);

        WiFiLocator.WiFiFingerprint j = new WiFiLocator.WiFiFingerprint();
        j.put("44-85-00-11-DA-EC", 5);
        j.put("44-85-FF-11-DA-EC", 5);

        WiFiLocator.WiFiFingerprint fingerprint = new WiFiLocator.WiFiFingerprint();
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
            WiFiLocator.WiFiFingerprint fingerprint1 = new WiFiLocator.WiFiFingerprint();
            fingerprint1.put("44-85-00-11-DA-EC", (int) (Math.random()*50 + 20));
            fingerprint1.put("44-85-FF-11-DA-EC", (int) (Math.random()*50 + 20));
            marks.add(new Fingerprint(0, 0, fingerprint1));
        }
        // get 20% of total 30, but minimum is 10
        List<Fingerprint> similarMarks = WiFiLocator.getSimilarMarks(marks, fingerprint, 0.2f);

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
    public void getMarksWithSameApsTest() {
        WiFiLocator.WiFiFingerprint a = new WiFiLocator.WiFiFingerprint();
        a.put("44-85-00-11-DA-EC", 0); // 00
        a.put("44-85-FF-11-DA-EC", 0); // FF

        WiFiLocator.WiFiFingerprint b = new WiFiLocator.WiFiFingerprint();
        b.put("44-85-00-11-DA-EC", 0); // 00
        b.put("44-85-FF-11-DA-EC", 0); // FF
        b.put("44-85-AA-11-DA-EC", 0); // AA

        WiFiLocator.WiFiFingerprint c = new WiFiLocator.WiFiFingerprint();
        c.put("44-85-00-11-DA-EC", 0); // 00
        c.put("44-85-FF-11-DA-EC", 0); // FF
        c.put("44-85-AA-11-DA-EC", 0); // AA

        WiFiLocator.WiFiFingerprint d = new WiFiLocator.WiFiFingerprint();
        d.put("44-85-00-11-DA-EC", 0); // 00
        d.put("44-85-BB-11-DA-EC", 0); // BB

        WiFiLocator.WiFiFingerprint e = new WiFiLocator.WiFiFingerprint();
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

        WiFiLocator.WiFiFingerprint fingerprint1 = new WiFiLocator.WiFiFingerprint();
        fingerprint1.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint1.put("44-85-FF-11-DA-EC", 0); // FF

        List<Fingerprint> filteredMarks = WiFiLocator.getMarksWithSameAps(marks, fingerprint1);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(4));
        assertThat(filteredMarks, not(contains(dMark)));
        assertThat(filteredMarks, contains(aMark, bMark, cMark, eMark));

// --------------------------------------------------------

        WiFiLocator.WiFiFingerprint fingerprint2 = new WiFiLocator.WiFiFingerprint();
        fingerprint2.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint2.put("44-85-BB-11-DA-EC", 0); // BB

        filteredMarks = WiFiLocator.getMarksWithSameAps(marks, fingerprint2);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(2));
        assertThat(filteredMarks, not(contains(aMark, bMark, cMark)));
        assertThat(filteredMarks, contains(dMark, eMark));

// --------------------------------------------------------

        WiFiLocator.WiFiFingerprint fingerprint3 = new WiFiLocator.WiFiFingerprint();
        fingerprint3.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint3.put("44-85-BB-11-DA-EC", 0); // BB
        fingerprint3.put("44-85-FF-11-DA-EC", 0); // FF

        filteredMarks = WiFiLocator.getMarksWithSameAps(marks, fingerprint3);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, hasSize(1));
        assertThat(filteredMarks, not(contains(aMark, bMark, cMark, dMark)));
        assertThat(filteredMarks, contains(eMark));

// --------------------------------------------------------

        WiFiLocator.WiFiFingerprint fingerprint4 = new WiFiLocator.WiFiFingerprint();
        fingerprint4.put("44-85-00-11-DA-EC", 0); // 00
        fingerprint4.put("44-85-BB-11-DA-EC", 0); // BB
        fingerprint4.put("44-85-FF-11-DA-EC", 0); // FF
        fingerprint4.put("44-85-AA-11-DA-EC", 0); // AA

        filteredMarks = WiFiLocator.getMarksWithSameAps(marks, fingerprint4);

        assertNotNull(filteredMarks);
        assertThat(filteredMarks, either(
                both(not(contains(aMark, eMark, dMark))).and(contains(bMark, cMark)))
        .or(
                both(not(contains(aMark, bMark, cMark, dMark))).and(contains(eMark))
        ));
    }
}

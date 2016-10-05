package com.example.neutrino.maze;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 10/5/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class WiFiSampleDistanceTests {
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
}

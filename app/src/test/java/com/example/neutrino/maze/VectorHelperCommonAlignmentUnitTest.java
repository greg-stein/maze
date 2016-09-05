package com.example.neutrino.maze;

import android.graphics.PointF;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
        */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class VectorHelperCommonAlignmentUnitTest {

    private static final Logger LOGGER = Logger.getLogger(VectorHelperCommonAlignmentUnitTest.class.getName());

    @Before
    public void setup() {
        LOGGER.setLevel(Level.FINE);
    }

    @Test
    public void alignVectors45DegreesTest() throws Exception {
        final float V2_X = 3.0f;
        final float V2_Y = 3.0f;

        // Reference vector
        PointF u1 = new PointF(0, 0);
        PointF u2 = new PointF(3, 0);
        LOGGER.info("u2: x=" + u2.x + " y=" + u2.y);

        // Alignee
        PointF v1 = new PointF(0, 0);
        PointF v2 = new PointF(V2_X, V2_Y);

        float threshold = 0.1f;
        VectorHelper.alignVector(u1, u2, v1, v2, threshold);

        // Test that in this case end vertex is not changed:
        assertThat(v2.x, is(V2_X));
        assertThat(v2.y, is(V2_Y));
    }

    @Test
    public void alignVectorSharpAngleTest() {
        // Reference vector
        PointF u1 = new PointF(0, 0);
        PointF u2 = new PointF(10, 0);

        // Alignee
        PointF v1 = new PointF(0, 0);
        PointF v2 = new PointF(10, 1);

        float threshold = 0.1f;
        VectorHelper.alignVector(u1, u2, v1, v2, threshold);

        // Test that end vertex has been changed:
        assertThat(v2.y, is(v1.y));
    }

    @Test
    public void alignVectorSharpAngleTest2() {
        // Reference vector
        PointF u1 = new PointF(0, 0);
        PointF u2 = new PointF(10, 0);

        // Alignee
        PointF v1 = new PointF(0, 0);
        PointF v2 = new PointF(1, 10);

        float threshold = 0.1f;
        VectorHelper.alignVector(u1, u2, v1, v2, threshold);

        // Test that end vertex has been changed:
        assertThat(v2.x, is(v1.x));
    }
}
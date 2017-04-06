package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.rendering.VectorHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
        */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class VectorHelperCommonUnitTest {

    private static final Logger LOGGER = Logger.getLogger(VectorHelperCommonUnitTest.class.getName());

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

    @Test
    public void linesIntersectPositiveTest() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, 4);
        PointF B = new PointF(3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(3, 4);

        assertTrue(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectMNotObscuredTest() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, 4);
        PointF B = new PointF(3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(-1, 4);

        assertFalse(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectMBelowLineTest() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, 4);
        PointF B = new PointF(3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(1, 2);

        assertFalse(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectMInOppositeQuarterTest() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, 4);
        PointF B = new PointF(3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(-1, -1);

        assertFalse(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectLineInOppositeQuarterTest() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, -4);
        PointF B = new PointF(-3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(3, 4);

        assertFalse(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectLineInOppositeQuarterTest2() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, -4);
        PointF B = new PointF(-3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(-1, -2);

        assertFalse(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectCollinearLinesMAboveLine() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(3, 3);
        PointF B = new PointF(5, 5);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(7, 7);

        assertTrue(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectCollinearLinesMBelowLine() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(3, 3);
        PointF B = new PointF(5, 5);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(1, 1);

        assertFalse(VectorHelper.linesIntersect(A, B, O, M));
    }

}
package com.example.neutrino.maze;

import android.graphics.PointF;
import android.graphics.RectF;

import com.example.neutrino.maze.rendering.VectorHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
        */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE)
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
    public void linesIntersectPositiveTestQuarter2() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, 4);
        PointF B = new PointF(-3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(-3, 4);

        assertTrue(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectPositiveTestQuarter2LongLine() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(-300, -396);
        PointF B = new PointF(297, 404);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(-3, 4);

        assertTrue(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectMNotObscuredTestQuarter2() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, 4);
        PointF B = new PointF(-3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(1, 4);

        assertFalse(VectorHelper.linesIntersect(A, B, O, M));
    }

    @Test
    public void linesIntersectMBelowLineTestQuarter2() {
        // Line1: (O, 4) -- (3, 0)
        PointF A = new PointF(0, 4);
        PointF B = new PointF(-3, 0);

        // Line2: (0, 0) -- (3, 4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(-1, 2);

        assertFalse(VectorHelper.linesIntersect(A, B, O, M));
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
    public void linesIntersectLineInOppositeQuarterPositiveTest() {
        // Line1: (O, -4) -- (-3, 0)
        PointF A = new PointF(0, -4);
        PointF B = new PointF(-3, 0);

        // Line2: (0, 0) -- (-3, -4)
        PointF O = new PointF(0, 0);
        PointF M = new PointF(-3, -4);

        assertTrue(VectorHelper.linesIntersect(A, B, O, M));
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

//    @Ignore
    @Test
    public void almostParameterizedTestBLAD() {
        Object[][] parameters = new Object[][] {
                {127.58f, 57.76f, 147.29f, 62.69f, 137.10f, 59.92f, 137.11f, 60.60f, true}, // <-- intersection
                {127.58f, 57.76f, 147.29f, 62.69f, 137.11f, 60.60f, 137.11f, 61.28f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 137.11f, 61.28f, 137.10f, 61.96f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 137.10f, 61.96f, 137.08f, 62.64f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 137.08f, 62.64f, 137.07f, 63.32f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 137.07f, 63.32f, 137.03f, 64.00f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 137.03f, 64.00f, 137.04f, 63.32f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 137.04f, 63.32f, 136.99f, 62.64f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 136.99f, 62.64f, 136.88f, 61.97f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 136.88f, 61.97f, 136.76f, 61.30f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 136.76f, 61.30f, 136.66f, 60.63f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 136.66f, 60.63f, 136.54f, 59.96f, true}, // <-- intersection
                {127.58f, 57.76f, 147.29f, 62.69f, 136.54f, 59.96f, 136.42f, 59.29f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 136.42f, 59.29f, 136.28f, 58.62f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 136.28f, 58.62f, 136.13f, 57.96f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 136.13f, 57.96f, 135.97f, 57.30f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 135.97f, 57.30f, 135.81f, 56.64f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 135.81f, 56.64f, 135.62f, 55.99f, false},
                {127.58f, 57.76f, 147.29f, 62.69f, 135.62f, 55.99f, 135.62f, 55.31f, false}
        };

        PointF A = new PointF();
        PointF B = new PointF();
        PointF O = new PointF();
        PointF M = new PointF();

        for (Object[] params : parameters) {
            A.set((Float)params[0], (Float)params[1]);
            B.set((Float)params[2], (Float)params[3]);
            O.set((Float)params[4], (Float)params[5]);
            M.set((Float)params[6], (Float)params[7]);
            Boolean result = (Boolean)params[8];

            assertThat(VectorHelper.linesIntersect(A, B, O, M), is(equalTo(result)));
        }
    }

    @Test
    public void projectionCommonTest() {
        PointF a = new PointF(0f, 0f);
        PointF b = new PointF(10f, 0f);
        PointF c = new PointF(0f, 0f);
        PointF d = new PointF(5f, 7f);

        PointF proj = VectorHelper.projection(c, d, a, b);

        assertThat(proj.x, is(equalTo(5f)));
        assertThat(proj.y, is(equalTo(0f)));
    }

    @Test
    public void intersectRectLineGoesThroughDiagonalTest() {
        PointF a = new PointF(0, 0);
        PointF b = new PointF(10, 10);
        RectF rect = new RectF(3, 3, 7, 7);
        rect.sort();

        assertTrue(VectorHelper.lineIntersect(a, b, rect));
    }

    @Test
    public void intersectRectLineGoesThroughOppositeSidesTest() {
        PointF a = new PointF(0, 0);
        PointF b = new PointF(10, 10);
        RectF rect = new RectF(3, 1, 5, 15);
        rect.sort();

        assertTrue(VectorHelper.lineIntersect(a, b, rect));
    }

    @Test
    public void intersectRectLineGoesThroughCornerTest() {
        PointF a = new PointF(0, 0);
        PointF b = new PointF(10, 10);
        RectF rect = new RectF(2, 3, 7, 1);
        rect.sort();

        assertTrue(VectorHelper.lineIntersect(a, b, rect));
    }

    @Test
    public void intersectRectLineTouchesCornerTest() {
        PointF a = new PointF(0, 0);
        PointF b = new PointF(10, 10);
        RectF rect = new RectF(5, 5, 10, 1);
        rect.sort();

        assertTrue(VectorHelper.lineIntersect(a, b, rect));
    }

    @Test
    public void intersectRectLineDoesntIntersectTest() {
        PointF a = new PointF(0, 0);
        PointF b = new PointF(10, 10);
        RectF rect = new RectF(5, 2, 17, 1);
        rect.sort();

        assertFalse(VectorHelper.lineIntersect(a, b, rect));
    }
}
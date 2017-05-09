package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.rendering.VectorHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 9/4/2016.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class VectorHelperParameterizedAlignmentUnitTest {
    private static final float THRESHOLD = 0.1f;

    private static final Logger logger = Logger.getLogger(VectorHelperParameterizedAlignmentUnitTest.class.getName());

    public enum AlignmentType {ALIGNMENT_TYPE_VERTICAL, ALIGNMENT_TYPE_HORIZONTAL};
    private static AlignmentType mAlignmentType;

    // Reference vector
    private static final PointF u1 = new PointF(1, 0);
    private static final PointF u2 = new PointF(1, 0);

    // Alignee
    private static final PointF v1 = new PointF(1, 0);
    private static final PointF v2 = new PointF(1, 0);

    public VectorHelperParameterizedAlignmentUnitTest(PointF u1, PointF u2, PointF v1, PointF v2, AlignmentType alignmentType) {
        logger.info("U: (" + u1.x + ", " + u1.y + ") --> (" + u2.x + ", " + u2.y + ")");
        logger.info("V: (" + v1.x + ", " + v1.y + ") --> (" + v2.x + ", " + v2.y + ")");
        this.u1.set(u1);
        this.u2.set(u2);
        this.v1.set(v1);
        this.v2.set(v2);
        this.mAlignmentType = alignmentType;
    }

    @Before
    public void setup() {
        logger.setLevel(Level.FINE);
    }

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection mAlignVectorsTestData() {
        return Arrays.asList(new Object[][] {
                {new PointF(0, 0), new PointF(9, 0), new PointF(0, 0), new PointF(9, 1), AlignmentType.ALIGNMENT_TYPE_HORIZONTAL },
                {new PointF(0, 0), new PointF(10, 0), new PointF(0, 0), new PointF(10, -1), AlignmentType.ALIGNMENT_TYPE_HORIZONTAL },
                {new PointF(0, 0), new PointF(-10, 0), new PointF(0, 0), new PointF(-10, 1), AlignmentType.ALIGNMENT_TYPE_HORIZONTAL },
                {new PointF(0, 0), new PointF(-10, 0), new PointF(0, 0), new PointF(-10, -1), AlignmentType.ALIGNMENT_TYPE_HORIZONTAL },

                {new PointF(0, 0), new PointF(0, 10), new PointF(0, 0), new PointF(1, 10), AlignmentType.ALIGNMENT_TYPE_VERTICAL },
                {new PointF(0, 0), new PointF(0, 10), new PointF(0, 0), new PointF(-1, 10), AlignmentType.ALIGNMENT_TYPE_VERTICAL },
                {new PointF(0, 0), new PointF(0, -10), new PointF(0, 0), new PointF(1, -10), AlignmentType.ALIGNMENT_TYPE_VERTICAL },
                {new PointF(0, 0), new PointF(0, -10), new PointF(0, 0), new PointF(-1, -10), AlignmentType.ALIGNMENT_TYPE_VERTICAL },
        });
    }

    @Test
    public void alignVectorSharpAngleTest() {
        VectorHelper.alignVector(u1, u2, v1, v2, THRESHOLD);

        // Test that end vertex has been changed:
        switch (mAlignmentType) {

            case ALIGNMENT_TYPE_VERTICAL:
                assertThat(v2.x, is(v1.x));
                break;
            case ALIGNMENT_TYPE_HORIZONTAL:
                assertThat(v2.y, is(v1.y));
                break;
        }
    }
}

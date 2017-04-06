package com.example.neutrino.maze;

import android.graphics.Point;

import com.example.neutrino.maze.vectorization.HoughTransform;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Dima Ruinskiy on 03/01/17.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class HoughTransformTests {

    private static final Logger LOGGER = Logger.getLogger(HoughTransformTests.class.getName());

    // Test merging segments that are completely horizontal (Y=0)
    @Test
    public void HorizontalSegmentMergeTest() {

        List<HoughTransform.LineSegment> list = new ArrayList<>();

        list.add(new HoughTransform.LineSegment(new Point(21, 0), new Point(25, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(21, 0), new Point(30, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(1, 0), new Point(10, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(6, 0), new Point(11, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(3, 0), new Point(4, 0), new HoughTransform.HoughLine(0, 0)));

        List<HoughTransform.LineSegment> lsnew = HoughTransform.mergeSegments(list);

        for (HoughTransform.LineSegment seg : lsnew) {
            LOGGER.info(seg.start.toString() + " " + seg.end.toString() + "\n");
        }

        assertThat(lsnew.size(), is(equalTo(2)));
        assertTrue(lsnew.get(0).start.equals(1,0));
        assertTrue(lsnew.get(0).end.equals(11,0));
        assertTrue(lsnew.get(1).start.equals(21,0));
        assertTrue(lsnew.get(1).end.equals(30,0));
    }

    // Test merging segments that are completely vertical (X=0)
    @Test
    public void VerticalSegmentMergeTest() {

        List<HoughTransform.LineSegment> list = new ArrayList<>();

        list.add(new HoughTransform.LineSegment(new Point(0,-7), new Point(0,-2), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(0,15), new Point(0,22), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(0,2), new Point(0,6), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(0,4), new Point(0,18), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(0,3), new Point(0,5), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));

        List<HoughTransform.LineSegment> lsnew = HoughTransform.mergeSegments(list);

        for (HoughTransform.LineSegment seg : lsnew) {
            LOGGER.info(seg.start.toString() + " " + seg.end.toString() + "\n");
        }

        assertThat(lsnew.size(), is(equalTo(2)));
        assertTrue(lsnew.get(0).start.equals(0,-7));
        assertTrue(lsnew.get(0).end.equals(0,-2));
        assertTrue(lsnew.get(1).start.equals(0,2));
        assertTrue(lsnew.get(1).end.equals(0,22));
    }

    // Test merging a mixture of segments along 3 different lines (horizontal, vertical and at 45 degree angle)
    @Test
    public void MultipleLineSegmentMergeTest() {

        List<HoughTransform.LineSegment> list = new ArrayList<>();

        list.add(new HoughTransform.LineSegment(new Point(2, 2), new Point(3, 3), new HoughTransform.HoughLine(0,1)));
        list.add(new HoughTransform.LineSegment(new Point(0,-7), new Point(0,-2), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(6, 0), new Point(11, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(0,15), new Point(0,22), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(7, 7), new Point(12, 12), new HoughTransform.HoughLine(0,1)));
        list.add(new HoughTransform.LineSegment(new Point(11, 11), new Point(20, 20), new HoughTransform.HoughLine(0,1)));
        list.add(new HoughTransform.LineSegment(new Point(0,3), new Point(0,5), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(25, 0), new Point(30, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(21, 0), new Point(25, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(0,2), new Point(0,6), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(1, 0), new Point(10, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(0,4), new Point(0,18), new HoughTransform.HoughLine(0,(int)(Math.PI/HoughTransform.THETA_STEP))));
        list.add(new HoughTransform.LineSegment(new Point(3, 0), new Point(4, 0), new HoughTransform.HoughLine(0, 0)));
        list.add(new HoughTransform.LineSegment(new Point(1, 1), new Point(2, 2), new HoughTransform.HoughLine(0,1)));
        list.add(new HoughTransform.LineSegment(new Point(3, 3), new Point(4, 4), new HoughTransform.HoughLine(0,1)));

        List<HoughTransform.LineSegment> lsnew = HoughTransform.mergeSegments(list);

        for (HoughTransform.LineSegment seg : lsnew) {
            LOGGER.info(seg.start.toString() + " " + seg.end.toString() + "\n");
        }

        assertThat(lsnew.size(), is(equalTo(6)));
        assertTrue(lsnew.get(0).start.equals(1,0));
        assertTrue(lsnew.get(0).end.equals(11,0));
        assertTrue(lsnew.get(1).start.equals(21,0));
        assertTrue(lsnew.get(1).end.equals(30, 0));
        assertTrue(lsnew.get(2).start.equals(1,1));
        assertTrue(lsnew.get(2).end.equals(4,4));
        assertTrue(lsnew.get(3).start.equals(7,7));
        assertTrue(lsnew.get(3).end.equals(20,20));
        assertTrue(lsnew.get(4).start.equals(0,-7));
        assertTrue(lsnew.get(4).end.equals(0,-2));
        assertTrue(lsnew.get(5).start.equals(0,2));
        assertTrue(lsnew.get(5).end.equals(0, 22));
    }
}

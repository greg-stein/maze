package com.example.neutrino.maze;

import android.graphics.PointF;

import com.example.neutrino.maze.util.MovingAveragePointsQueue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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

/**
 * Created by Greg Stein on 4/11/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class MovingAveragePointsQueueTests {
    @Test
    public void commonPointsTest() {
        MovingAveragePointsQueue queue = new MovingAveragePointsQueue(3);
        queue.add(new PointF(1, 2));
        assertThat(queue.meanPoint().x, is(equalTo(1f / 1)));
        assertThat(queue.meanPoint().y, is(equalTo(2f / 1)));
        assertThat(queue.variance(), is(equalTo(0d)));

        queue.add(new PointF(3, 4));
        assertThat(queue.meanPoint().x, is(equalTo((1f + 3f) / 2)));
        assertThat(queue.meanPoint().y, is(equalTo((2f + 4f) / 2)));
        assertThat(queue.variance(), is(closeTo(2, 0.0005d)));

        queue.add(new PointF(5, 6));
        assertThat(queue.meanPoint().x, is(equalTo((1f + 3f + 5f) / 3)));
        assertThat(queue.meanPoint().y, is(equalTo((2f + 4f + 6f) / 3)));
        assertThat(queue.variance(), is(closeTo(5.333333333333333d, 0.0005d)));

        queue.add(new PointF(7, 8));
        assertThat(queue.meanPoint().x, is(equalTo((3f + 5f + 7f) / 3)));
        assertThat(queue.meanPoint().y, is(equalTo((4f + 6f + 8f) / 3)));
        assertThat(queue.variance(), is(closeTo(5.333333333333333d, 0.0005d)));
    }
}

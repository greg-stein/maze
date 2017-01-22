package com.example.neutrino.maze;

import android.graphics.Color;
import android.graphics.Point;

import com.example.neutrino.maze.floorplan.vectorization.ImageArray;
import com.example.neutrino.maze.floorplan.vectorization.PixelBuffer;
import com.example.neutrino.maze.floorplan.vectorization.PixelBufferChunk;

import junit.framework.Assert;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Greg Stein on 1/22/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PixelBufferTests {

    @Test
    public void pixelBufferTraverseTest() {
        final int CHUNK_SIZE = 10; // points
        final int DATA_SIZE = 34;  // points
        final int NUMBER_OF_CHUNKS = (int) Math.ceil(DATA_SIZE / (float)CHUNK_SIZE);

        PixelBuffer buffer = new PixelBuffer(CHUNK_SIZE );
        Point p = new Point(-1, 0);

        for (int coord = 0; coord < DATA_SIZE; coord++) {
            p.offset(1, 1); // generate sequence of points: (0,1),(1,2),(2,3),...
            buffer.putPixel(p);
        }

        assertThat(buffer, hasSize(NUMBER_OF_CHUNKS));

        Point expected = new Point(-1, 0);
        for (PixelBufferChunk chunk : buffer) {
            for (Point actual : chunk) {
                expected.offset(1, 1);
                assertThat(actual, is(equalTo(expected)));
            }
        }
    }
}

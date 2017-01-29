package com.example.neutrino.maze;

import android.graphics.Color;
import android.graphics.Point;

import com.example.neutrino.maze.floorplan.vectorization.ImageArray;
import com.example.neutrino.maze.floorplan.vectorization.KernelHoughTransform;
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
import java.util.List;

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

    @Test
    public void pixelBufferReverseTraverseTest() {
        final int CHUNK_SIZE = 10; // points
        final int DATA_SIZE = 34;  // points
        final int NUMBER_OF_CHUNKS = (int) Math.ceil(DATA_SIZE / (float)CHUNK_SIZE);

        PixelBuffer buffer = new PixelBuffer(CHUNK_SIZE);
        buffer.prepareForPushingBack();
        Point p = new Point(-1, 0);

        for (int coord = 0; coord < DATA_SIZE; coord++) {
            p.offset(1, 1); // generate sequence of points: (0,1),(1,2),(2,3),...
            buffer.pushBackPixel(p);
        }

        assertThat(buffer, hasSize(NUMBER_OF_CHUNKS + 1));

        Point expected = new Point(34, 35);
        for (PixelBufferChunk chunk : buffer) {
            for (Point actual : chunk) {
                expected.offset(-1, -1);
                assertThat(actual, is(equalTo(expected)));
            }
        }
    }

    @Test
    public void pixelBufferHybridTraverseTest() {
        final int CHUNK_SIZE = 10; // points
        final int DATA_SIZE = 34;  // points
        final int NUMBER_OF_CHUNKS = (int) Math.ceil(DATA_SIZE / (float)CHUNK_SIZE);

        PixelBuffer buffer = new PixelBuffer(CHUNK_SIZE);
        buffer.prepareForPushingBack();
        Point p = new Point(DATA_SIZE/2 - 1, DATA_SIZE/2); // (16, 17)

        for (int coord = 0; coord < DATA_SIZE/2 - 1; coord++) {
            p.offset(-1, -1); // generate sequence of points: (0,1),(1,2),(2,3),...,(16,17)
            buffer.pushBackPixel(p);
        }

        p = new Point(DATA_SIZE/2 - 2, DATA_SIZE/2 - 1); // (15,16) will be changes to (16, 17) in first iteration

        for (int coord = DATA_SIZE/2 - 1; coord < DATA_SIZE; coord++) {
            p.offset(1, 1); // generate sequence of points: (17,18),(18,19),...,(33,34)
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

    @Test
    public void pixelBufferDirectIndexingTest() {
        final int CHUNK_SIZE = 10; // points
        final int DATA_SIZE = 34;  // points
        final int NUMBER_OF_CHUNKS = (int) Math.ceil(DATA_SIZE / (float)CHUNK_SIZE);

        PixelBuffer buffer = new PixelBuffer(CHUNK_SIZE );
        Point p = new Point(-1, 0);

        for (int coord = 0; coord < DATA_SIZE; coord++) {
            p.offset(1, 1); // generate sequence of points: (0,1),(1,2),(2,3),...
            buffer.putPixel(p);
        }

        buffer.initCornersDetection();
        Point actual = new Point();
        int i = 0;
        for (PixelBufferChunk chunk : buffer) {
            for (Point point : chunk) {
                buffer.get(i++, actual);
                assertThat(actual, is(equalTo(point)));
            }
        }
    }

    @Test
    public void pixelBufferReverseIndexingTest() {
        final int CHUNK_SIZE = 10; // points
        final int DATA_SIZE = 34;  // points
        final int NUMBER_OF_CHUNKS = (int) Math.ceil(DATA_SIZE / (float)CHUNK_SIZE);

        PixelBuffer buffer = new PixelBuffer(CHUNK_SIZE);
        buffer.prepareForPushingBack();
        Point p = new Point(-1, 0);

        for (int coord = 0; coord < DATA_SIZE; coord++) {
            p.offset(1, 1); // generate sequence of points: (0,1),(1,2),(2,3),...
            buffer.pushBackPixel(p);
        }

        buffer.initCornersDetection();
        Point actual = new Point();
        int i = 0;
        for (PixelBufferChunk chunk : buffer) {
            for (Point point : chunk) {
                buffer.get(i++, actual);
                assertThat(actual, is(equalTo(point)));
            }
        }
    }

    @Test
    public void pixelBufferHybridIndexingTest() {
        final int CHUNK_SIZE = 10; // points
        final int DATA_SIZE = 34;  // points
        final int NUMBER_OF_CHUNKS = (int) Math.ceil(DATA_SIZE / (float)CHUNK_SIZE);

        PixelBuffer buffer = new PixelBuffer(CHUNK_SIZE);
        buffer.prepareForPushingBack();
        Point p = new Point(DATA_SIZE/2 - 1, DATA_SIZE/2); // (16, 17)

        for (int coord = 0; coord < DATA_SIZE/2 - 1; coord++) {
            p.offset(-1, -1); // generate sequence of points: (0,1),(1,2),(2,3),...,(16,17)
            buffer.pushBackPixel(p);
        }

        p = new Point(DATA_SIZE/2 - 2, DATA_SIZE/2 - 1); // (15,16) will be changes to (16, 17) in first iteration

        for (int coord = DATA_SIZE/2 - 1; coord < DATA_SIZE; coord++) {
            p.offset(1, 1); // generate sequence of points: (17,18),(18,19),...,(33,34)
            buffer.putPixel(p);
        }

        buffer.initCornersDetection();
        Point actual = new Point();
        int i = 0;
        for (PixelBufferChunk chunk : buffer) {
            for (Point point : chunk) {
                buffer.get(i++, actual);
                assertThat(actual, is(equalTo(point)));
            }
        }
    }

    @Test
    public void simpleSubdivideTest() {
        int[] data = new int[] {
                //   0            1            2            3            4            5            6            7            8            9
                Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 0
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 1
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 2
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 3
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 4
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 5
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 6
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 7
                Color.WHITE, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.WHITE, // 8
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 9
                Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 10
        };
        ImageArray imageArray = new ImageArray(data, 10, 11);
        imageArray.findBlackPixels();
        KernelHoughTransform kht = new KernelHoughTransform(imageArray);
        List<PixelBuffer> chains = kht.getPixelChains(imageArray);

        PixelBuffer chain = chains.get(0);
        chain.initCornersDetection();
        chain.findStraightSegments();
        int[] expectedCorners = new int[14];
        expectedCorners[0] = expectedCorners[13] = expectedCorners[6] = PixelBuffer.CORNER;

        assertThat(chain.mCorners, is(equalTo(expectedCorners)));
    }

    @Test
    public void oneRecursiveCallSubdivideTest() { //TODO!!!
        int[] data = new int[] {
                //   0            1            2            3            4            5            6            7            8            9
                Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 0
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 1
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 2
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 3
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 4
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 5
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 6
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 7
                Color.WHITE, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.WHITE, // 8
                Color.WHITE, Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 9
                Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, // 10
        };
        ImageArray imageArray = new ImageArray(data, 10, 11);
        imageArray.findBlackPixels();
        KernelHoughTransform kht = new KernelHoughTransform(imageArray);
        List<PixelBuffer> chains = kht.getPixelChains(imageArray);

        PixelBuffer chain = chains.get(0);
        chain.initCornersDetection();
        chain.findStraightSegments();
        int[] expectedCorners = new int[14];
        expectedCorners[0] = expectedCorners[13] = expectedCorners[6] = PixelBuffer.CORNER;

        assertThat(chain.mCorners, is(equalTo(expectedCorners)));
    }
}

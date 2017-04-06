package com.example.neutrino.maze;

import android.graphics.Color;
import android.graphics.Point;

import com.example.neutrino.maze.vectorization.ImageArray;
import com.example.neutrino.maze.vectorization.PixelBufferChunk;
import com.example.neutrino.maze.vectorization.PixelBufferChunkReversed;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 1/19/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PixelBufferChunkReversedTests {

    @Test
    public void pixelBufferChunkIteratorTest() {
        final int SIZE = 10;
        PixelBufferChunk chunk = new PixelBufferChunkReversed(SIZE);
        int coord = 0;
        for (int i = 0; i < SIZE; i++) {
            chunk.putPixel(coord++, coord++);
        }

        int i = SIZE * 2 - 1;
        for (Point p : chunk) {
            assertThat(p.y, is(equalTo(i--)));
            assertThat(p.x, is(equalTo(i--)));
        }

        assertThat(i, is(equalTo(-1)));
    }

    @Test
    public void pixelBufferChunkIteratorRemoveTest() {
        final int SIZE = 10;
        PixelBufferChunk chunk = new PixelBufferChunkReversed(SIZE);
        int coord = 0;
        for (int i = 0; i < SIZE; i++) {
            chunk.putPixel(coord++, coord++);
        }

        Point p2delete = new Point(5, 4);
        Iterator<Point> iterator = chunk.iterator();
        while (iterator.hasNext()) {
            Point p = iterator.next();
            if (p.equals(p2delete)) {
                iterator.remove();
            }
        }

        Point markedAsDeleted = new Point(-1, -1);
        for (Point p : chunk) {
            assertThat(p, is(not(equalTo(p2delete))));
            // Check that (-1, -1) is not returned by iterator
            assertThat(p, is(not(equalTo(markedAsDeleted))));
        }
        assertThat(chunk, not(hasItem(p2delete)));
    }

    @Test
    public void iterator2deletesTest() {
        final int SIZE = 10;
        PixelBufferChunk chunk = new PixelBufferChunkReversed(SIZE);
        int coord = 0;
        for (int i = 0; i < SIZE; i++) {
            chunk.putPixel(coord++, coord++);
        }

        Point p0 = new Point(0, 1);
        Point p1 = new Point(2, 3);
        Point p2 = new Point(4, 5);
        Point p3 = new Point(6, 7);
        Point p4 = new Point(8, 9);
        Point p5 = new Point(10, 11);
        Point p6 = new Point(12, 13);
        Point p7 = new Point(14, 15);
        Point p8 = new Point(16, 17);
        Point p9 = new Point(18, 19);
        Iterator<Point> iterator = chunk.iterator();
        while (iterator.hasNext()) {
            Point p = iterator.next();
            if (p.equals(p3) || p.equals(p5)) {
                iterator.remove();
            }
        }

        Point markedAsDeleted = new Point(-1, -1);
        for (Point p : chunk) {
            assertThat(p, is(not(equalTo(p3))));
            assertThat(p, is(not(equalTo(p5))));
            // Check that (-1, -1) is not returned by iterator
            assertThat(p, is(not(equalTo(markedAsDeleted))));
        }
        assertThat(chunk, not(hasItem(p3)));
        assertThat(chunk, not(hasItem(p5)));
        assertThat(chunk, hasItems(p0, p1, p2, p4, p6, p7, p8, p9));
    }

    @Test
    public void iteratorDeleteConsequentPointsTest() {
        final int SIZE = 10;
        PixelBufferChunk chunk = new PixelBufferChunkReversed(SIZE);
        int coord = 0;
        for (int i = 0; i < SIZE; i++) {
            chunk.putPixel(coord++, coord++);
        }

        Point p0 = new Point(0, 1);
        Point p1 = new Point(2, 3);
        Point p2 = new Point(4, 5);
        Point p3 = new Point(6, 7);
        Point p4 = new Point(8, 9);
        Point p5 = new Point(10, 11);
        Point p6 = new Point(12, 13);
        Point p7 = new Point(14, 15);
        Point p8 = new Point(16, 17);
        Point p9 = new Point(18, 19);

        Iterator<Point> iterator = chunk.iterator();
        while (iterator.hasNext()) {
            Point p = iterator.next();
            if (p.equals(p3) || p.equals(p4) || p.equals(p5)) {
                iterator.remove();
            }
        }

        Point markedAsDeleted = new Point(-1, -1);
        for (Point p : chunk) {
            assertThat(p, is(not(equalTo(p3))));
            assertThat(p, is(not(equalTo(p4))));
            assertThat(p, is(not(equalTo(p5))));
            // Check that (-1, -1) is not returned by iterator
            assertThat(p, is(not(equalTo(markedAsDeleted))));
            System.out.print(String.format("(%d, %d)--", p.x, p.y));
        }
        System.out.println("[]");
        assertThat(chunk, not(hasItem(p3)));
        assertThat(chunk, not(hasItem(p4)));
        assertThat(chunk, not(hasItem(p5)));
        assertThat(chunk, hasItems(p0, p1, p2, p6, p7, p8, p9));
    }

    @Test
    public void iteratorDeleteLastPointsTest() {
        final int SIZE = 10;
        PixelBufferChunk chunk = new PixelBufferChunkReversed(SIZE);
        int coord = 0;
        for (int i = 0; i < SIZE; i++) {
            chunk.putPixel(coord++, coord++);
        }

        Point p0 = new Point(0, 1);
        Point p1 = new Point(2, 3);
        Point p2 = new Point(4, 5);
        Point p3 = new Point(6, 7);
        Point p4 = new Point(8, 9);
        Point p5 = new Point(10, 11);
        Point p6 = new Point(12, 13);
        Point p7 = new Point(14, 15);
        Point p8 = new Point(16, 17);
        Point p9 = new Point(18, 19);

        Iterator<Point> iterator = chunk.iterator();
        while (iterator.hasNext()) {
            Point p = iterator.next();
            if (p.equals(p7) || p.equals(p8) || p.equals(p9)) {
                iterator.remove();
            }
        }

        Point markedAsDeleted = new Point(-1, -1);
        for (Point p : chunk) {
            assertThat(p, is(not(equalTo(p7))));
            assertThat(p, is(not(equalTo(p8))));
            assertThat(p, is(not(equalTo(p9))));
            // Check that (-1, -1) is not returned by iterator
            assertThat(p, is(not(equalTo(markedAsDeleted))));
            System.out.print(String.format("(%d, %d)--", p.x, p.y));
        }
        System.out.println("[]");
        assertThat(chunk, not(hasItem(p7)));
        assertThat(chunk, not(hasItem(p8)));
        assertThat(chunk, not(hasItem(p9)));
        assertThat(chunk, hasItems(p0, p1, p2, p3, p4, p5, p6));
    }

    @Test
    public void pixelBufferChunkTest() {
        Point[] points = new Point[]{
                new Point(0, 1),
                new Point(2, 3),
                new Point(4, 5),
                new Point(6, 7),
                new Point(8, 9),
                new Point(10, 11),
                new Point(12, 13),
                new Point(14, 15),
                new Point(16, 17),
                new Point(18, 19)
        };

        PixelBufferChunk chunk = new PixelBufferChunkReversed(10);
        for (Point p : points) {
            chunk.putPixel(p.x, p.y);
        }

        int counter = 9;
        Point p = new Point(-1, -1);
        while (p.x != 0 || p.y != 0) {

            chunk.getPixel(p);
            if (p.x != 0 || p.y != 0) {
                assertThat(p, is(equalTo(points[counter--])));
            }
        }

        chunk.reset();
        counter = 9;
        p.set(-1, -1);

        while (p.x != 0 || p.y != 0) {
            chunk.getPixel(p);
            if (p.x != 0 || p.y != 0) {
                assertThat(p, is(equalTo(points[counter--])));
            }
        }
    }

    @Test
    public void pixelBufferChunkRemovePixelTest() {
        Point[] points = new Point[]{
                                    // indices as they appear in the chunk
                new Point(0, 1),    // 9 * 2
                new Point(2, 3),    // 8 * 2
                new Point(4, 5),    // 7 * 2
                new Point(6, 7),    // 6 * 2
                new Point(8, 9),    // 5 * 2
                new Point(10, 11),  // 4 * 2
                new Point(12, 13),  // 3 * 2
                new Point(14, 15),  // 2 * 2
                new Point(16, 17),  // 1 * 2
                new Point(18, 19)   // 0 * 2
        };

        PixelBufferChunk chunk = new PixelBufferChunkReversed(10);
        for (Point p : points) {
            chunk.putPixel(p.x, p.y);
        }

        chunk.reset();
        for (Point point = new Point(-1, -1); point.x != 0 || point.y != 0; chunk.getPixel(point)) {
            if (point.x == -1 && point.y == -1) continue; // removed pixel?

            if (point.x == 6 && point.y == 7) {
                chunk.removePixel();
            }
        }

        // HAMCREST IS SHIT SHIT SHIT SHIT!
        // IT DOESN'T HAVE FUCKING MATCHERS FOR PRIMITIVE TYPE ARRAYS! FUCK IT!
        for (int i = 0; i < chunk.coords.length; i++) {
            if (chunk.coords[i] == 6 || chunk.coords[i] == 7) {
                Assert.fail("The point wasn't removed");
            }
        }

        assertThat(chunk.coords[12], is(equalTo(PixelBufferChunk.REMOVED_PIXEL)));
        assertThat(chunk.coords[13], is(equalTo(PixelBufferChunk.REMOVED_PIXEL)));
    }

    @Test
    public void pixelBufferChunkCompactTest() {
        PixelBufferChunk chunk = new PixelBufferChunk(10);
        int i;
        for (i = 0; i < 20; i += 2) {
            chunk.coords[i] = 1+(i*i)%17;
            chunk.coords[i+1] = 1+(i*i)%13;
        }
        for (i = 0; i < 22; i += 2) {
            System.out.print(chunk.coords[i] + " " + chunk.coords[i+1] + " ");
        }
        System.out.println();

        chunk.coords[0] = chunk.coords[1] = chunk.coords[10] = chunk.coords[11] = chunk.coords[14] = chunk.coords[15] = -1;

        for (i = 0; i < 22; i += 2) {
            System.out.print(chunk.coords[i] + " " + chunk.coords[i+1] + " ");
        }
        System.out.println();

        chunk.compact();

        for (i = 0; i < 22; i += 2) {
            System.out.print(chunk.coords[i] + " " + chunk.coords[i+1] + " ");
        }
        System.out.println();
    }

    @Test
    public void pixelBufferChunkMultiChunkTest() {
        // In this test we create image with number of black (foreground)
        // pixels > PIXEL_BUFFER_CHUNK_SIZE to generate two chunks
        final int DATA_LENGTH = ImageArray.PIXEL_BUFFER_CHUNK_SIZE * 2 + ImageArray.PIXEL_BUFFER_CHUNK_SIZE/2;
        int[] data = new int[DATA_LENGTH];
        Arrays.fill(data, Color.BLACK);
        ImageArray imageArray = new ImageArray(data, DATA_LENGTH/64, 64);
        imageArray.findBlackPixels();

        assertThat(imageArray.blackPixelsNum, is(equalTo(DATA_LENGTH - 1))); //(0,0) EOD mark
        assertNotNull(imageArray.pixelBufferChunks);
        assertThat(imageArray.pixelBufferChunks, hasSize(3));
        assertNotNull(imageArray.pixelBufferChunks.get(0));
        assertThat(imageArray.pixelBufferChunks.get(0).coordsCount, is(equalTo(2*ImageArray.PIXEL_BUFFER_CHUNK_SIZE)));
        assertNotNull(imageArray.pixelBufferChunks.get(1));
        assertThat(imageArray.pixelBufferChunks.get(1).coordsCount, is(equalTo(2*ImageArray.PIXEL_BUFFER_CHUNK_SIZE)));
        assertNotNull(imageArray.pixelBufferChunks.get(2));
        assertThat(imageArray.pixelBufferChunks.get(2).coordsCount, is(equalTo(2*(ImageArray.PIXEL_BUFFER_CHUNK_SIZE/2 - 1))));
    }
}


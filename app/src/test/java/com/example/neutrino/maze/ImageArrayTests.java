package com.example.neutrino.maze;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;

import com.example.neutrino.maze.floorplan.vectorization.ImageArray;
import com.example.neutrino.maze.floorplan.vectorization.PixelBufferChunk;

import junit.framework.Assert;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Array;
import java.util.Arrays;

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

/**
 * Created by Greg Stein on 12/20/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ImageArrayTests {

    @Test
    public void imageArrayIndexingTest() {
        int[] data = new int[] {
                Color.BLACK, Color.WHITE, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.WHITE,
                Color.BLACK, Color.WHITE, Color.BLACK
        };
        ImageArray imageArray = new ImageArray(data, 3, 3);

        assertThat(imageArray.dataLength, is(Matchers.equalTo(9)));
        assertThat(imageArray.width, is(Matchers.equalTo(3)));
        assertThat(imageArray.height, is(Matchers.equalTo(3)));
        assertThat(imageArray.get(0,0), is(Matchers.equalTo(Color.BLACK)));
        assertThat(imageArray.get(1,0), is(Matchers.equalTo(Color.WHITE)));
        assertThat(imageArray.get(2,0), is(Matchers.equalTo(Color.BLACK)));
        assertThat(imageArray.get(0,1), is(Matchers.equalTo(Color.WHITE)));
        assertThat(imageArray.get(1,1), is(Matchers.equalTo(Color.BLACK)));
        assertThat(imageArray.get(2,1), is(Matchers.equalTo(Color.WHITE)));
        assertThat(imageArray.get(0,2), is(Matchers.equalTo(Color.BLACK)));
        assertThat(imageArray.get(1,2), is(Matchers.equalTo(Color.WHITE)));
        assertThat(imageArray.get(2,2), is(Matchers.equalTo(Color.BLACK)));

        imageArray.findBlackPixels();
        // There are 5 black pixels, but we ignore first pixel at (0, 0).
        // This is because (0, 0) is special end-mark. Hence we expect 4
        assertThat(imageArray.blackPixelsNum, is(Matchers.equalTo(5 - 1)));
    }

    @Test
    public void findBlackPixelsTest() {
        // 4 black pixels
        int[] colors = new int[]{
                Color.WHITE, Color.BLACK, Color.WHITE,
                Color.BLACK, Color.WHITE, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.WHITE,
        };

        Bitmap bitmap = Bitmap.createBitmap(colors, 3, 3, Bitmap.Config.ARGB_8888);
        ImageArray imageArray = new ImageArray(bitmap);
        imageArray.findBlackPixels();

        assertThat(imageArray.blackPixelsNum, is(equalTo(4)));
        assertNotNull(imageArray.pixelBufferChunks);
        assertThat(imageArray.pixelBufferChunks, hasSize(1));
        PixelBufferChunk chunk = imageArray.pixelBufferChunks.get(0);
        assertNotNull(chunk);
        assertThat(chunk.pixelsCount, is(4 * 2));
        int[] expectedCoords = new int[]{
                1, 0,
                0, 1,
                2, 1,
                1, 2
        };
        assertArrayEquals(expectedCoords, Arrays.copyOfRange(chunk.coords, 0, 8));
    }

    @Test
    public void findBlackPixels2ChunkTest() {
        final int IMAGE_WIDTH = 64;

        int[] colors = new int[(int) (ImageArray.PIXEL_BUFFER_CHUNK_SIZE * 1.5)]; // 1.5 * 4K
        Arrays.fill(colors, Color.BLACK);
        Bitmap bitmap = Bitmap.createBitmap(colors, IMAGE_WIDTH, colors.length / IMAGE_WIDTH, Bitmap.Config.ARGB_8888);
        ImageArray imageArray = new ImageArray(bitmap);
        imageArray.findBlackPixels();

        assertThat(imageArray.blackPixelsNum, is(equalTo(colors.length - 1))); // 1 - first pixel (0, 0) is ignored
        assertNotNull(imageArray.pixelBufferChunks);
        assertThat(imageArray.pixelBufferChunks, hasSize(2));

        PixelBufferChunk chunk1 = imageArray.pixelBufferChunks.get(0);
        assertNotNull(chunk1);
        assertThat(chunk1.pixelsCount, is(2 * ImageArray.PIXEL_BUFFER_CHUNK_SIZE));
        PixelBufferChunk chunk2 = imageArray.pixelBufferChunks.get(1);
        assertNotNull(chunk2);
        assertThat(chunk2.pixelsCount, is(2 * (ImageArray.PIXEL_BUFFER_CHUNK_SIZE / 2 - 1)));
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

        PixelBufferChunk chunk = new PixelBufferChunk(10);
        for (Point p : points) {
            chunk.putPixel(p.x, p.y);
        }

        int counter = 0;
        Point p = new Point(-1, -1);
        while (p.x != 0 || p.y != 0) {

            chunk.getPixel(p);
            if (p.x != 0 || p.y != 0) {
                assertThat(p, is(equalTo(points[counter++])));
            }
        }
        assertThat(counter, is(equalTo(points.length)));

        chunk.reset();
        counter = 0;
        p.set(-1, -1);

        while (p.x != 0 || p.y != 0) {
            chunk.getPixel(p);
            if (p.x != 0 || p.y != 0) {
                assertThat(p, is(equalTo(points[counter++])));
            }
        }
        assertThat(counter, is(equalTo(points.length)));
    }

    @Test
    public void pixelBufferChunkRemovePixelTest() {
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

        PixelBufferChunk chunk = new PixelBufferChunk(10);
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

        assertThat(chunk.coords[6], is(equalTo(PixelBufferChunk.REMOVED_PIXEL)));
        assertThat(chunk.coords[7], is(equalTo(PixelBufferChunk.REMOVED_PIXEL)));
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
        assertThat(imageArray.pixelBufferChunks.get(0).pixelsCount, is(equalTo(2*ImageArray.PIXEL_BUFFER_CHUNK_SIZE)));
        assertNotNull(imageArray.pixelBufferChunks.get(1));
        assertThat(imageArray.pixelBufferChunks.get(1).pixelsCount, is(equalTo(2*ImageArray.PIXEL_BUFFER_CHUNK_SIZE)));
        assertNotNull(imageArray.pixelBufferChunks.get(2));
        assertThat(imageArray.pixelBufferChunks.get(2).pixelsCount, is(equalTo(2*(ImageArray.PIXEL_BUFFER_CHUNK_SIZE/2 - 1))));
    }
}

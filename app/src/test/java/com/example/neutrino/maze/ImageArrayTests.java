package com.example.neutrino.maze;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.example.neutrino.maze.vectorization.ImageArray;
import com.example.neutrino.maze.vectorization.PixelBufferChunk;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
        assertThat(chunk.coordsCount, is(4 * 2));
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
        assertThat(chunk1.coordsCount, is(2 * ImageArray.PIXEL_BUFFER_CHUNK_SIZE));
        PixelBufferChunk chunk2 = imageArray.pixelBufferChunks.get(1);
        assertNotNull(chunk2);
        assertThat(chunk2.coordsCount, is(2 * (ImageArray.PIXEL_BUFFER_CHUNK_SIZE / 2 - 1)));
    }
}

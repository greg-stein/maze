package com.example.neutrino.maze;

import android.graphics.Color;

import com.example.neutrino.maze.floorplan.vectorization.ImageArray;
import com.example.neutrino.maze.floorplan.vectorization.KernelHoughTransform;
import com.example.neutrino.maze.floorplan.vectorization.PixelBufferChunk;
import com.example.neutrino.maze.floorplan.vectorization.PixelBuffer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Greg Stein on 1/18/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class KernelHoughTransformTests {

    public static final int W = Color.WHITE;
    public static final int B = Color.BLACK;

    @Test
    public void linkingPixelChains2DisjointChainsTest() {
        int[] data = new int[] {
             // 0  1  2  3  4  5
                W, W, W, W, W, W, // 0
                W, B, W, B, W, W, // 1
                W, B, W, B, W, W, // 2
                W, B, W, B, W, W, // 3
                W, B, W, B, W, W, // 4
                W, W, W, W, W, W, // 5
        };
        ImageArray imageArray = new ImageArray(data, 6, 6);
        imageArray.findBlackPixels();
        KernelHoughTransform kht = new KernelHoughTransform(imageArray);
        List<PixelBuffer> chains = kht.getPixelChains(imageArray);

        PixelBuffer expectedChain1 = new PixelBuffer();
        PixelBufferChunk chunk1 = new PixelBufferChunk(PixelBuffer.BUFFER_CHUNK_DEFAULT_SIZE);
        chunk1.putPixel(1, 1);
        chunk1.putPixel(1, 2);
        chunk1.putPixel(1, 3);
        chunk1.putPixel(1, 4);
        expectedChain1.add(chunk1);

        PixelBuffer expectedChain2 = new PixelBuffer();
        PixelBufferChunk chunk2 = new PixelBufferChunk(PixelBuffer.BUFFER_CHUNK_DEFAULT_SIZE);
        chunk2.putPixel(3, 1);
        chunk2.putPixel(3, 2);
        chunk2.putPixel(3, 3);
        chunk2.putPixel(3, 4);
        expectedChain2.add(chunk2);

        int[] actualCoords1 =  chains.get(0).get(0).coords;
        int[] actualCoords2 =  chains.get(1).get(0).coords;

        assertNotNull(chains);
        assertThat(chains, hasSize(2));
        assertThat(actualCoords1.length, is(equalTo(chunk1.coords.length)));
        assertThat(actualCoords1, is(equalTo(chunk1.coords)));
        assertThat(actualCoords2.length, is(equalTo(chunk2.coords.length)));
        assertThat(actualCoords2, is(equalTo(chunk2.coords)));
    }

    @Test
    public void linkingPixelChains2JointChainsOneShortTest() {
        int[] data = new int[] {
             // 0  1  2  3  4  5
                W, W, W, W, W, W, // 0
                W, B, W, W, W, W, // 1
                W, B, W, W, W, W, // 2
                W, B, B, B, B, W, // 3
                W, B, W, W, W, W, // 4
                W, W, W, W, W, W, // 5
        };
        ImageArray imageArray = new ImageArray(data, 6, 6);
        imageArray.findBlackPixels();
        KernelHoughTransform kht = new KernelHoughTransform(imageArray);
        List<PixelBuffer> chains = kht.getPixelChains(imageArray);

        PixelBuffer expectedChain1 = new PixelBuffer();
        PixelBufferChunk chunk1 = new PixelBufferChunk(PixelBuffer.BUFFER_CHUNK_DEFAULT_SIZE);
        chunk1.putPixel(1, 1);
        chunk1.putPixel(1, 2);
        chunk1.putPixel(2, 3);
        chunk1.putPixel(3, 3);
        chunk1.putPixel(4, 3);
        expectedChain1.add(chunk1);

        // The second chain is shorter than KernelHoughTransform.MIN_PIXELS_IN_CHAIN, so not reported
        int[] actualCoords1 =  chains.get(0).get(0).coords;

        assertNotNull(chains);
        assertThat(chains, hasSize(1));
        assertThat(actualCoords1.length, is(equalTo(chunk1.coords.length)));
        assertThat(actualCoords1, is(equalTo(chunk1.coords)));
    }

    @Test
    public void linkingPixelChains2JointChainsTest() {
        int[] data = new int[] {
             // 0  1  2  3  4  5
                W, W, W, W, W, W, // 0
                W, B, W, W, W, W, // 1
                W, B, W, W, W, W, // 2
                W, B, B, B, B, W, // 3
                W, B, W, W, W, W, // 4
                W, B, W, W, W, W, // 5
                W, B, W, W, W, W, // 6
                W, B, W, W, W, W, // 7
                W, W, W, W, W, W, // 8
        };
        ImageArray imageArray = new ImageArray(data, 6, 9);
        imageArray.findBlackPixels();
        KernelHoughTransform kht = new KernelHoughTransform(imageArray);
        List<PixelBuffer> chains = kht.getPixelChains(imageArray);

        PixelBuffer expectedChain1 = new PixelBuffer();
        PixelBufferChunk chunk1 = new PixelBufferChunk(PixelBuffer.BUFFER_CHUNK_DEFAULT_SIZE);
        chunk1.putPixel(1, 1);
        chunk1.putPixel(1, 2);
        chunk1.putPixel(2, 3);
        chunk1.putPixel(3, 3);
        chunk1.putPixel(4, 3);
        expectedChain1.add(chunk1);

        PixelBuffer expectedChain2 = new PixelBuffer();
        PixelBufferChunk chunk2 = new PixelBufferChunk(PixelBuffer.BUFFER_CHUNK_DEFAULT_SIZE);
        chunk2.putPixel(1, 3);
        chunk2.putPixel(1, 4);
        chunk2.putPixel(1, 5);
        chunk2.putPixel(1, 6);
        chunk2.putPixel(1, 7);
        expectedChain2.add(chunk2);

        // The second chain is shorter than KernelHoughTransform.MIN_PIXELS_IN_CHAIN, so not reported
        int[] actualCoords1 =  chains.get(0).get(0).coords;
        int[] actualCoords2 =  chains.get(1).get(0).coords;

        assertNotNull(chains);
        assertThat(chains, hasSize(2));
        assertThat(actualCoords1.length, is(equalTo(chunk1.coords.length)));
        assertThat(actualCoords1, is(equalTo(chunk1.coords)));
        assertThat(actualCoords2.length, is(equalTo(chunk2.coords.length)));
        assertThat(actualCoords2, is(equalTo(chunk2.coords)));
    }


    @Test
    public void simpleSubdivideTest() {
        int[] data = new int[] {
             // 0  1  2  3  4  5  6  7  8  9
                W, W, W, W, W, W, W, W, W, W, // 0
                W, B, W, W, W, W, W, W, W, W, // 1
                W, B, W, W, W, W, W, W, W, W, // 2
                W, B, W, W, W, W, W, W, W, W, // 3
                W, B, W, W, W, W, W, W, W, W, // 4
                W, B, W, W, W, W, W, W, W, W, // 5
                W, B, W, W, W, W, W, W, W, W, // 6
                W, B, W, W, W, W, W, W, W, W, // 7
                W, B, B, B, B, B, B, B, B, W, // 8
                W, B, W, W, W, W, W, W, W, W, // 9
                W, W, W, W, W, W, W, W, W, W, // 10
        };
        ImageArray imageArray = new ImageArray(data, 10, 11);
        imageArray.findBlackPixels();
        KernelHoughTransform kht = new KernelHoughTransform(imageArray);
        List<PixelBuffer> chains = kht.getPixelChains(imageArray);

        PixelBuffer chain = chains.get(0);
        chain.initCornersDetection();
        kht.findStraightSegments(chain);
        int[] expectedCorners = new int[14];
        expectedCorners[0] = expectedCorners[13] = expectedCorners[6] = PixelBuffer.CORNER;

        assertThat(chain.mCorners, is(equalTo(expectedCorners)));
    }

    @Test
    public void oneRecursiveCallSubdivideTest() {
        int[] data = new int[] {
             // 0  1  2  3  4  5  6  7  8  9 10 11 12 13
                W, W, W, W, W, W, W, W, W, W, W, W, W, W, // 0
                W, B, W, W, W, W, W, W, W, W, W, W, W, W, // 1
                W, B, W, W, W, W, W, W, W, W, W, W, W, W, // 2
                W, B, W, W, W, W, W, W, W, W, W, W, B, W, // 3
                W, B, W, W, W, W, W, W, W, W, W, B, W, W, // 4
                W, B, W, W, W, W, W, W, W, W, B, W, W, W, // 5
                W, B, W, W, W, W, W, W, W, B, W, W, W, W, // 6
                W, B, W, W, W, W, W, W, B, W, W, W, W, W, // 7
                W, B, B, B, B, B, B, B, W, W, W, W, W, W, // 8
                W, B, W, W, W, W, W, W, W, W, W, W, W, W, // 9
                W, W, W, W, W, W, W, W, W, W, W, W, W, W, // 10
        };
        ImageArray imageArray = new ImageArray(data, 14, 11);
        imageArray.findBlackPixels();
        KernelHoughTransform kht = new KernelHoughTransform(imageArray);
        List<PixelBuffer> chains = kht.getPixelChains(imageArray);

        PixelBuffer chain = chains.get(0);
        chain.initCornersDetection();
        kht.findStraightSegments(chain);
        int[] expectedCorners = new int[18];
        expectedCorners[0] = expectedCorners[7] = expectedCorners[12] = expectedCorners[17] = PixelBuffer.CORNER;

        assertThat(chain.mCorners, is(equalTo(expectedCorners)));
    }
}

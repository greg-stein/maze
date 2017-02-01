package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Color;
import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

import static com.example.neutrino.maze.floorplan.vectorization.PixelBuffer.CORNER;

/**
 * Created by Greg Stein on 1/18/2017.
 */
public class KernelHoughTransform {
    private static final int NEIGHBOURS_NUM = 8;
    private static final int[] NEIGHBOURS_X_OFFSET = { 0,  1,  0,  0, -1, -1,  0,  0};
    private static final int[] NEIGHBOURS_Y_OFFSET = {-1,  0,  1,  1,  0,  0, -1, -1};
    private static final int INVALID_COORD = -1;
    private static final Point INVALID_PIXEL = new Point(INVALID_COORD, INVALID_COORD);
    private static final int MIN_PIXELS_IN_CHAIN = 3;
    private static final int MIN_SEGMENT_LENGTH = 4;
    private static final double MIN_DEVIATION_RATIO = 0.05d;

    private ImageArray mImage;

    public KernelHoughTransform(ImageArray image) {
        mImage = image;
    }


    private PixelBuffer link(ImageArray image, Point pRef) {
        PixelBuffer pixels = new PixelBuffer();
        Point p = new Point(pRef);

        do {
            pixels.putPixel(p);
            image.set(p, Color.WHITE);
            next(image, p);
        } while (!p.equals(INVALID_PIXEL));

        p.set(pRef.x, pRef.y);
        next(image, p);
        if (!p.equals(INVALID_PIXEL)) {
            do {
                pixels.pushBackPixel(p); // add in reverse
                image.set(p, Color.WHITE);
                next(image, p);
            } while (!p.equals(INVALID_PIXEL));
        }

        return pixels;
    }

    private void next(ImageArray image, Point pSeed) {
        for (int neighbourIndex = 0; neighbourIndex < NEIGHBOURS_NUM; neighbourIndex++) {
            pSeed.offset(NEIGHBOURS_X_OFFSET[neighbourIndex], NEIGHBOURS_Y_OFFSET[neighbourIndex]);
            if (image.get(pSeed) == Color.BLACK) {
                return;
            }
        }
        pSeed.set(INVALID_PIXEL.x, INVALID_PIXEL.y);
    }

    public List<PixelBuffer> getPixelChains(ImageArray image) {
        List<PixelBuffer> chains = new ArrayList<>();

        for (PixelBufferChunk chunk : image.pixelBufferChunks) {
            for (Point p : chunk) {
                if (image.get(p) == Color.BLACK) {
                    final PixelBuffer chain = link(image, p);
                    if (chain.getPixelsCount() >= MIN_PIXELS_IN_CHAIN) {
                        chains.add(chain);
                    }
                }
            }
        }
        return chains;
    }

    public  void subdivide(PixelBuffer buffer, int startIdx, int endIdx) {
        Point start = new Point();
        Point end = new Point();
        buffer.mCorners[startIdx] = CORNER;
        buffer.mCorners[endIdx] = CORNER;
        buffer.get(startIdx, start);
        buffer.get(endIdx, end);
        final int xDiff = end.x - start.x;
        final int yDiff = end.y - start.y;

        int maxDeviation = 0;
        int maxDevIndex = 0;
        Point p = new Point();
        for (int index = startIdx; index <= endIdx; index++) {
            buffer.get(index, p);

            final int deviation = Math.abs(yDiff * p.x - xDiff * p.y + end.x * start.y - end.y * start.x);
            if (deviation > maxDeviation) {
                maxDeviation = deviation;
                maxDevIndex = index;
            }
        }

        // Do both parts satisfy minimum line segment length condition?
        if (maxDevIndex - startIdx > MIN_SEGMENT_LENGTH && endIdx - maxDevIndex > MIN_SEGMENT_LENGTH) {
            final int squaredLength = xDiff * xDiff + yDiff * yDiff;
            // Minimum deviation-to-length ratio condition
            if ((double)maxDeviation / squaredLength > MIN_DEVIATION_RATIO) {
                subdivide(buffer, startIdx, maxDevIndex);
                subdivide(buffer, maxDevIndex, endIdx);
            }
        }
    }

    public void findStraightSegments(PixelBuffer buffer) {
        int pixelsCount = buffer.getPixelsCount();

        buffer.mCorners = new int[pixelsCount];
        subdivide(buffer, 0, pixelsCount - 1);
    }
}
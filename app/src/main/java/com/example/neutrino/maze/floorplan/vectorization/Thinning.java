package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Color;
import android.graphics.Point;

import java.util.List;

/**
 * Created by Greg Stein on 12/13/2016.
 */
public class Thinning {
    private static final int NEIGHBOURS_TO_CHECK = 9;

    // Indices:                                        0   1   2   3   4   5   6   7   8
    //                                                P2  P3  P4  P5  P6  P7  P8  P9  P2
    private static final int[] NEIGHBOURS_X_OFFSET = { 0,  1,  1,  1,  0, -1, -1, -1,  0};
    private static final int[] NEIGHBOURS_Y_OFFSET = {-1, -1,  0,  1, -1,  1,  0, -1, -1};

    // These are return values from getNeighboursAndTransitions()
    private static int neighbours;
    private static int transitions;
    private static boolean hasThreeConsequentEvenBlackNeighbours; // :D

    public static ImageArray doZhangSuenThinning(ImageArray binaryImage) {
        boolean changeOccurred;

        do {
            changeOccurred = doZhangSuenStep(binaryImage, true); // Step 1
            changeOccurred |= doZhangSuenStep(binaryImage, false); // Step 2
        } while (changeOccurred);

        return binaryImage;
    }

    private static boolean doZhangSuenStep(ImageArray binaryImage, boolean isFirstStep) {
        PixelBufferChunk pixelsToRemove = new PixelBufferChunk(binaryImage.blackPixelsNum);
        boolean changeOccurred = false;

        List<PixelBufferChunk> chunks = binaryImage.pixelBufferChunks;
        for (PixelBufferChunk chunk : chunks) {
            chunk.reset();
            for (Point point = new Point(-1, -1); point.x != 0 || point.y != 0; chunk.getPixel(point)) {
                if (point.x == -1 && point.y == -1) continue; // removed pixel?

                getNeighboursAndTransitions(binaryImage, point, isFirstStep);
                if (neighbours < 2 || neighbours > 6) continue;
                if (transitions != 1) continue;
                if (hasThreeConsequentEvenBlackNeighbours) continue;

                chunk.removePixel(); // mark it with (-1, -1)
                pixelsToRemove.putPixel(point.x, point.y);
                changeOccurred = true;
            }
        }

        Point p = new Point(-1, -1);
        while (p.x != 0 || p.y != 0) {
            pixelsToRemove.getPixel(p);
            binaryImage.set(p, Color.WHITE);
        }

        return changeOccurred;
    }

    private static void getNeighboursAndTransitions(ImageArray image, Point p, boolean isFirstStep) {
        neighbours = 0;
        transitions = 0;
        hasThreeConsequentEvenBlackNeighbours = false;
        int whiteEvenNeighbourIndex = -1;
        int whiteEvenNeighbours = 0;
        for (int i = 0; i < NEIGHBOURS_TO_CHECK - 1; i++) {
            final int neighbourX = p.x + NEIGHBOURS_X_OFFSET[i];
            final int neighbourY = p.y + NEIGHBOURS_Y_OFFSET[i];
            if (image.get(neighbourX, neighbourY) == Color.BLACK) {
               neighbours++;
            } else {
                final int nextNeighbourX = p.x + NEIGHBOURS_X_OFFSET[i + 1];
                final int nextNeighbourY = p.y + NEIGHBOURS_Y_OFFSET[i + 1];
                if (image.get(nextNeighbourX, nextNeighbourY) == Color.BLACK) {
                    transitions++;
                }
                if (i % 2 == 0) { // even Neighbour? P2/4/6/8??
                    whiteEvenNeighbours++;
                    whiteEvenNeighbourIndex = i;
                }
            }
        }
        if (isFirstStep) {
            hasThreeConsequentEvenBlackNeighbours = (whiteEvenNeighbours == 0 ||
                    (whiteEvenNeighbours == 1 && (whiteEvenNeighbourIndex == 0 || whiteEvenNeighbourIndex == 6))); // P2 || P8
        } else {
            hasThreeConsequentEvenBlackNeighbours = (whiteEvenNeighbours == 0 ||
                    (whiteEvenNeighbours == 1 && (whiteEvenNeighbourIndex == 2 || whiteEvenNeighbourIndex == 4))); // P4 || P6
        }
    }
}

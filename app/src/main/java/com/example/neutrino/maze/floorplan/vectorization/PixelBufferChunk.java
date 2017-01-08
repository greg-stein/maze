package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Point;

/**
 * Created by Dima Ruinskiy on 08-01-17.
 */
public class PixelBufferChunk {
    public static final int END_OF_CHUNK = 0;
    public static final int REMOVED_PIXEL = -1;

    public final int[] coords;
    public int pixelsCount = 0;
    public final int size;
    public int position;

    public PixelBufferChunk(int size) {
        coords = new int[2 * (size + 1)]; // two coords. Last pair is always 0
        this.size = size;
        position = -2;
    }

    // Achtung! this method doesn't check boundaries!
    public void putPixel(int x, int y) {
        coords[pixelsCount] = x;
        coords[pixelsCount + 1] = y;
        pixelsCount += 2;
    }

    // If this func returns (0, 0) - it's the end of an array!!!
    public void getPixel(Point p) {
        position += 2;
        p.x = coords[position];
        p.y = coords[position + 1];
    }

    public void removePixel() {
        coords[position] = REMOVED_PIXEL;
        coords[position + 1] = REMOVED_PIXEL;
    }

    public void compact() {
        int newpos = position = 0;
        int x, y;
        for (;;) {
            x = coords[position];
            y = coords[position + 1];
            if (x == -1 && y == -1) {
                position += 2;
            } else {
                coords[newpos] = x;
                coords[newpos + 1] = y;
                position += 2;
                newpos += 2;
                if (x == 0 && y == 0) {
                    reset();
                    return;
                }
            }
        }
    }

    public void reset() {
        position = -2;
    }
}

package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Point;

import java.util.Iterator;

/**
 * Created by Dima Ruinskiy on 08-01-17.
 */
public class PixelBufferChunk implements Iterable<Point> {
    public static final int END_OF_CHUNK = 0;
    public static final int REMOVED_PIXEL = -1;

    public final int[] coords;
    public int coordsCount = 0;
    public int pixelsCount = 0;
    public final int size;
    public int position;
    public int removedPixelsNum = 0;

    public PixelBufferChunk(int size) {
        coords = new int[2 * (size + 1)]; // two coords. Last pair is always 0
        this.size = size;
        position = 0;
    }

    // Achtung! this method doesn't check boundaries!
    public void putPixel(int x, int y) {
        coords[coordsCount] = x;
        coords[coordsCount + 1] = y;
        coordsCount += 2;
        pixelsCount++;
    }

    // If this func returns (0, 0) - it's the end of an array!!!
    public void getPixel(Point p) {
        p.x = coords[position];
        p.y = coords[position + 1];
        position += 2;
    }

    public void removePixel() {
        coords[position - 2] = REMOVED_PIXEL;
        coords[position - 1] = REMOVED_PIXEL;
        removedPixelsNum++; // TODO: What if we remove already removed pixel?
    }

    public boolean endReached() {
        return coords[position] == 0 && coords[position + 1] == 0;
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
        position = 0;
    }

    @Override
    public Iterator<Point> iterator() {
        return new PixelBufferChunkIterator();
    }

    private class PixelBufferChunkIterator implements Iterator<Point> {

        final Point current = new Point();

        public PixelBufferChunkIterator() {
            PixelBufferChunk.this.reset();
        }

        @Override
        public boolean hasNext() {
            do {
                PixelBufferChunk.this.getPixel(current);
                // Skip removed pixel
                if (current.x == REMOVED_PIXEL && current.y == REMOVED_PIXEL) {
                    continue;
                } else {
                    break;
                }
            } while (current.x != END_OF_CHUNK || current.y != END_OF_CHUNK);

            return (current.x != END_OF_CHUNK || current.y != END_OF_CHUNK);
        }

        @Override
        public Point next() {
            return current;
        }

        @Override
        public void remove() {
            PixelBufferChunk.this.removePixel();
        }
    }
}

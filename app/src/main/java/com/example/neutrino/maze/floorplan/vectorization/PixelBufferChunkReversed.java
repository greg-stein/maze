package com.example.neutrino.maze.floorplan.vectorization;

/**
 * Created by Greg Stein on 1/23/2017.
 */
public class PixelBufferChunkReversed extends PixelBufferChunk {
    public PixelBufferChunkReversed(int size) {
        super(size);
        coordsCount = 2 * (size - 1);
        position = coordsCount;
    }

    // This method doesn't check for boundaries
    @Override
    public void putPixel(int x, int y) {
        coords[coordsCount] = x;
        coords[coordsCount + 1] = y;
        position = coordsCount;
        coordsCount -= 2;
        pixelsCount++;
    }

    @Override
    public void reset() {
        position = coordsCount + 2;
    }

    @Override
    public int getFirstIndex() {
        return coordsCount + 2;
    }
}

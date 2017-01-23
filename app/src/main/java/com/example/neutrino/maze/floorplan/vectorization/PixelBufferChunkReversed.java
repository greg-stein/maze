package com.example.neutrino.maze.floorplan.vectorization;

/**
 * Created by Greg Stein on 1/23/2017.
 */
public class PixelBufferChunkReversed extends PixelBufferChunk {
    public PixelBufferChunkReversed(int size) {
        super(size);
        pixelsCount = 2 * (size - 1);
        position = pixelsCount;
    }

    // This method doesn't check for boundaries
    @Override
    public void putPixel(int x, int y) {
        coords[pixelsCount] = x;
        coords[pixelsCount + 1] = y;
        position = pixelsCount;
        pixelsCount -= 2;
    }

    @Override
    public void reset() {
        position = pixelsCount + 2;
    }
}

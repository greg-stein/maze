package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by Greg Stein on 1/18/2017.
 */
public class PixelBuffer extends ArrayList<PixelBufferChunk> {
    public static final int BUFFER_CHUNK_DEFAULT_SIZE = 256;

    private PixelBufferChunk mLastChunk;
    private PixelBufferChunk mFirstChunk = null;
    private final int mBufferChunkSize;

    public PixelBuffer() {
        this(BUFFER_CHUNK_DEFAULT_SIZE);
    }

    public PixelBuffer(int bufferChunkSize) {
        mBufferChunkSize = bufferChunkSize;
        addChunk();
    }

    public void putPixel(Point p) {
        if (mLastChunk.pixelsCount >= mBufferChunkSize) {
            addChunk();
        }
        mLastChunk.putPixel(p.x, p.y);
    }

    public void prepareForPushingBack() {
        if (mFirstChunk == null) {
            pushBackChunk();
        }
    }

    public void pushBackPixel(Point p) {
        if (mFirstChunk.pixelsCount >= mBufferChunkSize) {
            pushBackChunk();
        }
        mFirstChunk.putPixel(p.x, p.y);
    }

    private void pushBackChunk() {
        mFirstChunk = new PixelBufferChunkReversed(mBufferChunkSize);
        this.add(0, mFirstChunk);
    }

    private void addChunk() {
        mLastChunk = new PixelBufferChunk(mBufferChunkSize);
        this.add(mLastChunk);
    }
}

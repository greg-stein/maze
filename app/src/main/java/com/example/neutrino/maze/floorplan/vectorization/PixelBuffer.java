package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by Greg Stein on 1/18/2017.
 */
public class PixelBuffer extends ArrayList<PixelBufferChunk> {
    public static final int BUFFER_CHUNK_DEFAULT_SIZE = 256;
    public static final int CORNER = 1;

    private PixelBufferChunk mLastChunk;
    private PixelBufferChunk mFirstChunk = null;
    private final int mBufferChunkSize;
    private int mPixelsCount = 0;

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
        mPixelsCount++;
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
        mPixelsCount++;
    }

    public int getPixelsCount() { return mPixelsCount; }

    private void pushBackChunk() {
        mFirstChunk = new PixelBufferChunkReversed(mBufferChunkSize);
        this.add(0, mFirstChunk);
    }

    private void addChunk() {
        mLastChunk = new PixelBufferChunk(mBufferChunkSize);
        this.add(mLastChunk);
    }

    // For each pixels this array stores 1 if corresponding pixel is corner and 0 otherwise
    public int[] mCorners;
    private int mFirstChunkPixelsNum;
    private int mLastChunkPixelsNum;
    private int mInnerChunksNum;
    private int mPixelsUpToLastChunk;
    public void initDirectIndexing() {
        mFirstChunkPixelsNum = get(0).pixelsCount;
        mLastChunkPixelsNum = get(size()-1).pixelsCount;
        mInnerChunksNum = Math.max(0, size() - 2);
        mPixelsUpToLastChunk = mFirstChunkPixelsNum + mInnerChunksNum * mBufferChunkSize;
    }

    private int mIndexWithinChunk;
    private int mChunkIndex;
    private void toLocalIndex(int index) {
        if (index < mFirstChunkPixelsNum) { // first chunk?
            mChunkIndex = 0;
            mIndexWithinChunk = index;
        } else if (index > mPixelsUpToLastChunk) { // last chunk?
            mChunkIndex = size() - 1;
            mIndexWithinChunk = index - mPixelsUpToLastChunk;
        } else { // in inner chunk
            final int innerIndex = index - mFirstChunkPixelsNum;
            mChunkIndex = innerIndex / mBufferChunkSize + 1;
            mIndexWithinChunk = innerIndex % mBufferChunkSize;
        }
    }

    // Gets point using given global index. Point must be initialized
    public void get(int index, Point p) {
        toLocalIndex(index);
        PixelBufferChunk chunk = get(mChunkIndex);
        mIndexWithinChunk <<= 1;
        mIndexWithinChunk += chunk.getFirstIndex();
        p.set(chunk.coords[mIndexWithinChunk], chunk.coords[mIndexWithinChunk + 1]);
    }
}

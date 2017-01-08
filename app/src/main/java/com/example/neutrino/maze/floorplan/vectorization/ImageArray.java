package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 12/15/2016.
 */
public class ImageArray {
    public static final int PIXEL_BUFFER_CHUNK_SIZE = 4096; // 4K=

    public final int[] dataArray;
    public final int width;
    public final int height;
    public final int dataLength;
    public final List<PixelBufferChunk> pixelBufferChunks = new ArrayList<>();
    public int blackPixelsNum = 0;

    public ImageArray(int width, int height) {
        this(new int[width * height], width, height);
    }

    public ImageArray(int[] dataArray, int width, int height) {
        this.dataArray = dataArray;
        this.dataLength = dataArray.length;
        this.width = width;
        this.height = height;
    }

    public ImageArray(Bitmap image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.dataArray = new int[width * height];
        image.getPixels(dataArray, 0, width, 0, 0, width, height);
        this.dataLength = dataArray.length;
    }

    public int get(int x, int y) {
        return dataArray[y * width + x];
    }

    public void set(Point p, int value) {
        dataArray[p.y * width + p.x] = value;
    }

    public void set(int x, int y, int value) {
        dataArray[y * width + x] = value;
    }

    public void accumulate(int x, int y, int delta) {
        dataArray[y * width + x] += delta;
    }

    public int getMax() {
        int max = dataArray[0];
        for (int i = width * height - 1; i > 0; i--)
            if (dataArray[i] > max)
                max = dataArray[i];
        return max;
    }

    public Bitmap toBitmap() {
        return Bitmap.createBitmap(dataArray, width, height, Bitmap.Config.ARGB_8888);
    }

    public void findBlackPixels() {
        PixelBufferChunk currentChunk = null;
        int pointsInCurrentChunk = PIXEL_BUFFER_CHUNK_SIZE; // create new chunk in first iteration

        // we skip point (0, 0) as it indicates end of array
        for (int i = 1; i < dataLength; i++) {
            if (dataArray[i] == Color.BLACK) {
                blackPixelsNum++;
                final int x = i % width;
                final int y = i / width;
                if (pointsInCurrentChunk >= PIXEL_BUFFER_CHUNK_SIZE) {
                    currentChunk = new PixelBufferChunk(PIXEL_BUFFER_CHUNK_SIZE);
                    pixelBufferChunks.add(currentChunk);
                    pointsInCurrentChunk = 0;
                }
                currentChunk.putPixel(x, y);
                pointsInCurrentChunk++;
            }
        }
    }
}
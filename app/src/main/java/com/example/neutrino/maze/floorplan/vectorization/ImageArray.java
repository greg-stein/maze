package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Bitmap;

/**
 * Created by Greg Stein on 12/15/2016.
 */
public class ImageArray {
    public final int[] dataArray;
    public final int width;
    public final int height;
    public final int dataLength;

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
}
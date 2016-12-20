package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.io.IOException;

/**
 * Created by Greg Stein on 12/13/2016.
 */
public class FloorplanVectorizer {
    public static Bitmap debugBM;

    public static Iterable<CollinearSegments.ILineSegment> vectorize(Bitmap image) {
        Bitmap scaledImage = getResizedBitmap(image, image.getWidth()/2, image.getHeight()/2);
        image.recycle();

        Bitmap grayImage = toGrayscale(scaledImage);
        scaledImage.recycle();

        ImageArray imageArray = new ImageArray(grayImage);
        grayImage.recycle();

        binarize(imageArray, calcOtsuThreshold(imageArray));
        imageArray.findBlackPixels(); // this updates internal multiarray with black pixels

        Thinning.doZhangSuenThinning(imageArray);
        debugBM = imageArray.toBitmap();


//        HoughTransform houghTransform = new HoughTransform(imageArray);
//        houghTransform.buildHoughSpace();
//        List<CollinearSegments.ILineSegment> lines = houghTransform.getLines(50);
//
//        return lines;
        return null;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    private static Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);

//        float[] mat = new float[]{
//                0.3f, 0.59f, 0.11f, 0, 0,
//                0.3f, 0.59f, 0.11f, 0, 0,
//                0.3f, 0.59f, 0.11f, 0, 0,
//                0, 0, 0, 1, 0,};
//        ColorMatrixColorFilter f = new ColorMatrixColorFilter(mat);

        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private static int calcOtsuThreshold(ImageArray grayScaledImage) {
        final int GRAY_LEVELS = 256;

        int[] histogram = new int[GRAY_LEVELS];
        final int width = grayScaledImage.width;
        final int height = grayScaledImage.height;

        // Calculate histogram
        for (int index = 0; index < grayScaledImage.dataLength; index ++) {
            final int h = grayScaledImage.dataArray[index] & 0xFF;
            histogram[h]++;
        }

        int total = grayScaledImage.dataLength;

        float sum = 0;
        for (int t = 0 ; t < GRAY_LEVELS; t++)
            sum += t * histogram[t];

        float sumB = 0;
        int weightBackground = 0;
        int weightForeground = 0;

        float varianceMax = 0;
        int threshold = 0;

        for (int t = 0 ; t < GRAY_LEVELS; t++) {
            weightBackground += histogram[t];
            if (weightBackground == 0) continue;

            weightForeground = total - weightBackground;
            if (weightForeground == 0) break;

            sumB += t * histogram[t];

            float meanBackground = sumB / weightBackground;
            float meanForeground = (sum - sumB) / weightForeground;
            float meanDiff = meanBackground - meanForeground;

            // Calculate Between Class Variance
            float varianceBetween = weightBackground * weightForeground * meanDiff * meanDiff;

            // Check if new maximum found
            if (varianceBetween > varianceMax) {
                varianceMax = varianceBetween;
                threshold = t;
            }
        }

        return threshold;
    }

    private static void binarize(ImageArray grayScaledImage, int threshold) {
        for (int index = 0; index < grayScaledImage.dataLength; index ++) {
            final int grayValue = grayScaledImage.dataArray[index] & 0xFF;
            grayScaledImage.dataArray[index] = (grayValue > threshold)? Color.WHITE : Color.BLACK;
        }
    }
}

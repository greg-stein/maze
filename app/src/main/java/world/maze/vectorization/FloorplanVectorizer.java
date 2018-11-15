package world.maze.vectorization;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;

import world.maze.floorplan.IFloorPlanPrimitive;
import world.maze.rendering.VectorHelper;
import world.maze.vectorization.HoughTransform.LineSegment;
import world.maze.floorplan.Wall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Greg Stein on 12/13/2016.
 */
public class FloorplanVectorizer {
    public static final int PADDING = 1;
    public static final float MIN_CONNECT_DISTANCE = 0.5f;
    private static final float MIN_CONNECT_SQ_DISTANCE = MIN_CONNECT_DISTANCE * MIN_CONNECT_DISTANCE;
    private static final int GRAY_LEVELS = 256;

    public static Bitmap debugBM;

    public static List<IFloorPlanPrimitive> vectorize(Bitmap image) {
        if (image == null) return null;
//        Bitmap scaledImage = getResizedBitmap(image, image.getWidth()/2, image.getHeight()/2);
//        image.recycle();

        Bitmap grayImage = toGrayscale(image, PADDING);
        image.recycle();

        ImageArray imageArray = new ImageArray(grayImage);
        grayImage.recycle();

        binarize(imageArray, calcOtsuThreshold(imageArray));
        imageArray.findBlackPixels(); // this updates internal multiarray with black pixels

        Thinning.doZhangSuenThinning(imageArray);
        debugBM = imageArray.toBitmap();

        LineSegmentsRecognizer kht = new LineSegmentsRecognizer(imageArray);
        List<LineSegment> lineSegments = kht.findStraightSegments();
        List<IFloorPlanPrimitive> walls = translateToWalls(lineSegments);

        return walls;
    }

    public static List<IFloorPlanPrimitive> translateToWalls(Collection<LineSegment> lines) {
        List<IFloorPlanPrimitive> walls = new ArrayList<>(lines.size());
        // TODO: ACHTUNG!! This is only for tests! Scale factor should be set by user!!!
        // TODO: And not in this stage, but later when floorplan is added
        float s = 21.0f/1433; // scale factor

        for (HoughTransform.LineSegment segment : lines) {
            Wall wall = new Wall(s * segment.start.x, s * segment.start.y,
                    s * segment.end.x, s * segment.end.y); // Width for debug: , 0.01f
            walls.add(wall);
        }

        return walls;
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

    public static Bitmap toGrayscale(Bitmap bmpOriginal, int padding)
    {
        int width = bmpOriginal.getWidth() + 2 * padding;
        int height = bmpOriginal.getHeight() + 2 * padding;

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
        c.drawColor(Color.WHITE);
        c.drawBitmap(bmpOriginal, padding, padding, paint);
        return bmpGrayscale;
    }

    public static int calcOtsuThreshold(Bitmap grayScaledImage) {
        int[] histogram = new int[GRAY_LEVELS];
        int height = grayScaledImage.getHeight();
        int width = grayScaledImage.getWidth();

        for(int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                final int h = grayScaledImage.getPixel(x, y) & 0xFF;
                histogram[h]++;
            }
        }

        int threshold = getThreshold(histogram, width * height);

        return threshold;
    }

    public static int calcOtsuThreshold(ImageArray grayScaledImage) {
        int[] histogram = new int[GRAY_LEVELS];

        // Calculate histogram
        for (int index = 0; index < grayScaledImage.dataLength; index ++) {
            final int h = grayScaledImage.dataArray[index] & 0xFF;
            histogram[h]++;
        }

        int threshold = getThreshold(histogram, grayScaledImage.dataLength);

        return threshold;
    }

    private static int getThreshold(int[] histogram, int total) {
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

    public static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    public static Bitmap toBinary(Bitmap bitmap, int threshold, AsyncTask<Void, Void, Void> binarizeTask) {
        int width, height;
        height = bitmap.getHeight();
        width = bitmap.getWidth();
        Bitmap bmpBinary = Bitmap.createBitmap(bitmap);

        for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
                // get one pixel color
                int pixel = bitmap.getPixel(x, y);

                //get binary value
                if((pixel&0x000000FF) < threshold){
                    bmpBinary.setPixel(x, y, 0xFF000000);
                } else{
                    bmpBinary.setPixel(x, y, 0xFFFFFFFF);
                }
            }
            if (binarizeTask!= null && binarizeTask.isCancelled()) return null;
        }
        return bmpBinary;
    }

    public static void binarize(ImageArray grayScaledImage, int threshold) {
        for (int index = 0; index < grayScaledImage.dataLength; index ++) {
            final int grayValue = grayScaledImage.dataArray[index] & 0xFF;
            grayScaledImage.dataArray[index] = (grayValue > threshold)? Color.WHITE : Color.BLACK;
        }
    }

    public static List<Wall> connect(List<Wall> walls) {
        for (Wall wall : walls) {
            for (Wall anotherWall : walls) {
                if (wall != anotherWall) {
                    if (VectorHelper.squareDistance(wall.getStart(), anotherWall.getStart()) < MIN_CONNECT_SQ_DISTANCE) {
                        wall.setStart(anotherWall.getStart()); // consider also maintaining a pointer: wall.connectedAtA.add(anotherWall)
                    } else if (VectorHelper.squareDistance(wall.getStart(), anotherWall.getEnd()) < MIN_CONNECT_SQ_DISTANCE) {
                        wall.setStart(anotherWall.getEnd());
                    } else if (VectorHelper.squareDistance(wall.getEnd(), anotherWall.getStart()) < MIN_CONNECT_SQ_DISTANCE) {
                        wall.setEnd(anotherWall.getStart());
                    } else if (VectorHelper.squareDistance(wall.getEnd(), anotherWall.getEnd()) < MIN_CONNECT_SQ_DISTANCE) {
                        wall.setEnd(anotherWall.getEnd());
                    }
                }
            }
        }

        return walls;
    }
}
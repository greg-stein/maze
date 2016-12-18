package com.example.neutrino.maze.floorplan.vectorization;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Greg Stein on 12/12/2016.
 * <p/>
 * Java Implementation of the Hough Transform.<br />
 * Used for finding straight lines in an mImage.<br />
 * </p>
 * <p/>
 * Note: This class is based on original code from:<br />
 * <a href="http://homepages.inf.ed.ac.uk/rbf/HIPR2/hough.htm">http://homepages.inf.ed.ac.uk/rbf/HIPR2/hough.htm</a>
 * </p>
 * @author Olly Oechsle, University of Essex
 */
public class HoughTransform {
    // The size of the neighbourhood in which to search for other local maxima
    final static int HOUGH_NEIGHBOURHOOD_SIZE = 4;
    final static int MAX_POINT_LINE_DISTANCE = 2;
    // How many discrete values of theta shall we check?
    public static final int MAX_THETA = 360;
    // Using MAX_THETA, work out the step
    public static final double THETA_STEP = Math.PI / MAX_THETA;

    public class HoughBin extends ArrayList<Point>{};

    // the mImageWidth and mImageHeight of the mImage
    private int mImageWidth, mImageHeight;

    // the hough array
    private int[][] mHoughArray;

    // the coordinates of the centre of the mImage
    private float mCenterX, mCenterY;
    // the height of the hough array
    private int mHoughHeight;
    // double the hough mImageHeight (allows for negative numbers)
    private int mDoubleHeight;
    // the number of points that have been added
    private int mNumPoints;

    private HoughBin[][] mBins;
    private ImageArray mImage;

    // cache of values of sin and cos for different theta values. Has a significant performance improvement.
    private static double[] sinCache;
    private static double[] cosCache;

    static {
        // cache the values of sin and cos for faster processing
        sinCache = new double[MAX_THETA];
        cosCache = sinCache.clone();
        for (int t = 0; t < MAX_THETA; t++) {
            double realTheta = t * THETA_STEP;
            sinCache[t] = Math.sin(realTheta);
            cosCache[t] = Math.cos(realTheta);
        }
    }

    public HoughTransform(ImageArray image) {
        mImageWidth = image.width;
        mImageHeight = image.height;
        mImage = image;

        // Calculate the maximum mImageHeight the hough array needs to have
        mHoughHeight = (int) (Math.sqrt(2) * Math.max(mImageWidth, mImageHeight)) / 2;

        // Double the mImageHeight of the hough array to cope with negative r values
        mDoubleHeight = 2 * mHoughHeight;

        // Create the hough array
        mHoughArray = new int[MAX_THETA][mDoubleHeight];

        // Find edge points and vote in array
        mCenterX = mImageWidth / 2;
        mCenterY = mImageHeight / 2;

        // Count how many points there are
        mNumPoints = 0;

        mBins = new HoughBin[MAX_THETA][mDoubleHeight];
    }

    /**
     * Adds points from an mImage. The mImage is assumed to be binary black and white.
     * black = foreground, white = background
     */
    public void buildHoughSpace() {
        // Now find edge points and update the hough array
        for (int x = 0; x < mImageWidth; x++) {
            for (int y = 0; y < mImageHeight; y++) {
                // Find non-black pixels
                if ((mImage.get(x, y) & 0x000000ff) != 0x000000ff) {
                    addPoint(x, y);
                }
            }
        }
    }

    /**
     * Adds a single point to the hough transform. You can use this method directly
     * if your data isn't represented as a buffered image.
     */
    public void addPoint(int x, int y) {

        // Go through each value of theta
        for (int t = 0; t < MAX_THETA; t++) {

            //Work out the r values for each theta step
            int r = (int) (((x - mCenterX) * cosCache[t]) + ((y - mCenterY) * sinCache[t]));

            // this copes with negative values of r
            r += mHoughHeight;

            if (r < 0 || r >= mDoubleHeight) continue;

            // Increment the hough array
            mHoughArray[t][r]++;

            if (mBins[t][r] == null) mBins[t][r] = new HoughBin();
            mBins[t][r].add(new Point(x, y));
        }

        mNumPoints++;
    }

    /**
     * Once points have been added in some way this method extracts the lines and returns them as a Vector
     * of ILineSegment objects.
     *
     * @param threshold The  threshold above which lines are determined from the Hough array
     */
    public List<CollinearSegments.ILineSegment> getLines(int threshold) {
        List<CollinearSegments.ILineSegment> lineSegments = new ArrayList<>(20);
        Set<Point> points = new HashSet<>();

        // Only proceed if the hough array is not empty
        if (mNumPoints == 0) return lineSegments;

        // Search for local peaks above threshold to draw
        for (int t = 0; t < MAX_THETA; t++) {
            loop:
            for (int r = HOUGH_NEIGHBOURHOOD_SIZE; r < mDoubleHeight - HOUGH_NEIGHBOURHOOD_SIZE; r++) {

                // Only consider points above threshold
                if (mHoughArray[t][r] > threshold) {

                    int peak = mHoughArray[t][r];

                    points.clear();
                    points.addAll(mBins[t][r]);

                    // Check that this peak is indeed the local maxima
                    for (int dx = -HOUGH_NEIGHBOURHOOD_SIZE; dx <= HOUGH_NEIGHBOURHOOD_SIZE; dx++) {
                        for (int dy = -HOUGH_NEIGHBOURHOOD_SIZE; dy <= HOUGH_NEIGHBOURHOOD_SIZE; dy++) {
                            int dt = t + dx;
                            int dr = r + dy;
                            if (dt < 0) dt = dt + MAX_THETA;
                            else if (dt >= MAX_THETA) dt = dt - MAX_THETA;
                            if (mHoughArray[dt][dr] > peak) {
                                // found a bigger point nearby, skip
                                continue loop;
                            }

                            // Take values from same column only
                            if ((dt == t) && mBins[dt][dr] != null) {
                                points.addAll(mBins[dt][dr]);
                            }
                        }
                    }

                    // calculate the true value of theta
                    double theta = t * THETA_STEP;
                    eliminateOutliers(r, t, points);
                    lineSegments.addAll(CollinearSegments.findCollinearSegments(r, theta, points));
                }
            }
        }

        return lineSegments;
    }

    // Removes points that do not lie on line given by normal (r, theta)
    private void eliminateOutliers(int r, int theta, Iterable<Point> points) {
        Iterator<Point> i = points.iterator();

        while (i.hasNext()) {
            Point p = i.next();
            int d = Math.abs((int)((p.x-mCenterX) * cosCache[theta] + (p.y-mCenterY) * sinCache[theta] - r + mHoughHeight));

            if (d > MAX_POINT_LINE_DISTANCE) {
                i.remove();
            }
        }
    }

    /**
     * Gets the highest value in the hough array
     */
    public int getHighestValue() {
        int max = 0;
        for (int t = 0; t < MAX_THETA; t++) {
            for (int r = 0; r < mDoubleHeight; r++) {
                if (mHoughArray[t][r] > max) {
                    max = mHoughArray[t][r];
                }
            }
        }
        return max;
    }

    /**
     * Gets the hough array as an mImage, in case you want to have a look at it.
     */
    public Bitmap getHoughArrayImage() {
        int max = getHighestValue();
        Bitmap image = Bitmap.createBitmap(MAX_THETA, mDoubleHeight, Bitmap.Config.ARGB_8888);
        for (int t = 0; t < MAX_THETA; t++) {
            for (int r = 0; r < mDoubleHeight; r++) {
                double value = 255 * ((double) mHoughArray[t][r]) / max;
                int v = 255 - (int) value;
                int c = Color.argb(0, v, v, v);
                image.setPixel(t, r, c);
            }
        }
        return image;
    }

}


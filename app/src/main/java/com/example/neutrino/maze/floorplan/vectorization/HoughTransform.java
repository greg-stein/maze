package com.example.neutrino.maze.floorplan.vectorization;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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

    public static final Comparator<Point> X_COMPARATOR = new Comparator<Point>() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        public int compare(Point p1, Point p2) {
            int compare = Integer.compare(p1.x, p2.x);
            // Make compare method consistent with equals which is NOT used by treeset
            if (compare == 0 && !p1.equals(p2)) return Integer.compare(p1.y, p2.y);
            return compare;
        }
    };

    public static final Comparator<Point> Y_COMPARATOR = new Comparator<Point>() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        public int compare(Point p1, Point p2) {
            int compare = Integer.compare(p1.y, p2.y);
            // Make compare method consistent with equals which is NOT used by treeset
            if (compare == 0 && !p1.equals(p2)) return Integer.compare(p1.x, p2.x);
            return compare;
        }
    };

    public static class HoughLine {
        public int rho;
        public int theta;

        public HoughLine(int rho, int theta) {
            this.rho = rho;
            this.theta = theta;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof HoughLine)) return false;
            HoughLine other = (HoughLine) o;
            return other.rho == this.rho && other.theta == this.theta;
        }

        @Override
        public int hashCode() {
            return (rho * 71) ^ theta;
        }
    }

    public static class LineSegment implements Comparable<LineSegment> {
        private HoughLine line;
        public Point start;
        public Point end;

        public LineSegment(Point start, Point end) {
            this.start = start;
            this.end = end;
        }

        public LineSegment(Point start, Point end, HoughLine line) {
            this(start, end);
            this.line = line;
        }

        @Override
        public int compareTo(LineSegment another) {

            if (this.start.x < another.start.x)
                return -1;
            if (this.start.x > another.start.x)
                return 1;
            if (this.end.x < another.end.x)
                return -1;
            if (this.end.x > another.end.x)
                return 1;

            if (this.start.y < another.start.y)
                return -1;
            if (this.start.y > another.start.y)
                return 1;
            if (this.end.y < another.end.y)
                return -1;
            if (this.end.y > another.end.y)
                return 1;

            return 0;
        }

        public boolean equals (LineSegment another) {
            return (this.compareTo(another) == 0);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LineSegment)) return false;
            return equals((LineSegment)o);
        }
    }

    // The size of the neighbourhood in which to search for other local maxima
    final static int HOUGH_NEIGHBOURHOOD_SIZE = 4;
    final static int MAX_POINT_LINE_DISTANCE = 2;
    // How many discrete values of theta shall we check?
    public static final int MAX_THETA = 360;
    // Using MAX_THETA, work out the step
    public static final double THETA_STEP = Math.PI / MAX_THETA;
    private static final int INIT_LINES_NUMBER = 200;
    public static final int CONTINUOUS_LINE_MAX_GAP = 4;
    public static final int MIN_LINE_SEGMENT_LENGTH = 10;
    public static final int MIN_DIFF_BETWEEN_SEGMENTS = 4;
    private static final int CONTINUOUS_LINE_MAX_GAP_SQ =
            CONTINUOUS_LINE_MAX_GAP * CONTINUOUS_LINE_MAX_GAP; // actually for comparing squared distance
    private static final int MIN_LINE_SEGMENT_LENGTH_SQ =
            MIN_LINE_SEGMENT_LENGTH * MIN_LINE_SEGMENT_LENGTH; // and this is a square of minimal length!
    private static final int MIN_DIFF_BETWEEN_SEGMENTS_SQ = MIN_DIFF_BETWEEN_SEGMENTS * MIN_DIFF_BETWEEN_SEGMENTS;

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
    }

    /**
     * Adds points from an mImage. The mImage is assumed to be binary black and white.
     * black = foreground, white = background
     */
    public void buildHoughSpace() {
        // Now find edge points and update the hough array

        List<PixelBufferChunk> chunks = mImage.pixelBufferChunks;
        for (PixelBufferChunk chunk : chunks) {
            chunk.reset();
            for (Point p = new Point(-1, -1); p.x != 0 || p.y != 0; chunk.getPixel(p)) {
                if (p.x != -1 && p.y != -1) { // removed pixel?
                    addPoint(p.x, p.y);
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
        }

        mNumPoints++;
    }

    /**
     * Once points have been added in some way this method extracts the lines and returns them as a Vector
     * of ILineSegment objects.
     *
     * @param threshold The  threshold above which lines are determined from the Hough space
     */
    public List<LineSegment> getLineSegments(int threshold) {
//        Set<Point> points = new HashSet<>();
        List<HoughLine> foundLines = new ArrayList<>(INIT_LINES_NUMBER);
        List<LineSegment> lineSegments = new ArrayList<>(50);

        // Only proceed if the hough array is not empty
        if (mNumPoints == 0) return lineSegments;

        // Search for local peaks above threshold to draw
        for (int t = 0; t < MAX_THETA; t++) {
            loop:
            for (int r = HOUGH_NEIGHBOURHOOD_SIZE; r < mDoubleHeight - HOUGH_NEIGHBOURHOOD_SIZE; r++) {

                // Only consider points above threshold
                if (mHoughArray[t][r] > threshold) {

                    int peak = mHoughArray[t][r];

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
                        }
                    }

                    foundLines.add(new HoughLine(r, t));
                }
            }
        }

        Map<HoughLine, SortedSet<Point>> linesToPointsMap;
        linesToPointsMap = clusterize(mImage.pixelBufferChunks, foundLines);
        for (Map.Entry<HoughLine, SortedSet<Point>> linePoints : linesToPointsMap.entrySet()) {
            final HoughLine line = linePoints.getKey();
            final SortedSet<Point> points = linePoints.getValue();
            eliminateOutliers(line.rho, line.theta, points);
            recognizeSegments(line, points, lineSegments);
        }

        List<LineSegment> mergedSegments = mergeSegments (lineSegments);

        return mergedSegments;
    }

    public static List<LineSegment> mergeSegments(List<LineSegment> lineSegments) {

        HashMap<HoughLine, SortedSet<LineSegment>> segmentsByLine = new HashMap<>();

        // Organize all segments by their HoughLine (rho,theta) in a sorted set
        for (LineSegment s: lineSegments) {
            if (segmentsByLine.get(s.line) == null) {
                segmentsByLine.put(s.line, new TreeSet<LineSegment>());
            }
            segmentsByLine.get(s.line).add(s);
        }

        List<LineSegment> mergedSegments = new ArrayList<>();

        for (SortedSet<LineSegment> set: segmentsByLine.values()) { // Merge for this (rho,theta)
            List<LineSegment> mergedList = mergeSegmentsSameLine(set);
            mergedSegments.addAll(mergedList);
        }

        return mergedSegments;
    }

    public static List<LineSegment> mergeSegmentsSameLine(SortedSet<LineSegment> set) {
        List<LineSegment> newlist = new ArrayList<>();

        LineSegment current = set.first();   // Assume there is always a first otherwise why are we even here

        for (LineSegment seg : set) {
            if (X_COMPARATOR.compare(seg.start, current.end) <= 0) {     // starts inside current segment (or at boundary)
                if (X_COMPARATOR.compare(seg.end, current.end) > 0) {   // ends outside current segment
                    current.end = seg.end;                              // extend current segment
                }
            } else {    // Check if segments are distinct but very close
                int dx = Math.abs(seg.start.x - current.end.x);
                int dy = Math.abs(seg.start.y - current.end.y);
                int gap = dx*dx + dy*dy;
                if (gap < MIN_DIFF_BETWEEN_SEGMENTS_SQ) {
                    current.end = seg.end;
                } else {
                    newlist.add(current);   // cannot merge segments - finalize current
                    current = seg;          // now this is the current segment
                }
            }
        }

        newlist.add(current);   // Add the last segment

        return newlist;
    }

    // I love generics
    // TODO: implement Iterable<Point> in PixelBufferChunk
    private Map<HoughLine, SortedSet<Point>> clusterize(List<PixelBufferChunk> points, List<HoughLine> lines) {
        Map<HoughLine, SortedSet<Point>> linesToPointsMap = new HashMap<>();

        for (PixelBufferChunk chunk : points) {
            chunk.reset();
            for (Point p = new Point(-1, -1); p.x != 0 || p.y != 0; chunk.getPixel(p)) {
                if (p.x == -1 && p.y == -1) continue; // removed pixel?

                // Find closest line to the point
                HoughLine closestLine = null;
                for (HoughLine line : lines) {
                    final float x = p.x - mCenterX;
                    final float y = p.y - mCenterY;
                    final int rho = (int) (x * cosCache[line.theta] + y * sinCache[line.theta]) + mHoughHeight;

                    //TODO: throw last bit of this comparison? & 0xFFFE
                    if (line.rho == rho) {
                        closestLine = line;
                    }
                }

                if (closestLine == null) continue; // line of this point wasn't recognized (too short?)
                SortedSet<Point> linePoints = linesToPointsMap.get(closestLine);
                if (linePoints == null) {
                    final double theta = closestLine.theta * THETA_STEP; // real theta
                    if ((3*Math.PI/4 > theta) && (theta > Math.PI/4)) { // horizontal line
                        linePoints = new TreeSet<>(Y_COMPARATOR);
                    } else {                                            // vertical line
                        linePoints = new TreeSet<>(X_COMPARATOR);
                    }
                    linesToPointsMap.put(closestLine, linePoints);
                }
                if (!linePoints.add(new Point(p.x, p.y))) { // need clone here, p is changed in the loop
                    throw new RuntimeException("Something fucked up here!");
                }
            }
        }

        return linesToPointsMap;
    }

    private static void createSegment(Point start, Point end, HoughLine line, List<LineSegment> segments) {
        int dx = Math.abs(start.x - end.x);
        int dy = Math.abs(start.y - end.y);
        int segmentLen = dx*dx + dy*dy; // square len
        if (segmentLen > MIN_LINE_SEGMENT_LENGTH_SQ) {// skip creating short segments
            LineSegment newSegment = new LineSegment(start, end, line);
            segments.add(newSegment);
        }
    }

    // Preconditions:
    //     points should be sorted either by X or Y
    //     segments != null, factory != null, points != null
    private static void recognizeSegments(HoughLine line, SortedSet<Point> points, List<LineSegment> segments) {
        Point segmentStart = points.first();
        Point segmentEnd = points.first();

        for (Point point: points) {
            int gap = (int)(Math.pow(point.x - segmentEnd.x, 2) + Math.pow(point.y - segmentEnd.y, 2));

            if (gap > CONTINUOUS_LINE_MAX_GAP_SQ)  {// new segment detected?
                createSegment(segmentStart, segmentEnd, line, segments);
                segmentStart = point;
                segmentEnd = point;
            } else {
                segmentEnd = point;
            }
        }

        // Add last segment
        createSegment(segmentStart, segmentEnd, line, segments);
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


package com.example.neutrino.maze.floorplan.vectorization;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by Greg Stein on 12/12/2016.
 */
public class CollinearSegments {
    public static final int CONTINUOUS_LINE_MAX_GAP = 4;
    public static final int MIN_LINE_SEGMENT_LENGTH = 10;
    private static final int CONTINUOUS_LINE_MAX_GAP_SQ =
            CONTINUOUS_LINE_MAX_GAP * CONTINUOUS_LINE_MAX_GAP; // actually for comparing squared distance
    private static final int MIN_LINE_SEGMENT_LENGTH_SQ =
            MIN_LINE_SEGMENT_LENGTH * MIN_LINE_SEGMENT_LENGTH; // and this is a square of minimal length!
    public static final double MAX_ORTOGONALITY_FACTOR = 0.05; // this indicates max deviation of found segments to line in terms of angle

    public interface ILineSegment {
        void draw(Bitmap image, int color);
        Point getStart();
        Point getEnd();
        void setStart(Point start);
        void setEnd(Point end);
    }

    public static abstract class LineSegmentBase implements ILineSegment {
        public abstract void draw(Bitmap image, int color);

        protected Point start;
        protected Point end;

        public Point getStart() {return start;}
        public Point getEnd() {return end;}
        public void setStart(Point start) {this.start = start;}
        public void setEnd(Point end) {this.end = end;}

        // Used for drawing in Bresenham algorithm
        protected int dy;
        protected int dx;

        protected int dy2; // slope scaling factors to avoid floating
        protected int dx2; // point

        protected int ix; // increment direction
        protected int iy;

        protected int x1;
        protected int x2;
        protected int y1;
        protected int y2;

        protected int d = 0;

        protected LineSegmentBase(Point start, Point end) {
            this.start = start;
            this.end = end;

            x1 = start.x;
            y1 = start.y;
            x2 = end.x;
            y2 = end.y;

            dy = Math.abs(y2 - y1);
            dx = Math.abs(x2 - x1);

            dy2 = (dy << 1); // slope scaling factors to avoid floating
            dx2 = (dx << 1); // point

            ix = x1 < x2 ? 1 : -1; // increment direction
            iy = y1 < y2 ? 1 : -1;
        }
    }

    public static class VerticalSegment extends LineSegmentBase {
        public VerticalSegment(Point start, Point end) {
            super(start, end);
        }

        public void draw(Bitmap image, int color) {
            while (true) {
                image.setPixel(x1, y1, color);
                if (y1 == y2) break;
                y1 += iy;
                d += dx2;
                if (d > dy) {
                    x1 += ix;
                    d -= dy2;
                }
            }
        }
    }

    public static class HorizontalSegment extends LineSegmentBase {
        public HorizontalSegment(Point start, Point end) {
            super(start, end);
        }

        public void draw(Bitmap image, int color) {
            while (true) {
                image.setPixel(x1, y1, color);
                if (x1 == x2) break;
                x1 += ix;
                d += dy2;
                if (d > dx) {
                    y1 += iy;
                    d -= dx2;
                }
            }
        }
    };

    public interface ILineSegmentAbstractFactory {
        ILineSegment createSegment(Point start, Point end);
    };

    public static final ILineSegmentAbstractFactory VerticalSegmentsFactory = new ILineSegmentAbstractFactory() {
        public ILineSegment createSegment(Point start, Point end) {
            return new VerticalSegment(start, end);
        }
    };

    public static final ILineSegmentAbstractFactory HorizontalSegmentsFactory = new ILineSegmentAbstractFactory() {
        public ILineSegment createSegment(Point start, Point end) {
            return new HorizontalSegment(start, end);
        }
    };

    public static final Comparator<Point> X_COMPARATOR = new Comparator<Point>() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        public int compare(Point p1, Point p2) {
            return Integer.compare(p1.x, p2.x);
        }
    };

    public static final Comparator<Point> Y_COMPARATOR = new Comparator<Point>() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        public int compare(Point p1, Point p2) {
            return Integer.compare(p1.y, p2.y);
        }
    };

    private static void createSegment(Point start, Point end, List<ILineSegment> segments, ILineSegmentAbstractFactory factory) {
        int dx = Math.abs(start.x - end.x);
        int dy = Math.abs(start.y - end.y);
        int segmentLen = dx*dx + dy*dy; // square len
        if (segmentLen > MIN_LINE_SEGMENT_LENGTH_SQ) {// skip creating short segments
            ILineSegment newSegment = factory.createSegment(start, end);
            segments.add(newSegment);
        }
    }

    // Preconditions:
    //     points should be sorted either by X or Y
    //     segments != null, factory != null, points != null
    private static void recognizeSegments(SortedSet<Point> points, List<ILineSegment> segments, ILineSegmentAbstractFactory factory) {
        Point segmentStart = points.first();
        Point segmentEnd = points.first();

        for (Point point: points) {
            int gap = (int)(Math.pow(point.x - segmentEnd.x, 2) + Math.pow(point.y - segmentEnd.y, 2));

            if (gap > CONTINUOUS_LINE_MAX_GAP_SQ)  {// new segment detected?
                createSegment(segmentStart, segmentEnd, segments, factory);
                segmentStart = point;
                segmentEnd = point;
            } else {
                segmentEnd = point;
            }
        }

        // Add last segment
        createSegment(segmentStart, segmentEnd, segments, factory);
    }

    public static List<ILineSegment> findCollinearSegments(int rho, double theta, Set<Point> points) {
        List<ILineSegment> segments = new ArrayList<>();
        SortedSet<Point> orderedPoints;

        if ((3*Math.PI/4 > theta) && (theta > Math.PI/4)) { // horizontal line
            orderedPoints = new TreeSet<>(X_COMPARATOR);
            orderedPoints.addAll(points);
            recognizeSegments(orderedPoints, segments, HorizontalSegmentsFactory);
        } else {                                            // vertical line
            orderedPoints = new TreeSet<>(Y_COMPARATOR);
            orderedPoints.addAll(points);
            recognizeSegments(orderedPoints, segments, VerticalSegmentsFactory);
        }

        return segments;
    }

    // For debug only
    private static void print(Iterable<Point> points) {
        for (Point p : points) {
            System.out.println("Point: (" + p.x + ", " + p.y + ")");
        }
    }
}
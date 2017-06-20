package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;
import android.support.v4.graphics.ColorUtils;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.rendering.GlRenderBuffer;
import com.example.neutrino.maze.rendering.VectorHelper;

/**
 * Created by neutrino on 7/7/2016.
 */
public class ThickLineSegment extends FloorPlanPrimitiveBase {
    public static final int ALPHA = 128;
    public static final int OPAQUE = 255;
    private static final int VERTICES_NUM = 4; // it's a rect after all
    protected static final int VERTICES_DATA_LENGTH = VERTICES_NUM * GlRenderBuffer.COORDS_PER_VERTEX;
    protected static final int INDICES_DATA_LENGTH = 6;
    private static final float DEFAULT_THICKNESS = 0.2f; // 20cm
    private static final float DEFAULT_COORDS_SOURCE = 0.5f;

    public ChangeType getChangeType() {
        return mChangeType;
    }

    // Different change types
    public enum ChangeType {
        CHANGE_START,
        CHANGE_END,
        CHANGE_SEGMENT
    };

    private static final float CHANGE_ONE_END_THRESHOLD = 0.30f;

    private transient final short mDrawOrder[] = { 0, 1, 2,   // first triangle
            1, 2, 3 }; // second triangle

    private final PointF mStart = new PointF(0, 0);
    private final PointF mEnd = new PointF(0, 0);
    private float mThickness;

    private transient ChangeType mChangeType;
    private transient final PointF mTappedLocation = new PointF();

    @Override
    protected int getVerticesNum() {
        return VERTICES_DATA_LENGTH;
    }

    @Override
    protected int getIndicesNum() {
        return INDICES_DATA_LENGTH;
    }

    public ThickLineSegment() {
        init(-DEFAULT_COORDS_SOURCE, DEFAULT_COORDS_SOURCE, DEFAULT_COORDS_SOURCE,
                -DEFAULT_COORDS_SOURCE, DEFAULT_THICKNESS);
    }

    public ThickLineSegment(float x1, float y1, float x2, float y2)
    {
        init(x1, y1, x2, y2, DEFAULT_THICKNESS);
    }

    public ThickLineSegment(float x1, float y1, float x2, float y2, float thickness) {
        init(x1, y1, x2, y2, thickness/2);
    }

    public ThickLineSegment(PointF start, PointF end) {
        init(start.x, start.y, end.x, end.y, DEFAULT_THICKNESS);
    }

    public ThickLineSegment(PointF start, PointF end, float thickness) {
        init(start.x, start.y, end.x, end.y, thickness);
    }

    private void init(float x1, float y1, float x2, float y2, float thickness) {
        mStart.x = x1;
        mStart.y = y1;
        mEnd.x = x2;
        mEnd.y = y2;
        mThickness = thickness;
        VectorHelper.splitLine(mStart, mEnd, mThickness /2, mVertices);

        System.arraycopy(mDrawOrder, 0, super.mIndices, 0, INDICES_DATA_LENGTH);
        mChangeType = ChangeType.CHANGE_END;
    }

    @Override
    public void updateVertices() {
        VectorHelper.splitLine(mStart, mEnd, mThickness /2, mVertices);
    }

    @Override
    public void scaleVertices(float scaleFactor) {
        mStart.set(mStart.x * scaleFactor, mStart.y * scaleFactor);
        mEnd.set(mEnd.x * scaleFactor, mEnd.y * scaleFactor);
    }

    @Override
    public RectF getBoundingBox() {
        float endX = mEnd.x;
        float endY = mEnd.y;

        // Avoid empty rect
        if (mStart.x == endX) endX += Float.MIN_VALUE;
        if (mStart.y == endY) endY += Float.MIN_VALUE;

        RectF boundingBox = new RectF(mStart.x, mStart.y, endX, endY);
        boundingBox.sort();

        return boundingBox;
    }

    @Override
    public boolean hasPoint(float x, float y) {
        // First test if the point within bounding box of line
        RectF boundingBox = getBoundingBox();
        boundingBox.left -= mThickness /2;
        boundingBox.top -= mThickness /2;
        boundingBox.right += mThickness /2;
        boundingBox.bottom += mThickness /2;

        if (! boundingBox.contains(x, y)) {
            return false;
        }

        // Now check if distance from given point to line is less then twice the thickness
        // This uses method described on Wikipedia (https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line)
        float xDiff = mEnd.x - mStart.x;
        float yDiff = mEnd.y - mStart.y;

        float twiceArea = Math.abs(yDiff * x - xDiff * y + mEnd.x * mStart.y - mEnd.y * mStart.x);
        float distance = (float) (twiceArea / Math.sqrt(yDiff * yDiff + xDiff * xDiff));

        return distance <= mThickness;// /2;
    }

    public float getThickness() {
        return mThickness;
    }

    public void setThickness(float thickness) {
        this.mThickness = thickness;
    }

    public PointF getStart() {
        return mStart;
    }

    public void setStart(float x, float y) {
        this.mStart.set(x, y);
    }

    public void setStart(PointF a) {
        this.mStart.set(a);
    }

    public PointF getEnd() {
        return mEnd;
    }

    public void setEnd(float x, float y) {
        this.mEnd.set(x, y);
    }

    public void setEnd(PointF b) {
        this.mEnd.set(b);
    }

    @Override
    public void handleChangeStart() {
        setColor(ColorUtils.setAlphaComponent(AppSettings.wallColor, ALPHA));
        rewriteToBuffer();
    }

    @Override
    public void handleChangeEnd() {
        setColor(ColorUtils.setAlphaComponent(AppSettings.wallColor, ThickLineSegment.OPAQUE));
        rewriteToBuffer();
    }

    @Override
    public void handleChange(float x, float y) {
        float dx = x - mTappedLocation.x;
        float dy = y - mTappedLocation.y;

        switch (mChangeType) {
            case CHANGE_START: {
                mStart.offset(dx, dy);
                break;
            }
            case CHANGE_END: {
                mEnd.offset(dx, dy);
                break;
            }
            case CHANGE_SEGMENT: {
                mStart.offset(dx, dy);
                mEnd.offset(dx, dy);
                break;
            }
        }

        mTappedLocation.set(x, y);
        updateVertices();
        rewriteToBuffer();
    }

    @Override
    public void setTapLocation(float x, float y) {
        mTappedLocation.set(x, y);

        // First check if it is closer to End for the case when we create new wall (b/c start is
        // placed where you tapped and only end is changed on drag)
        if (PointF.length(mTappedLocation.x - mEnd.x, mTappedLocation.y - mEnd.y) <= CHANGE_ONE_END_THRESHOLD) {
            mChangeType = ChangeType.CHANGE_END;
        }
        else if (PointF.length(mTappedLocation.x - mStart.x, mTappedLocation.y - mStart.y) <= CHANGE_ONE_END_THRESHOLD) {
            mChangeType = ChangeType.CHANGE_START;
        }
        else {
            mChangeType = ChangeType.CHANGE_SEGMENT;
        }
    }

    @Override
    public boolean equals(Object another) {
        if (another == this) return true;
        if (!(another instanceof Wall)) return false;
        if (!super.equals(another)) return false;

        ThickLineSegment anotherSegment = (ThickLineSegment) another;

        if (!anotherSegment.mStart.equals(this.mStart.x, this.mStart.y)) return false;
        if (!anotherSegment.mEnd.equals(this.mEnd.x, this.mEnd.y)) return false;
        if (anotherSegment.mThickness != this.mThickness) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 17;

        hash *= 31;
        hash += Float.floatToIntBits(mStart.x);
        hash *= 31;
        hash += Float.floatToIntBits(mStart.y);
        hash *= 31;
        hash += Float.floatToIntBits(mEnd.x);
        hash *= 31;
        hash += Float.floatToIntBits(mEnd.y);

        return hash;
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)--(%.2f, %.2f)", mStart.x, mStart.y, mEnd.x, mEnd.y);
    }
}

package com.example.neutrino.maze.floorplan;

import android.graphics.PointF;
import android.graphics.RectF;

import com.example.neutrino.maze.rendering.GlRenderBuffer;
import com.example.neutrino.maze.rendering.VectorHelper;

/**
 * Created by neutrino on 7/7/2016.
 */
public class Wall extends FloorPlanPrimitiveBase {
    private static final int VERTICES_NUM = 4; // it's a rect after all
    private static final int VERTICES_DATA_LENGTH = VERTICES_NUM * GlRenderBuffer.COORDS_PER_VERTEX;
    private static final int INDICES_DATA_LENGTH = 6;
    private static final float DEFAULT_WIDTH = 0.2f; // 20cm
    private static final float DEFAULT_COORDS_SOURCE = 0.5f;

    public ChangeType getChangeType() {
        return mChangeType;
    }

    // Different change types
    public enum ChangeType {
        CHANGE_A,
        CHANGE_B,
        CHANGE_WALL
    };

    private static final float CHANGE_ONE_END_THRESHOLD = 0.30f;

    private transient final short mDrawOrder[] = { 0, 1, 2,   // first triangle
            1, 2, 3 }; // second triangle

    private final PointF mStart = new PointF(0, 0);
    private final PointF mEnd = new PointF(0, 0);
    private float mWidth;

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

    public Wall() {
        init(-DEFAULT_COORDS_SOURCE, DEFAULT_COORDS_SOURCE, DEFAULT_COORDS_SOURCE,
                -DEFAULT_COORDS_SOURCE, DEFAULT_WIDTH);
    }

    public Wall(float x1, float y1, float x2, float y2)
    {
        init(x1, y1, x2, y2, DEFAULT_WIDTH);
    }

    public Wall(float x1, float y1, float x2, float y2, float width) {
        init(x1, y1, x2, y2, width/2);
    }

    private void init(float x1, float y1, float x2, float y2, float width) {
        mStart.x = x1;
        mStart.y = y1;
        mEnd.x = x2;
        mEnd.y = y2;
        mWidth = width;
        VectorHelper.splitLine(mStart, mEnd, mWidth/2, mVertices);

        System.arraycopy(mDrawOrder, 0, super.mIndices, 0, INDICES_DATA_LENGTH);
        mChangeType = ChangeType.CHANGE_B;
    }

    @Override
    public void updateVertices() {
        VectorHelper.splitLine(mStart, mEnd, mWidth/2, mVertices);
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

    public boolean hasPoint(float x, float y) {
        // First test if the point within bounding box of line
        RectF boundingBox = getBoundingBox();
        boundingBox.left -= mWidth/2;
        boundingBox.top -= mWidth/2;
        boundingBox.right += mWidth/2;
        boundingBox.bottom += mWidth/2;

        if (! boundingBox.contains(x, y)) {
            return false;
        }

        // Now check if distance from given point to line is less then twice the width
        // This uses method described on Wikipedia (https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line)
        float xDiff = mEnd.x - mStart.x;
        float yDiff = mEnd.y - mStart.y;

        float twiceArea = Math.abs(yDiff * x - xDiff * y + mEnd.x * mStart.y - mEnd.y * mStart.x);
        float distance = (float) (twiceArea / Math.sqrt(yDiff * yDiff + xDiff * xDiff));

        return distance <= mWidth;// /2;
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float mWidth) {
        this.mWidth = mWidth;
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

    public void handleChange(float x, float y) {
        float dx = x - mTappedLocation.x;
        float dy = y - mTappedLocation.y;

        switch (mChangeType) {
            case CHANGE_A: {
                mStart.offset(dx, dy);
                break;
            }
            case CHANGE_B: {
                mEnd.offset(dx, dy);
                break;
            }
            case CHANGE_WALL: {
                mStart.offset(dx, dy);
                mEnd.offset(dx, dy);
                break;
            }
        }

        mTappedLocation.set(x, y);
    }

    public void setTapLocation(float x, float y) {
        mTappedLocation.set(x, y);

        // Tapped point closer to A?
        if (PointF.length(mTappedLocation.x - mStart.x, mTappedLocation.y - mStart.y) <= CHANGE_ONE_END_THRESHOLD) {
            mChangeType = ChangeType.CHANGE_A;
        }
        // Closer to B?
        else if (PointF.length(mTappedLocation.x - mEnd.x, mTappedLocation.y - mEnd.y) <= CHANGE_ONE_END_THRESHOLD) {
            mChangeType = ChangeType.CHANGE_B;
        }
        else {
            mChangeType = ChangeType.CHANGE_WALL;
        }
    }

    @Override
    public boolean equals(Object another) {
        if (another == this) return true;
        if (!(another instanceof Wall)) return false;
        if (!super.equals(another)) return false;

        Wall anotherWall = (Wall) another;

        if (!anotherWall.mStart.equals(this.mStart.x, this.mStart.y)) return false;
        if (!anotherWall.mEnd.equals(this.mEnd.x, this.mEnd.y)) return false;
        if (anotherWall.mWidth != this.mWidth) return false;

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

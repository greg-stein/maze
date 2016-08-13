package com.example.neutrino.maze;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by neutrino on 7/2/2016.
 */
public class FloorPlanView extends GLSurfaceView {
    private static final float CORRIDOR_DEFAULT_WIDTH = 0.03f;

    private final FloorPlanRenderer mRenderer = new FloorPlanRenderer();
    private boolean mDrugStarted;
    private static final PointF mLastBuildWallsLocation = new PointF();
    private static final PointF mNewWallsLocation =  new PointF();
    private static final float[] mWallsBuffer = new float[12];
    private static Wall mPreviousRightWall;
    private static Wall mPreviousLeftWall;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1;
    private boolean mIsInEditMode;

    public FloorPlanView(Context context) {
        super(context);
        init(context, null);
    }

    public FloorPlanView(Context context, AttributeSet attrs) {
        super(context,attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        mRenderer.setGlView(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public boolean isContentsInEditMode() {
        return mIsInEditMode;
    }

    public void setContentsInEditMode(boolean isEditMode) {
        this.mIsInEditMode = isEditMode;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            // Update scale
            mRenderer.setScale(mScaleFactor);
            requestRender();
            return true;
        }
    }


    public void updateAngle(float degree) {
        mRenderer.setAngle(mRenderer.getAngle() + degree);
        requestRender();
    }

    public void updateOffset(float offsetX, float offsetY) {
        mRenderer.setOffset(offsetX, offsetY);
        requestRender();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsInEditMode) {
            handleEditing(event);
        }
        else {
            handlePanAndZoom(event);
        }

        requestRender();
        return true;
    }

    private void handleEditing(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        // Get the index of the pointer associated with the action.
        int index = MotionEventCompat.getActionIndex(event);

        int xPos = (int) MotionEventCompat.getX(event, index);
        int yPos = (int) MotionEventCompat.getY(event, index);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDrugStarted = true;
                mRenderer.handleStartDrag(xPos, yPos);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDrugStarted && !mScaleDetector.isInProgress()) {
                    mRenderer.handleDrag(xPos, yPos);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDrugStarted) {
                    mRenderer.handleEndDrag(xPos, yPos);
                    mDrugStarted = false;
                }
                break;
        }
    }

    private void handlePanAndZoom(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
    }

    public void loadEngine() {
        mRenderer.loadEngine();
    }

    public void initCorridorWalls() {
        // Convert center of view to world coordinates
        mRenderer.windowToWorld(getMeasuredWidth()/2, getMeasuredHeight()/2, mLastBuildWallsLocation);
    }

    // This method builds walls from last call
    public void buildCorridorWalls() {
        // Convert center of view to world coordinates
        mRenderer.windowToWorld(getMeasuredWidth()/2, getMeasuredHeight()/2, mNewWallsLocation);

        VectorHelper.splitLine(mLastBuildWallsLocation, mNewWallsLocation, CORRIDOR_DEFAULT_WIDTH, mWallsBuffer);

        // Make walls continuous (not dashed)
        if (mPreviousRightWall != null) {
            mWallsBuffer[0] = mPreviousRightWall.getB().x;
            mWallsBuffer[1] = mPreviousRightWall.getB().y;
        }
        if (mPreviousLeftWall != null) {
            mWallsBuffer[3] = mPreviousLeftWall.getB().x;
            mWallsBuffer[4] = mPreviousLeftWall.getB().y;
        }

        Wall rightWall = new Wall(mWallsBuffer[0], mWallsBuffer[1], mWallsBuffer[6], mWallsBuffer[7]);
        Wall leftWall = new Wall(mWallsBuffer[3], mWallsBuffer[4], mWallsBuffer[9], mWallsBuffer[10]);

        mRenderer.addWall(rightWall);
        mRenderer.addWall(leftWall);

        mPreviousRightWall = rightWall;
        mPreviousLeftWall = leftWall;
        mLastBuildWallsLocation.set(mNewWallsLocation);
    }
}

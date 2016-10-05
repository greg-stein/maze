package com.example.neutrino.maze;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.example.neutrino.maze.floorplan.FloorPlanSerializer;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Wall;

import java.util.List;
import java.util.Map;

/**
 * Created by neutrino on 7/2/2016.
 */
public class FloorPlanView extends GLSurfaceView {
    private static final float CORRIDOR_DEFAULT_WIDTH = 1.0f; // 1m

    private final FloorPlanRenderer mRenderer = new FloorPlanRenderer();
    private boolean mDragStarted;
    private static final PointF mLastBuildWallsLocation = new PointF();
    private static final PointF mNewWallsLocation =  new PointF();
    private static final float[] mWallsBuffer = new float[12];
    private static Wall mPreviousRightWall;
    private static Wall mPreviousLeftWall;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = FloorPlanRenderer.DEFAULT_SCALE_FACTOR;
    private boolean mIsInEditMode;

    public static final int BUILDER_MODE_NONE = 0;
    public static final int BUILDER_MODE_LEFT = 1;
    public static final int BUILDER_MODE_RIGHT = 2;
    public static final int BUILDER_MODE_BOTH = 3;

    public int autobuilderMode = BUILDER_MODE_NONE;
    private final PointF mCurrentLocation = new PointF();

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

    public void setMode(boolean isEditMode) {
        this.mIsInEditMode = isEditMode;
    }

    public void setSetLocationOp(boolean mSetLocation) {
    }

    public enum Operation {
        NONE, ADD_WALL, REMOVE_WALL, SET_LOCATION
    }
    public Operation operation = Operation.NONE;

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
                switch (operation) {
                    case NONE:
                    case ADD_WALL:
                        mDragStarted = true;
                        mRenderer.handleStartDrag(xPos, yPos, operation);
                        break;
                    case REMOVE_WALL:
                        mRenderer.processWallDeletion(xPos, yPos);
                        break;
                    case SET_LOCATION:
                        setLocation(xPos, yPos);
                        break;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDragStarted && !mScaleDetector.isInProgress()) {
                    mRenderer.handleDrag(xPos, yPos);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDragStarted) {
                    mRenderer.handleEndDrag(xPos, yPos);
                }

                mDragStarted = false;
                break;
        }
    }

    private int mActivePointerId;
    private static final int INVALID_POINTER_ID = -1;
    private void handlePanAndZoom(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        // Get the index of the pointer associated with the action.

        mScaleDetector.onTouchEvent(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mActivePointerId = event.getPointerId(0);
                int index = MotionEventCompat.getActionIndex(event);
                final int x = (int) MotionEventCompat.getX(event, index);
                final int y = (int) MotionEventCompat.getY(event, index);

                mRenderer.handleStartPan(x, y);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex != INVALID_POINTER_ID) {
                    final int x = (int) event.getX(pointerIndex);
                    final int y = (int) event.getY(pointerIndex);

                    mRenderer.handlePan(x, y);
                }
                break;
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.02f, Math.min(mScaleFactor, 2.0f));

            // Update scale
            mRenderer.setScale(mScaleFactor);
            requestRender();
            return true;
        }
    }

    public void initWallsAutobuilder() {
        // Convert center of view to world coordinates
        mRenderer.windowToWorld(getMeasuredWidth()/2, getMeasuredHeight()/2, mLastBuildWallsLocation);
    }

    // This method builds walls from last call
    public void autobuildWalls() {
        // Convert center of view to world coordinates
        mRenderer.windowToWorld(getMeasuredWidth()/2, getMeasuredHeight()/2, mNewWallsLocation);

        VectorHelper.splitLine(mLastBuildWallsLocation, mNewWallsLocation, CORRIDOR_DEFAULT_WIDTH, mWallsBuffer);

        Wall leftWall = null, rightWall = null;

        // Make walls continuous (not dashed)
        if ((autobuilderMode & BUILDER_MODE_LEFT) != 0) {
            if (mPreviousLeftWall != null) {
                mWallsBuffer[3] = mPreviousLeftWall.getB().x;
                mWallsBuffer[4] = mPreviousLeftWall.getB().y;
            }
            leftWall = new Wall(mWallsBuffer[3], mWallsBuffer[4], mWallsBuffer[9], mWallsBuffer[10]);
            mRenderer.addPrimitive(leftWall);
        }

        if ((autobuilderMode & BUILDER_MODE_RIGHT) != 0) {
            if (mPreviousRightWall != null) {
                mWallsBuffer[0] = mPreviousRightWall.getB().x;
                mWallsBuffer[1] = mPreviousRightWall.getB().y;
            }
            rightWall = new Wall(mWallsBuffer[0], mWallsBuffer[1], mWallsBuffer[6], mWallsBuffer[7]);
            mRenderer.addPrimitive(rightWall);
        }

        mPreviousRightWall = rightWall;
        mPreviousLeftWall = leftWall;
        mLastBuildWallsLocation.set(mNewWallsLocation);
    }

    public enum ViewMode {VIEW_MODE, EDIT_MODE};
    private ViewMode mViewMode;

    private void initControls() {
    }

    public ViewMode getViewMode() {
        return mViewMode;
    }

    public void setViewMode(ViewMode viewMode) {
        this.mViewMode = viewMode;

        switch (viewMode) {
            case VIEW_MODE:
                break;
            case EDIT_MODE:
                break;
        }
    }

    public String getFloorPlanAsJSon() {
        List<IFloorPlanPrimitive> floorPlan = mRenderer.getFloorPlan();
        return FloorPlanSerializer.serializeFloorPlan(floorPlan);
    }

    public void setFloorPlanAsJSon(String gsonString) {
        List<IFloorPlanPrimitive> floorplan = FloorPlanSerializer.deserializeFloorPlan(gsonString);

        mRenderer.setFloorPlan(floorplan);
    }

    public void setOnWallLengthChangedListener(IWallLengthChangedListener listener) {
        mRenderer.setOnWallLengthChangedListener(listener);
    }

    public void putStep(float x, float y) {
        mRenderer.putStep(x, y);
    }


    // This overloaded method is used internally when user clicks on location
    private void setLocation(int x, int y) {
        PointF worldLocation = new PointF();
        mRenderer.windowToWorld(x, y, worldLocation);
        setLocation(worldLocation.x, worldLocation.y);
    }

    // This method is used when you want to set location programmatically
    public void setLocation(float x, float y) {
        mCurrentLocation.set(x, y);
        mRenderer.drawCurrentLocation(mCurrentLocation);

        if (mNewLocationListener != null) {
            mNewLocationListener.onLocationPlaced(x, y);
        }
    }

    public interface IOnLocationPlacedListener {
        void onLocationPlaced(float x, float y);
    }
    private IOnLocationPlacedListener mNewLocationListener = null;
    public void setOnLocationPlacedListener(IOnLocationPlacedListener listener) {
        this.mNewLocationListener = listener;
    }

    public void placeWiFiMarkAt(PointF center, WiFiTug.Fingerprint fingerprint) {
        mRenderer.putMark(center.x, center.y, fingerprint);
    }
}

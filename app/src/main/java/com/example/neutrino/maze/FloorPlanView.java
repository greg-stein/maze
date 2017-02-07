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
import com.example.neutrino.maze.floorplan.WifiMark;

import java.util.List;

/**
 * Created by neutrino on 7/2/2016.
 */
public class FloorPlanView extends GLSurfaceView {
    private final FloorPlanRenderer mRenderer = new FloorPlanRenderer();
    private boolean mDragStarted;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = FloorPlanRenderer.DEFAULT_SCALE_FACTOR;
    private boolean mIsInEditMode;
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

    public void highlightCentroidMarks(List<WifiMark> centroidMarks) {
        if (centroidMarks == null) return;
        mRenderer.highlightCentroidMarks(centroidMarks);
    }

    public void rescaleMap(float scaleFactor) {
        mRenderer.rescaleFloorplan(scaleFactor);
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
        float newOffsetX = mRenderer.getOffsetX() + offsetX;
        float newOffsetY = mRenderer.getOffsetY() + offsetY;
        mRenderer.setOffset(newOffsetX, newOffsetY);
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

    public enum ViewMode {VIEW_MODE, EDIT_MODE};
    private ViewMode mViewMode;

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

    public final <T extends IFloorPlanPrimitive> List<T> getPrimitives(Class<T> klazz) {
        List<IFloorPlanPrimitive> floorPlan = mRenderer.getFloorPlan();
        return CommonHelper.getPrimitives(klazz, floorPlan);
    }

    public String getFloorPlanAsJSon() {
        List<IFloorPlanPrimitive> floorPlan = mRenderer.getFloorPlan();
        return FloorPlanSerializer.serializeFloorPlan(floorPlan);
    }

    public void setFloorPlanAsJSon(String jsonString) {
        List<IFloorPlanPrimitive> floorplan = FloorPlanSerializer.deserializeFloorPlan(jsonString);

        mRenderer.setFloorPlan(floorplan);
    }

    public void setFloorPlan(List<? extends IFloorPlanPrimitive> floorplan) {
        mRenderer.setFloorPlan(floorplan);
    }

    public void setFloorPlan(List<? extends IFloorPlanPrimitive> floorplan, boolean inInit) {
        mRenderer.setFloorPlan(floorplan);
        if (!inInit) mRenderer.performQueuedTask();
    }

    public void setOnWallLengthChangedListener(IWallLengthChangedListener listener) {
        mRenderer.setOnWallLengthChangedListener(listener);
    }

    public void putStep(float x, float y) {
        mRenderer.putStep(x, y);
    }


    // This overloaded method is used internally when user clicks on location
    protected void setLocation(int x, int y) {
        PointF worldLocation = new PointF();
        mRenderer.windowToWorld(x, y, worldLocation);
        setLocation(worldLocation.x, worldLocation.y);
    }

    // This method is used when you want to set location programmatically
    public void setLocation(float x, float y) {
        mCurrentLocation.set(x, y);
        mRenderer.drawLocationMarkAt(mCurrentLocation);

        if (mNewLocationListener != null) {
            mNewLocationListener.onLocationPlaced(x, y);
        }
    }

    public void putLocationMarkAt(PointF location) {
        mRenderer.drawLocationMarkAt(location);
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

    public void clearFloorPlan() {
        mRenderer.clearFloorPlan();
    }
}

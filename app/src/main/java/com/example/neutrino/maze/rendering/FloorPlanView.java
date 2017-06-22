package com.example.neutrino.maze.rendering;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.EditText;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Path;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.rendering.FloorPlanRenderer.IWallLengthChangedListener;
import com.example.neutrino.maze.rendering.FloorPlanRenderer.IFloorplanLoadCompleteListener;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;

import java.util.List;

import static com.example.neutrino.maze.rendering.FloorPlanView.MapOperation.MOVE;

/**
 * Created by neutrino on 7/2/2016.
 */
public class FloorPlanView extends GLSurfaceView {
    private FloorPlanRenderer mRenderer;
    private boolean mDragStarted;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = FloorPlanRenderer.DEFAULT_SCALE_FACTOR;
    private boolean mIsInEditMode;
    private final PointF mCurrentLocation = new PointF();
    private boolean mHandlingTagCreation = false;
    private Path mPath;

    public FloorPlanView(Context context) {
        this(context, null);
    }

    public FloorPlanView(Context context, AttributeSet attrs) {
        super(context,attrs);
        if (!isInEditMode()) {
            mRenderer = new FloorPlanRenderer();
            init(context, attrs);
        }
    }

    private void init(Context context, AttributeSet attrs) {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        mRenderer.setGlView(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mRenderer.setOnFloorplanLoadCompleteListener(new IFloorplanLoadCompleteListener() {
            @Override
            public void onFloorplanLoadComplete() {
                showMap();
            }
        });
    }

    public void drawDistribution(PointF mean, float stdev) {
        if (AppSettings.inDebug) {
            mRenderer.drawDistribution(mean, stdev);
        }
    }

    public void renderPrimitive(IFloorPlanPrimitive primitive) {
        mRenderer.renderPrimitive(primitive);
    }

    public boolean isContentsInEditMode() {
        return mIsInEditMode;
    }

    public void setFloorplanEditMode(boolean isEditMode) {
        this.mIsInEditMode = isEditMode;
    }

    public void highlightCentroidMarks(List<Fingerprint> centroidMarks) {
        if (centroidMarks == null) return;
        mRenderer.highlightCentroidMarks(centroidMarks);
    }

    public void rescaleMap(float scaleFactor) {
        mRenderer.rescaleFloorplan(scaleFactor);
    }

    public enum MapOperation {
        MOVE, ADD, REMOVE, SET_LOCATION
    }
    public MapOperation mapOperation = MOVE;
    public enum MapOperand {
        WALL, SHORT_WALL, BOUNDARIES, LOCATION_TAG
    }
    public MapOperand operand;

    public enum Operation {
        NONE, ADD_WALL, REMOVE_WALL, ADD_TAG, SET_LOCATION
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
                switch (mapOperation) {
                    case MOVE: // move existing wall if under tap location
                        mRenderer.handleStartDrag(xPos, yPos, mapOperation, operand);
                        if (mRenderer.startDragHandled()) {
                            mDragStarted = true;
                        } else {
                            handlePanAndZoom(event);
                        }
                        break;
                    case ADD: switch (operand) {
                        case WALL:
                            mDragStarted = true;
                            mRenderer.handleStartDrag(xPos, yPos, mapOperation, operand);
                            break;
                        case SHORT_WALL:
                            break;
                        case BOUNDARIES:
                            break;
                        case LOCATION_TAG:
                            mHandlingTagCreation = true;
                            askForTagName(xPos, yPos);
                            break;
                        }
                        break;
                    case REMOVE:
                        mRenderer.processObjectDeletion(xPos, yPos);
                        break;
                    case SET_LOCATION:
                        setLocation(xPos, yPos);
                        break;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDragStarted) {
                    if (!mScaleDetector.isInProgress()) {
                        mRenderer.handleDrag(xPos, yPos);
                    }
                } else if (!mHandlingTagCreation) {
                    handlePanAndZoom(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDragStarted) {
                    mRenderer.handleEndDrag(xPos, yPos);
                    mDragStarted = false;
                } else if (!mHandlingTagCreation) {
                    handlePanAndZoom(event);
                }

                break;
        }
    }

    private void askForTagName(final int x, final int y) {
        final EditText input = new EditText(AppSettings.appActivity);
        String dialogTitle = "New tag";
        String okButtonCaption = "Add";
        PointF worldPoint = new PointF();
        mRenderer.windowToWorld(x, y, worldPoint);

        final Tag tagAtTapLocation = mRenderer.getTagHavingPoint(worldPoint.x, worldPoint.y);
        if (tagAtTapLocation != null) {
            input.setText(tagAtTapLocation.getLabel());
            dialogTitle = "Change tag";
            okButtonCaption = "Change";
        }

        new AlertDialog.Builder(AppSettings.appActivity)
                .setTitle(dialogTitle)
                .setView(input)
//                .setMessage("Paste in the link of an image to moustachify!")
                .setPositiveButton(okButtonCaption, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (tagAtTapLocation == null) {
                            mRenderer.createNewTag(x, y, input.getText().toString());
                        } else {
                            tagAtTapLocation.setLabel(input.getText().toString());
                            mRenderer.calculateTagBoundaries(tagAtTapLocation);
                        }
                        mHandlingTagCreation = false;
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mHandlingTagCreation = false;
                    }
                })
                .show();
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

    public void plot(FloorPlan floorPlan) {
        mRenderer.setFloorPlan(floorPlan.getSketch());
        mRenderer.setTags(floorPlan.getTags());
        mRenderer.performQueuedTask();
    }

    public void plot(List<? extends IFloorPlanPrimitive> floorplan, boolean inInit) {
        mRenderer.setFloorPlan((List<IFloorPlanPrimitive>) floorplan);
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

        if (mNewLocationListener != null) {
            mNewLocationListener.onLocationPlaced(worldLocation);
        }
    }

    // This method is used when you want to set location programmatically
    public void setLocation(float x, float y) {
        if (mRenderer.isFloorPlanSet()) { // otherwise no need to show current location
            mCurrentLocation.set(x, y);
            mRenderer.drawLocationMarkAt(mCurrentLocation);
        }
    }

    public void setLocation(PointF location) {
        setLocation(location.x, location.y);
    }

    public PointF getLocation() {
         return mCurrentLocation;
    }

    public void centerToPoint(PointF point) {
        mRenderer.setOffset(-point.x, -point.y);
    }

    public void showMap() {
        PointF mapVertex = mRenderer.getMapAnyVertex();
        centerToPoint(mapVertex);
    }

    public void centerToLocation() {
        centerToPoint(mCurrentLocation);
    }

    public void centerToPoint(int x, int y) {
        PointF worldLocation = new PointF();
        mRenderer.windowToWorld(x, y, worldLocation);
        centerToPoint(worldLocation);
    }

    public void putLocationMarkAt(PointF location) {
        mRenderer.drawLocationMarkAt(location);
        centerToPoint(location);
    }

    public interface IOnLocationPlacedListener {
        void onLocationPlaced(PointF location);
    }
    private IOnLocationPlacedListener mNewLocationListener = null;
    public void setOnLocationPlacedListener(IOnLocationPlacedListener listener) {
        this.mNewLocationListener = listener;
    }

    public Fingerprint placeWiFiMarkAt(PointF center, WiFiFingerprint wiFiFingerprint) {
        return mRenderer.putMark(center.x, center.y, wiFiFingerprint);
    }

    public void clearFloorPlan() {
        mRenderer.clearFloorPlan();
    }

    public void renderPath(Path path) {
        if (mPath != null) {
            mPath.cloak();
        }

        mPath = path;
        mRenderer.addPrimitive(path);
    }
}


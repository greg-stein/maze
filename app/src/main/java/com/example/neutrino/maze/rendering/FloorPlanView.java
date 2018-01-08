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
import com.example.neutrino.maze.core.IMainView;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Path;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.floorplan.transitions.Teleport;
import com.example.neutrino.maze.rendering.FloorPlanRenderer.IWallLengthChangedListener;
import com.example.neutrino.maze.util.IFuckingSimpleCallback;

import java.util.List;

import static com.example.neutrino.maze.core.IMainView.MapOperation.MOVE;

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
            mRenderer = new FloorPlanRenderer(getContext());
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

//    public void rescaleMap(float scaleFactor) {
//        mRenderer.rescaleFloorplan(scaleFactor);
//    }

    public IMainView.MapOperation mapOperation = MOVE;
    public IMainView.MapOperand operand;

    public void setElementFactory(IMainView.IElementFactory elementFactory) {
        mRenderer.setElementFactory(elementFactory);
    }

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
//                            mRenderer.getFloorPlan().setSketchDirty(true); // Need to flush it
                            break;
                        case SHORT_WALL:
                            break;
                        case BOUNDARIES:
                            break;
                        case TELEPORT:
                            mHandlingTagCreation = true;
                            askForTeleportNumber(xPos, yPos);
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
        final EditText input = new EditText(getContext());
        String dialogTitle = "New tag";
        String okButtonCaption = "Add";
        PointF worldPoint = new PointF();
        mRenderer.windowToWorld(x, y, worldPoint);

        // TODO: Tags should migrate to Floor
        final Tag tagAtTapLocation = mRenderer.getTagHavingPoint(worldPoint);
        if (tagAtTapLocation != null) {
            input.setText(tagAtTapLocation.getLabel());
            dialogTitle = "Change tag";
            okButtonCaption = "Change";
        }

        new AlertDialog.Builder(getContext())
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

    private void askForTeleportNumber(final int x, final int y) {
        final EditText input = new EditText(getContext());
        String dialogTitle = "New teleport";
        String okButtonCaption = "Add";
        final PointF worldPoint = new PointF();
        mRenderer.windowToWorld(x, y, worldPoint);

        // TODO: Tags should migrate to Floor
        Tag tagAtLocation = mRenderer.getTagHavingPoint(worldPoint);
        if (!(tagAtLocation instanceof Teleport)) {
            tagAtLocation = null;
        }
        if (tagAtLocation != null) {
            input.setText(tagAtLocation.getLabel());
            dialogTitle = "Change teleport id";
            okButtonCaption = "Change";
        }
        final Tag teleportAtTapLocation = tagAtLocation;

        new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setView(input)
//                .setMessage("Paste in the link of an image to moustachify!")
                .setPositiveButton(okButtonCaption, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (teleportAtTapLocation == null) {
                            Teleport teleport = new Teleport(worldPoint, input.getText().toString(),
                                    Teleport.Type.ELEVATOR);
                            mRenderer.addNewTag(teleport);
                            mRenderer.addPrimitive(teleport);
                        } else {
                            teleportAtTapLocation.setLabel(input.getText().toString());
                            mRenderer.calculateTagBoundaries(teleportAtTapLocation);
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

    public ElementsRenderGroup renderElementsGroup(List<IFloorPlanPrimitive> elements) {
        return mRenderer.renderElements(elements);
    }

    public TextRenderGroup renderTagsGroup(List<Tag> tags) {
        return mRenderer.renderTags(tags);
    }

    public void plot(FloorPlan floorPlan, PointF pointToShow) {
        mRenderer.renderElements(floorPlan.getSketch());
//        mRenderer.setTags(floorPlan.getTags());
        centerToPoint(pointToShow);
    }

    public void setOnWallLengthChangedListener(IWallLengthChangedListener listener) {
        mRenderer.setOnWallLengthChangedListener(listener);
    }

    public void setOnWallLengthDisplay(IWallLengthChangedListener listener) {
        mRenderer.setOnWallLengthStartChangingListener(listener);
    }

    public void setOnWallLengthHide(IFuckingSimpleCallback callback) {
        mRenderer.setOnWallLengthEndChangingListener(callback);
    }

    public void putStep(float x, float y) {
        mRenderer.putStep(x, y);
    }


    // This overloaded method is used internally when user clicks on location
    protected void setLocation(int x, int y) {
        PointF worldLocation = new PointF();
        mRenderer.windowToWorld(x, y, worldLocation);

        if (mNewLocationListener != null) {
            mNewLocationListener.onLocationSetByUser(worldLocation);
        }
    }

    // This method is used when you want to set location programmatically
    public void setLocation(float x, float y) {
        if (!mRenderer.isSketchEmpty()) { // otherwise no need to show current location
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
        void onLocationSetByUser(PointF location);
    }
    private IOnLocationPlacedListener mNewLocationListener = null;
    public void setOnLocationPlacedListener(IOnLocationPlacedListener listener) {
        this.mNewLocationListener = listener;
    }

    public void placeFingerprint(Fingerprint fingerprint) {
        mRenderer.putFingerprint(fingerprint);
    }

    public void clearFloorPlan() {
        Runnable glRunnable = new Runnable() {
            @Override
            public void run() {
                mRenderer.clearSketch();
                requestRender();
            }
        };
        queueEvent(glRunnable);
    }

    public void renderPath(Path path) {
        if (mPath != null) {
            mPath.cloak();
        }

        mPath = path;
        mRenderer.addPrimitive(path);
    }
}


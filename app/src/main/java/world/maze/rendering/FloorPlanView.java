package world.maze.rendering;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.support.v4.util.Pair;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.EditText;

import world.maze.AppSettings;
import world.maze.core.IMainView;
import world.maze.floorplan.Fingerprint;
import world.maze.floorplan.FloorPlan;
import world.maze.floorplan.IFloorPlanPrimitive;
import world.maze.floorplan.IMoveable;
import world.maze.floorplan.Path;
import world.maze.floorplan.Tag;
import world.maze.floorplan.transitions.Teleport;
import world.maze.rendering.FloorPlanRenderer.IWallLengthChangedListener;
import world.maze.util.IFuckingSimpleCallback;

import java.util.List;

import static world.maze.core.IMainView.MapOperation.MOVE;

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

    public void clearRenderedElements() {
        mRenderer.clearRenderedElements();
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
                // Achtung! this is for the case if continuation of current gesture will be panning
                mRenderer.handleStartPan(xPos, yPos);

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

        // This allows only one line. Even if "Enter" is pressed it will put space instead new line
        // However in the string a new line will still be present!
        input.setMaxLines(1);
        input.setTransformationMethod(new SingleLineTransformationMethod());

        String dialogTitle = "New tag";
        String okButtonCaption = "Add";
        final PointF worldPoint = new PointF();
        mRenderer.windowToWorld(x, y, worldPoint);

        // TODO: Tags should migrate to Floor
        final Pair<IRenderGroup, IMoveable> tagInfo = mRenderer.findObjectHavingPoint(worldPoint, Tag.class);
        if (tagInfo != null) {
            Tag foundTag = (Tag)tagInfo.second;
            input.setText(foundTag.getLabel());
            dialogTitle = "Change tag";
            okButtonCaption = "Change";
        }

        new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setView(input)
//                .setMessage("Paste in the link of an image to moustachify!")
                .setPositiveButton(okButtonCaption, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // System.lineSeparator() requires API LEVEL 19
                        final String tagLabel = input.getText().toString().replace(System.getProperty("line.separator"), " ");
                        Tag tagAtTapLocation;

                        if (tagInfo == null) { // New tag?
                            tagAtTapLocation = mRenderer.createNewTag(worldPoint, tagLabel);
                        } else { // existing tag
                            tagAtTapLocation = (Tag)tagInfo.second;
                            tagAtTapLocation.setLabel(tagLabel);
                            IRenderGroup tagGroup = tagInfo.first;
                            tagGroup.setChangedElement(tagAtTapLocation);
                        }

                        mRenderer.calculateTagBoundaries(tagAtTapLocation);

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

        // This allows only one line. Even if "Enter" is pressed it will put space instead new line
        // However in the string a new line will still be present!
        input.setMaxLines(1);
        input.setTransformationMethod(new SingleLineTransformationMethod());

        String dialogTitle = "New teleport";
        String okButtonCaption = "Add";
        final PointF worldPoint = new PointF();
        mRenderer.windowToWorld(x, y, worldPoint);

        // TODO: Tags should migrate to Floor
        final Pair<IRenderGroup, IMoveable> teleportInfo = mRenderer.findObjectHavingPoint(worldPoint, Teleport.class);
        if (teleportInfo != null) {
            // Existing teleport
            Teleport foundTeleport = (Teleport) teleportInfo.second;
            input.setText(foundTeleport.getLabel());
            dialogTitle = "Change teleport id";
            okButtonCaption = "Change";
        }

        new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setView(input)
                .setPositiveButton(okButtonCaption, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // System.lineSeparator() requires API LEVEL 19
                        String teleportId = input.getText().toString().replace(System.getProperty("line.separator"), "");
                        Teleport teleportAtTapLocation;

                        if (teleportInfo == null) { // New teleport?
                            teleportAtTapLocation = mRenderer.createNewTeleport(worldPoint, teleportId);
                        } else { // Existing teleport
                            teleportAtTapLocation = (Teleport) teleportInfo.second;
                            teleportAtTapLocation.setLabel(teleportId);
                            IRenderGroup teleportGroup = teleportInfo.first;
                            teleportGroup.setChangedElement(teleportAtTapLocation);
                        }
                        mRenderer.calculateTagBoundaries(teleportAtTapLocation);
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

    public ElementsRenderGroup renderElementsGroup(List<? extends IFloorPlanPrimitive> elements) {
        return mRenderer.renderElements(elements);
    }

    public TextRenderGroup renderTagsGroup(List<? extends Tag> tags) {
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

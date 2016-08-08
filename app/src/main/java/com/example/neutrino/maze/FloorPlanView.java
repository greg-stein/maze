package com.example.neutrino.maze;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;

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
    }

    public void updateAngle(float degree) {
        mRenderer.setAngle(mRenderer.getAngle() + degree);
        requestRender();
    }

    public void updateOffset(float offsetX, float offsetY) {
        mRenderer.setOffset(offsetX, offsetY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        // Get the index of the pointer associated with the action.
        int index = MotionEventCompat.getActionIndex(event);
        int xPos = -1;
        int yPos = -1;

        if (event.getPointerCount() > 1) {
            // Multitouch event
            xPos = (int)MotionEventCompat.getX(event, index);
            yPos = (int)MotionEventCompat.getY(event, index);
        } else {
            // Single touch event
            xPos = (int)MotionEventCompat.getX(event, index);
            yPos = (int)MotionEventCompat.getY(event, index);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mDrugStarted = true;
                    mRenderer.handleStartDrag(xPos, yPos);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mDrugStarted) {
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

        return true;
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

        Wall rightWall = new Wall(mWallsBuffer[0], mWallsBuffer[1], mWallsBuffer[6], mWallsBuffer[7]);
        Wall leftWall = new Wall(mWallsBuffer[3], mWallsBuffer[4], mWallsBuffer[9], mWallsBuffer[10]);

        mRenderer.addWall(rightWall);
        mRenderer.addWall(leftWall);

        mLastBuildWallsLocation.set(mNewWallsLocation);
    }
}

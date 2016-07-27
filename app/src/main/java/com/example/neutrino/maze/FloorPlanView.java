package com.example.neutrino.maze;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by neutrino on 7/2/2016.
 */
public class FloorPlanView extends GLSurfaceView {
    private final FloorPlanRenderer mRenderer = new FloorPlanRenderer();

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
            if (action == MotionEvent.ACTION_DOWN) {
                mRenderer.handleTouch(xPos, yPos);
            }
        }

        return true;
    }
}

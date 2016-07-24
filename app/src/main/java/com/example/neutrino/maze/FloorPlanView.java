package com.example.neutrino.maze;

import android.content.Context;
import android.opengl.GLSurfaceView;
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
    public boolean onTouchEvent(MotionEvent e) {
        mRenderer.handleTouch();

        return true;
    }
}

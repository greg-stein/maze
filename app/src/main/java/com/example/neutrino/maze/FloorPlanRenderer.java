package com.example.neutrino.maze;

import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.support.v4.graphics.ColorUtils;

import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.LocationMark;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.floorplan.WifiMark;

import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by neutrino on 7/2/2016.
 */
public class FloorPlanRenderer implements GLSurfaceView.Renderer {

    public static final int ALPHA = 128;
    public static final int OPAQUE = 255;
    static final float DEFAULT_SCALE_FACTOR = 0.1f;

    public volatile float mAngle;
    private float mOffsetX;
    private float mOffsetY;
    private float mScaleFactor = DEFAULT_SCALE_FACTOR;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mModelMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];
    private final float[] mTranslationMatrix = new float[16];
    private final float[] mScaleMatrix = new float[16];
    private final float[] mIntermediateMatrix = new float[16];
    private static float[] mRay = new float[6]; // ray represented by 2 points

    private GLSurfaceView mGlView;
    private GlEngine mGlEngine;
    private int mViewPortWidth;
    private int mViewPortHeight;
    private final PointF mDragStart = new PointF();
    private Wall mSelectedWall;
    private boolean mAddedWallByDrag;
    private static final float[] mBgColorF = new float[4];
    private LocationMark mLocationMark = null;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background frame color
        VectorHelper.colorTo3F(AppSettings.mapBgColor, mBgColorF);
        GLES20.glClearColor(mBgColorF[0], mBgColorF[1], mBgColorF[2], mBgColorF[3]);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glDepthFunc(GLES20.GL_LESS);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Initialize the accumulated scale, rotation & translation matrices
        Matrix.setIdentityM(mScaleMatrix, 0);
        Matrix.setIdentityM(mRotationMatrix, 0);
        Matrix.setIdentityM(mTranslationMatrix, 0);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -3.0f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = 0.0f; //-5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        loadEngine();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // Remember these values for later use
        mViewPortWidth = width;
        mViewPortHeight = height;

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 3.0f;
        final float far = 7.0f;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    // Placed here to avoid unnecessary memory allocation on each onDrawFrame() call
    private final float[] mScratch = new float[16];

    @Override
    public void onDrawFrame(GL10 gl) {
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        // Combine the rotation matrix with the projection and camera view
        Matrix.multiplyMM(mScratch, 0, mMVPMatrix, 0, mModelMatrix, 0);

        if (mGlEngine != null) mGlEngine.render(mScratch);
    }

    private void updateModelMatrix() {
        Matrix.setIdentityM(mScaleMatrix,0);
        Matrix.scaleM(mScaleMatrix, 0, mScaleFactor, mScaleFactor, mScaleFactor);

        Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, 1.0f);

        Matrix.setIdentityM(mTranslationMatrix,0);
        Matrix.translateM(mTranslationMatrix, 0, mOffsetX, mOffsetY, 0);

        // Model = Scale * Rotate * Translate
        Matrix.multiplyMM(mIntermediateMatrix, 0, mScaleMatrix, 0, mRotationMatrix, 0);
        Matrix.multiplyMM(mModelMatrix, 0, mIntermediateMatrix, 0, mTranslationMatrix, 0);
    }

    protected void runOnGlThread(Runnable runnable) {
        mGlView.queueEvent(runnable);
    }

    protected void runOnUiThread(Runnable runnable) {
        if (AppSettings.appActivity != null) {
            AppSettings.appActivity.runOnUiThread(runnable);
        }
    }

    public float getAngle() {
        return mAngle;
    }

    public void setAngle(float angle) {
        mAngle = angle;
        updateModelMatrix();
    }

    public void setOffset(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        updateModelMatrix();
    }

    public void setScale(float scale) {
        mScaleFactor = scale;
        updateModelMatrix();
    }

    public void setGlView(GLSurfaceView glView) {
        this.mGlView = glView;
    }

    public void windowToWorld(int x, int y, PointF worldPoint) {
        final float WINDOW_Z_NEAR = 0.0f;
        final float WINDOW_Z_FAR = 1.0f;

        float rayStartPos[] = new float[4];
        float rayEndPos[] = new float[4];
        float modelView[] = new float[16];
        int viewport[] = {0, 0, mViewPortWidth, mViewPortHeight};

        // Model matrix is scale + rotation + translation
        Matrix.multiplyMM(modelView, 0, mViewMatrix, 0, mModelMatrix, 0);

        int windowX = x;
        int windowY = mViewPortHeight - y;

        int result = GLU.gluUnProject(windowX, windowY, WINDOW_Z_NEAR, modelView, 0, mProjectionMatrix, 0,
                viewport, 0, rayStartPos, 0);

        if (result == GL10.GL_TRUE) {
            rayStartPos[0] /= rayStartPos[3];
            rayStartPos[1] /= rayStartPos[3];
            rayStartPos[2] /= rayStartPos[3];
            System.arraycopy(rayStartPos, 0, mRay, 0, 3);
        }

        result = GLU.gluUnProject(windowX, windowY, WINDOW_Z_FAR, modelView, 0, mProjectionMatrix, 0,
                viewport, 0, rayEndPos, 0);

        if (result == GL10.GL_TRUE) {
            rayEndPos[0] /= rayEndPos[3];
            rayEndPos[1] /= rayEndPos[3];
            rayEndPos[2] /= rayEndPos[3];
            System.arraycopy(rayEndPos, 0, mRay, 3, 3);
        }

        worldPoint.x = mRay[0];
        worldPoint.y = mRay[1];
    }

    public void handleStartDrag(final int x, final int y, final FloorPlanView.Operation operation) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                windowToWorld(x, y, mDragStart);

                mAddedWallByDrag = false;

                if (operation == FloorPlanView.Operation.ADD_WALL) {
                    // Add new wall at the point
                    mSelectedWall = new Wall(mDragStart.x, mDragStart.y, mDragStart.x, mDragStart.y);
                    addPrimitive(mSelectedWall);
                    mAddedWallByDrag = true;
                    mSelectedWall.setColor(ColorUtils.setAlphaComponent(AppSettings.wallColor, ALPHA));
                }
                else {
                    mSelectedWall = mGlEngine.findWallHavingPoint(mDragStart.x, mDragStart.y);
                    if (mSelectedWall != null) {
                        mSelectedWall.setTapLocation(mDragStart.x, mDragStart.y);
                        mSelectedWall.setColor(ColorUtils.setAlphaComponent(AppSettings.wallColor, ALPHA));
                    }
                }

            }
        });
    }

    public void handleDrag(final int x, final int y) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                final PointF worldPoint = new PointF();
                windowToWorld(x, y, worldPoint);

                if (mSelectedWall != null) {
                    if (mGlEngine.getWallsNum() > 1) {
//                        // Aligns worldPoint!
//                        // TODO: Fix flickering with this method!!
//                        mGlEngine.alignChangeToExistingWalls(mSelectedWall, worldPoint);
                    }

                    if (mAddedWallByDrag) {
                        mSelectedWall.setB(worldPoint.x, worldPoint.y);
                    }
                    else {
                        mSelectedWall.handleChange(worldPoint.x, worldPoint.y);
                    }
                    mSelectedWall.updateVertices();
                    mGlEngine.updateSingleObject(mSelectedWall);
                    onWallLengthChanged(mSelectedWall);
                }
            }
        });
    }

    public void handleEndDrag(int x, int y) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                if (mSelectedWall != null) {
                    mSelectedWall.setColor(ColorUtils.setAlphaComponent(AppSettings.wallColor, OPAQUE));
                    mGlEngine.updateSingleObject(mSelectedWall);
                }
                mSelectedWall = null;
            }
        });
    }

    public void processWallDeletion(final int x, final int y) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                final PointF worldPoint = new PointF();
                windowToWorld(x, y, worldPoint);

                Wall candidate = mGlEngine.findWallHavingPoint(worldPoint.x, worldPoint.y);
                if (candidate != null) {
                    mGlEngine.removePrimitive(candidate);
                }
            }
        });
    }

    public void loadEngine() {
        runOnGlThread(new Runnable() {
            @Override
            public void run()  {
                if (mGlEngine == null) {
                    mGlEngine = new GlEngine(1000);
                    mGlEngine.allocateGpuBuffers();
                }

                if (mQueuedTaskForGlThread != null) {
                    mQueuedTaskForGlThread.run();
                }
            }
        });
    }

    private Runnable mQueuedTaskForGlThread = null;
    public void setFloorPlan(final List<IFloorPlanPrimitive> primitives) {
        mQueuedTaskForGlThread = new Runnable() {
            @Override
            public void run() {
                mGlEngine.setFloorPlan(primitives);
                refreshGpuBuffers();
            }
        };
    }

    public void addPrimitive(IFloorPlanPrimitive primitive) {
        mGlEngine.registerPrimitive(primitive);

        refreshGpuBuffers();
    }

    private void refreshGpuBuffers() {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                // Refresh GPU buffers with added wall
                // TODO: This could introduce a performance issue (on each touch all the map
                // TODO: is rewritten in GPU memory)
                mGlEngine.deallocateGpuBuffers();
                mGlEngine.allocateGpuBuffers();
            }
        });
    }

    private static final PointF mPanStart = new PointF();
    public void handleStartPan(final int x, final int y) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                windowToWorld(x, y, mPanStart);
            }
        });
    }

    private static final PointF mCurrentPan = new PointF();
    public void handlePan(final int x, final int y) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                windowToWorld(x, y, mCurrentPan);
                float dx = mCurrentPan.x - mPanStart.x;
                float dy = mCurrentPan.y - mPanStart.y;
                mOffsetX += dx;
                mOffsetY += dy;
                updateModelMatrix();
            }
        });
    }

    public List<IFloorPlanPrimitive> getFloorPlan() {
        return mGlEngine.getFloorPlan();
    }

    private IWallLengthChangedListener mWallLengthChangedListener = null;
    public void setOnWallLengthChangedListener(IWallLengthChangedListener listener) {
        this.mWallLengthChangedListener = listener;
    }
    private void onWallLengthChanged(final Wall wall) {
        if (mWallLengthChangedListener != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PointF wallVector = new PointF();
                    wallVector.set(wall.getB());
                    wallVector.offset(-wall.getA().x, -wall.getA().y);

                    mWallLengthChangedListener.onWallLengthChanged(wallVector.length());
                }
            });
        }
    }

    public void putStep(final float x, final float y) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                Footprint footprint = new Footprint(x, y);
                footprint.setColor(AppSettings.footprintColor);
                addPrimitive(footprint);
            }
        });
    }

    public void putMark(final float x, final float y, final WiFiTug.Fingerprint wifiFingerprint) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                WifiMark mark = new WifiMark(x, y, wifiFingerprint);
                addPrimitive(mark);
            }
        });
    }

    public void drawCurrentLocation(final PointF currentLocation) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                if (mLocationMark == null) {
                    mLocationMark = new LocationMark(currentLocation);
                    mLocationMark.setColor(AppSettings.locationMarkColor);
                    addPrimitive(mLocationMark);
                }
                else {
                    mLocationMark.setCenter(currentLocation);
                    mLocationMark.updateVertices();
                    mGlEngine.updateSingleObject(mLocationMark);
                }
            }
        });
    }
}

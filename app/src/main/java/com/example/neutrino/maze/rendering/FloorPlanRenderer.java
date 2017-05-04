package com.example.neutrino.maze.rendering;

import android.graphics.Color;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.support.v4.graphics.ColorUtils;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.LocationMark;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.WiFiLocator.WiFiFingerprint;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by neutrino on 7/2/2016.
 */
public class FloorPlanRenderer implements GLSurfaceView.Renderer {

    public static final int ALPHA = 128;
    public static final int OPAQUE = 255;
    static final float DEFAULT_SCALE_FACTOR = 0.1f;
    private static final int DEFAULT_BUFFER_VERTICES_NUM = 65536;

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
    private List<GlRenderBuffer> mGlBuffers = new ArrayList<>();
    private GlRenderBuffer mCurrentBuffer = null;
    private int mViewPortWidth;
    private int mViewPortHeight;
    private final PointF mDragStart = new PointF();
    private Wall mSelectedWall;
    private boolean mAddedWallByDrag;
    private static final float[] mBgColorF = new float[4];
    private LocationMark mLocationMark = null;
    private List<IFloorPlanPrimitive> mFloorPlanPrimitives;
    private List<Tag> mTags;

    private GLText glText;

    static private String vertexShaderCode;
    static private String fragmentShaderCode;
    static private String textRenderVertexShaderCode;
    static private String textRenderFragmentShaderCode;

    static {
        vertexShaderCode = readResourceAsString("/res/raw/vertex_shader.glsl");
        fragmentShaderCode = readResourceAsString("/res/raw/fragment_shader.glsl");
        textRenderVertexShaderCode = readResourceAsString("/res/raw/text_render_vertex_shader.glsl");
        textRenderFragmentShaderCode = readResourceAsString("/res/raw/text_render_fragment_shader.glsl");
    }

    // Used for debug only to show distribution of active fingerprints
    private LocationMark mDistributionIndicator;

    private static String readResourceAsString(String path) {
        Exception innerException;
        Class<? extends FloorPlanRenderer> aClass = FloorPlanRenderer.class;
        InputStream inputStream = aClass.getResourceAsStream(path);

        byte[] bytes;
        try {
            bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            innerException = e;
        }
        throw new RuntimeException("Cannot load shader code from resources.", innerException);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background frame color
        VectorHelper.colorTo3F(AppSettings.mapBgColor, mBgColorF);
        GLES20.glClearColor(mBgColorF[0], mBgColorF[1], mBgColorF[2], mBgColorF[3]);
        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

//        GLES20.glDepthFunc(GLES20.GL_LESS);
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Initialize the accumulated scale, rotation & translation matrices
        Matrix.setIdentityM(mScaleMatrix, 0);
        Matrix.setIdentityM(mRotationMatrix, 0);
        Matrix.setIdentityM(mTranslationMatrix, 0);

        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 3.0f;

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

        AppSettings.oglProgram = ShaderHelper.createAndLinkProgram(vertexShaderCode, fragmentShaderCode,
                ShaderHelper.POSITION_ATTRIBUTE, ShaderHelper.COLOR_ATTRIBUTE);

        AppSettings.oglTextRenderProgram = ShaderHelper.createAndLinkProgram(textRenderVertexShaderCode, textRenderFragmentShaderCode,
                ShaderHelper.POSITION_ATTRIBUTE, ShaderHelper.TEXTURE_COORDINATE_ATTRIBUTE, ShaderHelper.MVP_MATRIX_INDEX_ATTRIBUTE);

        mCurrentBuffer = new GlRenderBuffer(DEFAULT_BUFFER_VERTICES_NUM);
        mGlBuffers.add(mCurrentBuffer);

        if (mQueuedTaskForGlThread != null) {
            mQueuedTaskForGlThread.run();
        }

        // Create the GLText
        glText = new GLText(AppSettings.oglTextRenderProgram, AppSettings.appActivity.getAssets());

        // Load the font from file (set size + padding), creates the texture
        // NOTE: after a successful call to this the font is ready for rendering!
        glText.setScale(0.03f);
        glText.load( "Roboto-Regular.ttf", 36, 0, 0);  // Create Font (Height: 14 Pixels / X+Y Padding 2 Pixels)
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

        // Clear frame
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(AppSettings.oglProgram);

        for (GlRenderBuffer glBuffer : mGlBuffers) {
            glBuffer.render(mScratch);
        }

        renderTags();
    }

    public void addPrimitive(IFloorPlanPrimitive primitive) {
        if (!mCurrentBuffer.put(primitive)) {
            mCurrentBuffer = new GlRenderBuffer(DEFAULT_BUFFER_VERTICES_NUM);
            mGlBuffers.add(mCurrentBuffer);
            mCurrentBuffer.put(primitive);
        }

        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                mCurrentBuffer.allocateGpuBuffers();
            }
        });
        mFloorPlanPrimitives.add(primitive);
    }

    private void addPrimitives(List<IFloorPlanPrimitive> primitives) {
        for (IFloorPlanPrimitive primitive : primitives) {
            primitive.updateVertices();

            if (!mCurrentBuffer.put(primitive)) {
                mCurrentBuffer.allocateGpuBuffers();
                mCurrentBuffer = new GlRenderBuffer(DEFAULT_BUFFER_VERTICES_NUM);
                mGlBuffers.add(mCurrentBuffer);
                mCurrentBuffer.put(primitive);
            }
        }
        mCurrentBuffer.allocateGpuBuffers();

        mFloorPlanPrimitives = primitives;
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

    public boolean isFloorPlanSet() {return mFloorPlanPrimitives != null;}

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

    // Only for NONE mode (moving existing wall).
    public boolean startDragHandled() {
        return mSelectedWall != null;
    }

    public void handleStartDrag(final int x, final int y, final FloorPlanView.Operation operation) {
        // This is needed for FloorPlanView to know if there is any object under tap location
        windowToWorld(x, y, mDragStart);
        mSelectedWall = findWallHavingPoint(mDragStart.x, mDragStart.y);

        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                mAddedWallByDrag = false;

                if (operation == FloorPlanView.Operation.ADD_WALL) {
                    // Add new wall at the point
                    mSelectedWall = new Wall(mDragStart.x, mDragStart.y, mDragStart.x, mDragStart.y);
                    addPrimitive(mSelectedWall);
                    mAddedWallByDrag = true;
                    mSelectedWall.setColor(ColorUtils.setAlphaComponent(AppSettings.wallColor, ALPHA));
                }
                else {
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
                    if (mAddedWallByDrag) {
                        mSelectedWall.setB(worldPoint.x, worldPoint.y);
                    }
                    else {
                        mSelectedWall.handleChange(worldPoint.x, worldPoint.y);
                    }
                    mSelectedWall.updateVertices();
                    mSelectedWall.rewriteToBuffer();
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
                    mSelectedWall.rewriteToBuffer();
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

                Wall candidate = findWallHavingPoint(worldPoint.x, worldPoint.y);
                if (candidate != null) {
                    candidate.cloak();
                    mFloorPlanPrimitives.remove(candidate);
                }
            }
        });
    }
    // TODO: remove queued task and use simpler method to run this on start
    private Runnable mQueuedTaskForGlThread = null;

    public void setFloorPlan(final List<IFloorPlanPrimitive> primitives) {
        mQueuedTaskForGlThread = new Runnable() {
            @Override
            public void run() {
                addPrimitives(primitives);
                onFloorplanLoadComplete();
            }
        };
    }

    public void performQueuedTask() {
        if (mQueuedTaskForGlThread != null) {
            runOnGlThread(mQueuedTaskForGlThread);
            mQueuedTaskForGlThread = null;
        }
    }

    public void setTags(List<Tag> tags) {
        this.mTags = tags;
    }

    private void renderTags() {
        synchronized (FloorPlan.mTagsListLocker) {
            if (mTags == null || mTags.size() == 0) return;

            GLES20.glUseProgram(AppSettings.oglTextRenderProgram);
            glText.begin(0.0f, 0.0f, 1.0f, 1.0f, mScratch); // Begin Text Rendering (Set Color BLUE)

            for (Tag tag : mTags) {
                final PointF tagLocation = tag.getLocation();
                glText.drawC(tag.getLabel(), tagLocation.x, tagLocation.y, -mAngle);
            }
            glText.end();                                    // End Text Rendering
        }
    }

    public Wall findWallHavingPoint(float x, float y) {
        for (IFloorPlanPrimitive primitive : mFloorPlanPrimitives) {
            if (primitive instanceof Wall) {
                Wall wall = (Wall) primitive;
                if (!wall.isRemoved() && wall.hasPoint(x, y)) {
                    return wall;
                }
            }
        }

        return null;
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
        return mFloorPlanPrimitives;
    }

    public void putStep(final float x, final float y) {
        Footprint footprint = new Footprint(x, y);
        footprint.setColor(AppSettings.footprintColor);
        addPrimitive(footprint);
    }

    public Fingerprint putMark(final float x, final float y, final WiFiFingerprint wifiFingerprint) {
        Fingerprint mark = new Fingerprint(x, y, wifiFingerprint);
        addPrimitive(mark);
        return mark;
    }

    public void drawLocationMarkAt(final PointF currentLocation) {
        if (mLocationMark == null) {
            mLocationMark = new LocationMark(currentLocation);
            mLocationMark.setColor(AppSettings.locationMarkColor);
            addPrimitive(mLocationMark);
        }
        else {
            mLocationMark.setCenter(currentLocation);
            mLocationMark.updateVertices();
            runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    mLocationMark.rewriteToBuffer();
                }
            });
        }
    }

    public void drawDistribution(final PointF mean, final float stdev) {
        if (AppSettings.inDebug) {
            float innerRadius = Math.max(0, stdev - 1);

            if (mDistributionIndicator == null) {
                mDistributionIndicator = new LocationMark(mean.x, mean.y, innerRadius, stdev);
                mDistributionIndicator.setColor(Color.GREEN);
                addPrimitive(mDistributionIndicator);
            } else {
                mDistributionIndicator.setCenter(mean);
                mDistributionIndicator.setInnerRadius(innerRadius);
                mDistributionIndicator.setOuterRadius(stdev);
                mDistributionIndicator.updateVertices();
                runOnGlThread(new Runnable() {
                    @Override
                    public void run() {
                        mLocationMark.rewriteToBuffer();
                    }
                });
            }
        }
    }

    public void renderPrimitive(final IFloorPlanPrimitive primitive) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                primitive.rewriteToBuffer();
            }
        });
    }

    public void clearFloorPlan() {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                for(IFloorPlanPrimitive primitive : mFloorPlanPrimitives) {
                    if (!primitive.isRemoved()) { // TODO: check if this is always true
                        primitive.cloak();
                    }
                }
                for (GlRenderBuffer buffer : mGlBuffers) {
                    buffer.deallocateGpuBuffers();
                }
                mGlBuffers.clear();
                mFloorPlanPrimitives.clear();
            }
        });
    }

    public void highlightCentroidMarks(List<Fingerprint> centroidMarks) {
        List<? extends IFloorPlanPrimitive> primitives = mFloorPlanPrimitives;
        for (IFloorPlanPrimitive primitive : primitives) {
            if (primitive instanceof Fingerprint) {
                final Fingerprint mark = (Fingerprint) primitive;
                if (centroidMarks.contains(mark))
                    mark.setColor(Color.RED);
                else
                    mark.setColor(Color.BLUE);
                runOnGlThread(new Runnable() {
                    @Override
                    public void run() {
                        mark.rewriteToBuffer();
                    }
                });
            }
        }
    }

    public void rescaleFloorplan(float scaleFactor) {
        if (mGlBuffers == null) return; // TODO: this should be fixed somehow
        List<? extends IFloorPlanPrimitive> primitives = mFloorPlanPrimitives;

        for (final IFloorPlanPrimitive primitive : primitives) {
            if (primitive.isRemoved()) continue; // Do not alter removed primitives
            primitive.scaleVertices(scaleFactor);
            primitive.updateVertices();
            runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    primitive.rewriteToBuffer();
                }
            });
        }
    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getOffsetY() {
        return mOffsetY;
    }
    public PointF getMapAnyVertex() {
        if (mFloorPlanPrimitives != null && mFloorPlanPrimitives.size() > 0) {
            return mGlBuffers.get(0).getFirstVertex();
        }
        return new PointF();
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

    public void createNewTag(final int x, final int y, final String label) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                PointF location = new PointF();
                windowToWorld(x, y, location);
                Tag newTag = new Tag(location, label);
                synchronized (FloorPlan.mTagsListLocker) {
                    mTags.add(newTag);
                }
            }
        });
    }

    /**
     * Created by Greg Stein on 9/19/2016.
     */
    public static interface IWallLengthChangedListener {
        void onWallLengthChanged(float wallLength);
    }

    private IFloorplanLoadCompleteListener mFloorplanLoadCompleteListener = null;
    public void setOnFloorplanLoadCompleteListener(IFloorplanLoadCompleteListener listener) {
        this.mFloorplanLoadCompleteListener = listener;
    }

    private void onFloorplanLoadComplete() {
        if (mFloorplanLoadCompleteListener != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFloorplanLoadCompleteListener.onFloorplanLoadComplete();
                }
            });
        }
    }

    /**
     * Created by Greg Stein on 9/19/2016.
     */
    public static interface IFloorplanLoadCompleteListener {
        void onFloorplanLoadComplete();
    }
}

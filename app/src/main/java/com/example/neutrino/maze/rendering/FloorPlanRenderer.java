package com.example.neutrino.maze.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.floorplan.Fingerprint;
import com.example.neutrino.maze.floorplan.FloorPlan;
import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.IMoveable;
import com.example.neutrino.maze.floorplan.LocationMark;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.floorplan.Wall;
import com.example.neutrino.maze.util.IFuckingSimpleCallback;

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
    private List<RenderGroup> mRenderGroups = new ArrayList<>();
    private GlRenderBuffer mCurrentBuffer = null;
    private int mViewPortWidth;
    private int mViewPortHeight;
    private final PointF mDragStart = new PointF();
    private IMoveable mMovedObject;
    private boolean mAddedWallByDrag;
    private static final float[] mBgColorF = new float[4];
    private LocationMark mLocationMark = null;
    private List<Tag> mTags;
    private Context mContext;

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
    private FloorPlan mFloorPlan;
    private Object mFloorPlanLock = new Object();

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

    public FloorPlanRenderer(Context context) {
        mContext = context;
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

        // Create the GLText
        glText = new GLText(AppSettings.oglTextRenderProgram, mContext.getAssets());

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

        for (RenderGroup group : mRenderGroups) {
            if (group.isReadyForRender()) {
                group.render(mScratch);
            }
        }

        renderTags();
    }

    private void addElementsToRenderGroup(List<IFloorPlanPrimitive> elements, RenderGroup group) {
    }

    public RenderGroup renderElements(List<IFloorPlanPrimitive> elements) {
        final RenderGroup newGroup = new RenderGroup(elements);

        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                newGroup.prepareForRender();
            }
        });

        mRenderGroups.add(newGroup);
        return newGroup;
    }

    public synchronized void addPrimitive(IFloorPlanPrimitive primitive) {
//        if (!mCurrentBuffer.put(primitive)) {
//            mCurrentBuffer = new GlRenderBuffer(GlRenderBuffer.DEFAULT_BUFFER_VERTICES_NUM);
//            mGlBuffers.add(mCurrentBuffer);
//            mCurrentBuffer.put(primitive);
//        }
//
//        runOnGlThread(new Runnable() {
//            @Override
//            public void run() {
//                mCurrentBuffer.allocateGpuBuffers();
//            }
//        });
//        mFloorPlan.addElement(primitive);
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
        if (mContext instanceof Activity) {
            ((Activity)mContext).runOnUiThread(runnable);
        }
    }

    public boolean isFloorPlanEmpty() {return mFloorPlan == null || mFloorPlan.getSketch().isEmpty();}

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
        return mMovedObject != null;
    }

    public void handleStartDrag(final int x, final int y, final FloorPlanView.MapOperation operation, final FloorPlanView.MapOperand operand) {
        // This is needed for FloorPlanView to know if there is any object under tap location
        windowToWorld(x, y, mDragStart);
        mMovedObject = mFloorPlan.findObjectHavingPoint(mDragStart.x, mDragStart.y);

        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                mAddedWallByDrag = false;

                // TODO: This code smells bad. Obviously refactor is needed. Look at where it is called from!
                if (operation == FloorPlanView.MapOperation.ADD && operand == FloorPlanView.MapOperand.WALL) {
                    // Add new wall at the point
                    mMovedObject = new Wall(mDragStart.x, mDragStart.y, mDragStart.x, mDragStart.y);
                    addPrimitive((IFloorPlanPrimitive) mMovedObject);
                    mAddedWallByDrag = true;
                    mMovedObject.setTapLocation(mDragStart.x, mDragStart.y);
                    mMovedObject.handleMoveStart();
                    onStartMove(mMovedObject);
                }
                else {
                    if (mMovedObject != null) {
                        mMovedObject.setTapLocation(mDragStart.x, mDragStart.y);
                        mMovedObject.handleMoveStart();
                        onStartMove(mMovedObject);
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

                if (mMovedObject != null) {
                    mMovedObject.handleMove(worldPoint.x, worldPoint.y);
                    // TODO: Handle This for non-wall
                    onMoving(mMovedObject);
                }
            }
        });
    }

    public void handleEndDrag(int x, int y) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                if (mMovedObject != null) {
                    mMovedObject.handleMoveEnd();
                    onEndMove(mMovedObject);
                }
                mMovedObject = null;
            }
        });
    }

    public void processObjectDeletion(final int x, final int y) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                final PointF worldPoint = new PointF();
                windowToWorld(x, y, worldPoint);

                IMoveable candidate = mFloorPlan.findObjectHavingPoint(worldPoint.x, worldPoint.y);
                if (candidate == null) return;

                if (candidate instanceof IFloorPlanPrimitive) {
                    IFloorPlanPrimitive candidatePrimitive = (IFloorPlanPrimitive) candidate;
                    candidatePrimitive.cloak();
                    mFloorPlan.removeElement(candidatePrimitive);
                } else if (candidate instanceof Tag) {
                    mTags.remove(candidate);
                }
            }
        });
    }

    public FloorPlan getFloorPlan() {
        return mFloorPlan;
    }

    public void setTags(List<Tag> tags) {
        this.mTags = tags;
        for (Tag tag : mTags) {
            calculateTagBoundaries(tag);
        }
    }

    private void renderTags() {
        synchronized (FloorPlan.mTagsListLocker) {
            if (mTags == null || mTags.size() == 0) return;

            GLES20.glUseProgram(AppSettings.oglTextRenderProgram);
            glText.begin(0.0f, 0.0f, 1.0f, 1.0f, mScratch); // Begin Text Rendering (Set Color BLUE)

            for (Tag tag : mTags) {
                drawTag(tag);
            }
            glText.end();                                   // End Text Rendering
        }
    }

    public void calculateTagBoundaries(Tag tag) {
        tag.setRenderedTextWidth(glText.getLength(tag.getLabel()));
        tag.setRenderedTextHeight(glText.getCharHeight());
        tag.updateBoundariesRect();
    }

    private void drawTag(Tag tag) {
        PointF tagLocation = tag.getLocation();
        float[] boundaries = tag.getBoundaryCorners();
        float[] boundariesTransformed = tag.getBoundaryCornersTransformed();

        android.graphics.Matrix m = new android.graphics.Matrix();
        m.setRotate(-mAngle, tagLocation.x, tagLocation.y);
        m.mapPoints(boundariesTransformed, boundaries);

        glText.draw(tag.getLabel(), boundariesTransformed[6], boundariesTransformed[7], 0, 0, 0, -mAngle);  // Draw Text Centered
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

    public void putStep(final float x, final float y) {
        Footprint footprint = new Footprint(x, y);
        footprint.setColor(AppSettings.footprintColor);
        addPrimitive(footprint);
    }

    public void putFingerprint(Fingerprint fingerprint) {
        addPrimitive(fingerprint);
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
        List<IFloorPlanPrimitive> floorPlanElements = mFloorPlan.getSketch();
        for(IFloorPlanPrimitive primitive : floorPlanElements) {
            if (!primitive.isRemoved()) { // TODO: check if this is always true
                primitive.cloak();
            }
        }
        for (RenderGroup group : mRenderGroups) {
            group.glDeallocate();
        }
//        for (GlRenderBuffer buffer : mGlBuffers) {
//            buffer.deallocateGpuBuffers();
//        }
//        mGlBuffers.clear();
//        mFloorPlan.clear();
//        mCurrentBuffer = new GlRenderBuffer(GlRenderBuffer.DEFAULT_BUFFER_VERTICES_NUM);
//        mGlBuffers.add(mCurrentBuffer);
    }

    public void highlightCentroidMarks(List<Fingerprint> centroidMarks) {
        List<? extends IFloorPlanPrimitive> primitives = mFloorPlan.getSketch();
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

//    public void rescaleFloorplan(float scaleFactor) {
//        if (mGlBuffers == null) return; // TODO: this should be fixed somehow
//        List<? extends IFloorPlanPrimitive> primitives = mFloorPlan.getSketch();
//
//        for (final IFloorPlanPrimitive primitive : primitives) {
//            if (primitive.isRemoved()) continue; // Do not alter removed primitives
//            primitive.scaleVertices(scaleFactor);
//            primitive.updateVertices();
//            runOnGlThread(new Runnable() {
//                @Override
//                public void run() {
//                    primitive.rewriteToBuffer();
//                }
//            });
//        }
//    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getOffsetY() {
        return mOffsetY;
    }

    private IWallLengthChangedListener mWallLengthChangedListener = null;
    private IWallLengthChangedListener mWallLengthStartChangingListener = null;
    private IFuckingSimpleCallback mWallLengthEndChangingListener = null;

    public void setOnWallLengthChangedListener(IWallLengthChangedListener listener) {
        this.mWallLengthChangedListener = listener;
    }

    public void setOnWallLengthStartChangingListener(IWallLengthChangedListener callback) {
        mWallLengthStartChangingListener = callback;
    }

    public void setOnWallLengthEndChangingListener(IFuckingSimpleCallback callback) {
        mWallLengthEndChangingListener = callback;
    }

    private void onStartMove(final IMoveable moved) {
        if ((mWallLengthStartChangingListener != null) && (moved instanceof Wall)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Wall wall = (Wall) moved;
                    float wallLength = wall.length();
                    mWallLengthStartChangingListener.onWallLengthChanged(wallLength);
                }
            });
        }
    }

    private void onMoving(final IMoveable moved) {
        if ((mWallLengthChangedListener != null) && (moved instanceof Wall)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Wall wall = (Wall) moved;
                        float wallLength = wall.length();
                        mWallLengthChangedListener.onWallLengthChanged(wallLength);
                    }
                });
        }
    }

    private void onEndMove(final IMoveable moved) {
        if ((mWallLengthEndChangingListener != null) && (moved instanceof Wall)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWallLengthEndChangingListener.onNotified();
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
                calculateTagBoundaries(newTag);
                synchronized (FloorPlan.mTagsListLocker) {
                    mTags.add(newTag);
                }
            }
        });
    }

    public void addNewTag(final Tag tag) {
        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                calculateTagBoundaries(tag);
                synchronized (FloorPlan.mTagsListLocker) {
                    mTags.add(tag);
                }
            }
        });
    }

    /**
     * Created by Greg Stein on 9/19/2016.
     */
    public interface IWallLengthChangedListener {
        void onWallLengthChanged(float wallLength);
    }
}

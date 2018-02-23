package com.example.neutrino.maze.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.support.v4.util.Pair;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.core.IMainView;
import com.example.neutrino.maze.floorplan.Fingerprint;
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
    private static float[] mRay = new float[6]; // ray represented by 2 points in 3D

    private GLSurfaceView mGlView;
    private List<IRenderGroup> mRenderGroups = new ArrayList<>();
    private int mViewPortWidth;
    private int mViewPortHeight;
    private final PointF mDragStart = new PointF();
    private IMoveable mMovedObject;
    private IRenderGroup mMovedObjectGroup;
    private static final float[] mBgColorF = new float[4];
    private LocationMark mLocationMark = null;

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
    private IMainView.IElementFactory mElementFactory;

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

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA/*GL_ONE*/, GLES20.GL_ONE_MINUS_SRC_ALPHA);

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

        for (IRenderGroup group : mRenderGroups) {
            if (group.isReadyForRender()) {
                group.render(mScratch, mAngle);
            } else {
                group.prepareForRender();
            }
        }
    }

    // TODO: refactor to use single method which receives IRenderGroup
    public void render(IRenderGroup group) {
        mRenderGroups.add(group); // Lock could be required
    }

    public ElementsRenderGroup renderElements(List<IFloorPlanPrimitive> elements) {
        final ElementsRenderGroup newGroup = new ElementsRenderGroup(elements);
        mRenderGroups.add(newGroup);

        return newGroup;
    }

    public TextRenderGroup renderTags(List<Tag> tags) {
        final TextRenderGroup newGroup = new TextRenderGroup(tags, glText);
        mRenderGroups.add(newGroup);

        return newGroup;
    }

    @Deprecated
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
//        mFloorPlan.addItem(primitive);
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

    public boolean isSketchEmpty() {return mRenderGroups == null || mRenderGroups.isEmpty() || mRenderGroups.get(0).isEmpty();}

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

    public void setElementFactory(IMainView.IElementFactory factory) {
        this.mElementFactory = factory;
    }

    // TODO: Check in the caller that there is an object at tap location, if not create it (if
    // TODO: requested) and call this method only after there is an existing object. Merge both
    // TODO: if/then (in the code of the method) into one block.
    public void handleStartDrag(final int x, final int y, final IMainView.MapOperation operation, final IMainView.MapOperand operand) {
        // This is needed for FloorPlanView to know if there is any object under tap location
        windowToWorld(x, y, mDragStart);
        final Pair<IRenderGroup, IMoveable> objectHavingPointInfo = findObjectHavingPoint(mDragStart);
        mMovedObject = (objectHavingPointInfo == null)? null : objectHavingPointInfo.second;
        mMovedObjectGroup = (objectHavingPointInfo == null)? null : objectHavingPointInfo.first;

        runOnGlThread(new Runnable() {
            @Override
            public void run() {
                // TODO: This code smells bad. Obviously refactor is needed. Look at where it is called from!
                if (operation == IMainView.MapOperation.ADD && operand == IMainView.MapOperand.WALL) {
                    // Add new wall at the point
                    mMovedObject = mElementFactory.createElement(operand, mDragStart); //new Wall(mDragStart.x, mDragStart.y, mDragStart.x, mDragStart.y);
                    mMovedObject.setTapLocation(mDragStart.x, mDragStart.y);
                    mMovedObject.handleMoveStart();
                    onStartMove(mMovedObject);
                }
                else {
                    if (mMovedObject != null) {
                        mMovedObject.setTapLocation(mDragStart.x, mDragStart.y);
                        mMovedObject.handleMoveStart();
                        onStartMove(mMovedObject);
                        // Mark render group of moved element as changed
                        objectHavingPointInfo.first.setChanged(true);
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
                    mMovedObjectGroup.setChanged(true);
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

                Pair<IRenderGroup, IMoveable> candidate = findObjectHavingPoint(worldPoint);
                if (candidate == null) return;

                IRenderGroup candidateGroup = candidate.first;
                IMoveable candidateElement = candidate.second;

                // In case of render element, make it invisible first (it will stay in GPU buffers)
                if (candidate.second instanceof IFloorPlanPrimitive) {
                    IFloorPlanPrimitive candidatePrimitive = (IFloorPlanPrimitive) candidate.second;
                    candidatePrimitive.cloak();
                }
                candidateGroup.removeElement(candidateElement);
                candidateGroup.setChanged(true); // mark for later update to server
            }
        });
    }

    public void calculateTagBoundaries(Tag tag) {
        tag.setRenderedTextWidth(glText.getLength(tag.getLabel()));
        tag.setRenderedTextHeight(glText.getCharHeight());
        tag.updateBoundariesRect();
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

    public void clearSketch() {
        for (IRenderGroup group : mRenderGroups) {
            group.clear();
            group.glDeallocate();
        }
    }

    // TODO: This method should be moved elsewhere (maybe into controller)
    public void highlightCentroidMarks(List<Fingerprint> centroidMarks) {
//        List<? extends IFloorPlanPrimitive> primitives = mFloorPlan.getSketch();
//        for (IFloorPlanPrimitive primitive : primitives) {
//            if (primitive instanceof Fingerprint) {
//                final Fingerprint mark = (Fingerprint) primitive;
//                if (centroidMarks.contains(mark))
//                    mark.setColor(Color.RED); // Todo: highlighted fingerprint color define in AppSettings
//                else
//                    mark.setColor(Color.BLUE); // Todo: fingerprint color define in AppSettings
//                runOnGlThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mark.rewriteToBuffer();
//                    }
//                });
//            }
//        }
    }

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

    public Tag createNewTag(PointF worldPoint, String label) {
        return (Tag)mElementFactory.createElement(IMainView.MapOperand.LOCATION_TAG, worldPoint, label);
    }

    // Returns IMoveable element under given coords and its render group
    public Pair<IRenderGroup, IMoveable> findObjectHavingPoint(PointF p) {
        for (IRenderGroup group : mRenderGroups) {
            final IMoveable elementHavingPoint = group.findElementHavingPoint(p);
            if (elementHavingPoint != null) {
                return new Pair<>(group, elementHavingPoint);
            }
        }

        return null;
    }

    public Pair<IRenderGroup, IMoveable> findObjectHavingPoint(PointF p, Class objectType) {
        for (IRenderGroup group : mRenderGroups) {
            final IMoveable elementHavingPoint = group.findElementHavingPoint(p);
            if (elementHavingPoint != null && objectType.isInstance(elementHavingPoint)) {
                return new Pair<>(group, elementHavingPoint);
            }
        }

        return null;
    }

    /**
     * Created by Greg Stein on 9/19/2016.
     */
    public interface IWallLengthChangedListener {
        void onWallLengthChanged(float wallLength);
    }
}

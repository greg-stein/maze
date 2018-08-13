package world.maze.core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by Greg Stein on 4/11/2017.
 */

public class SensorListener implements SensorEventListener {
    /*
     * time smoothing constant for low-pass filter
     * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    private static final float LOW_PASS_ALPHA = 0.5f;

    private static SensorListener instance = null;
    private static final Object mutex = new Object();
    private boolean mActive;

    public static SensorListener getInstance(Context context) {

        if (instance == null) {
            synchronized (mutex) {
                if (instance == null)
                    instance = new SensorListener(context);
            }
        }
        return instance;
    }

    private List<IDeviceRotationListener> mDeviceRotationEventListeners = new ArrayList<>();
    private List<IStepDetectedListener> mStepDetectedEventListeners = new ArrayList<>();
    private List<IGravityChangedListener> mGravityChangedEventListeners = new ArrayList<>();

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private Sensor mGravity;
    private Sensor mRotation;
    private Sensor mStepDetector;

    private boolean mHaveAccelerometer;
    private boolean mHaveMagnetometer;
    private boolean mHaveGravity;
    private boolean mHaveRotation;
    private boolean mHaveStepDetector;

    private float[] mGravitySensorRawData;
    private float[] mGeomagneticSensorRawData;
    private static final float[] mGravitySensorRawDataAugmented = new float[4];
    private static final float[] mGravitySensorAdjustedData = new float[4];
    private static final float[] mRotationMatrix = new float[16];   // 4x4 for compatibility with OpenGL
    private static final float[] mInclinationMatrix = new float[9];
    private static final float[] mOrientation = new float[3];

    private SensorListener(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    private boolean subscribersExist() {
        if (mStepDetectedEventListeners.size() > 0) return true;
        if (mDeviceRotationEventListeners.size() > 0) return true;
        return false;
    }

    public void resume() {
        // for the system's orientation sensor registered listeners
        mHaveRotation = mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_GAME);
        if (!mHaveRotation) {
            mHaveGravity = mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_GAME);

            // if there is a gravity sensor we do not need the accelerometer
            if (!mHaveGravity) {
                mHaveAccelerometer = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
            mHaveMagnetometer = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        }

        mHaveStepDetector = mSensorManager.registerListener(this, mStepDetector, SensorManager.SENSOR_DELAY_UI);
        mActive = true;
    }

    public void pause() {
        mSensorManager.unregisterListener(this);
        mActive = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean gotRotationMatrix = false;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY: {
                mGravitySensorRawData = lowPass(event.values, mGravitySensorRawData);
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                mGravitySensorRawData = lowPass(event.values, mGravitySensorRawData);
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD: {
                mGeomagneticSensorRawData = lowPass(event.values, mGeomagneticSensorRawData);
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR: {
                // calculate the rotation matrix
                SensorManager.getRotationMatrixFromVector( mRotationMatrix, event.values );
                gotRotationMatrix = true;
                break;
            }
            case Sensor.TYPE_STEP_DETECTOR: {
                emitStepDetectedEvent();
            }
        }

        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
            if (mGravitySensorRawData != null && mGeomagneticSensorRawData != null) {
                gotRotationMatrix = SensorManager.getRotationMatrix(mRotationMatrix, mInclinationMatrix,
                        mGravitySensorRawData, mGeomagneticSensorRawData);
            }
        }

        if (gotRotationMatrix) {
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            double degree = Math.toDegrees(mOrientation[0]);
            emitDeviceRotationEvent(degree);
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER || event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                mGravitySensorRawDataAugmented[0] = mGravitySensorRawData[0];
                mGravitySensorRawDataAugmented[1] = mGravitySensorRawData[1];
                mGravitySensorRawDataAugmented[2] = mGravitySensorRawData[2];
                // according to getRotationMatrix documentation: [0 0 g] = R * gravity (g = magnitude of gravity)
                // the third element (Z coordinate) of the resultant vector is the current magnitude of acceleration in the earth Z axis)
                Matrix.multiplyMV(mGravitySensorAdjustedData, 0, mRotationMatrix, 0, mGravitySensorRawDataAugmented, 0);
                emitGravityChangedEvent(mGravitySensorAdjustedData[2]);
            }
        }
    }

    public boolean isActive() {
        return mActive;
    }

    public interface IDeviceRotationListener {
        void onDeviceRotated(double degree);
    }

    public void addDeviceRotationListener(IDeviceRotationListener listener) {
        if (!mDeviceRotationEventListeners.contains(listener)) {
            mDeviceRotationEventListeners.add(listener);
        }
        if (!isActive()) resume();
    }

    public void removeDeviceRotationListener(IDeviceRotationListener listener) {
        mDeviceRotationEventListeners.remove(listener);
        if (!subscribersExist()) pause();
    }

    private void emitDeviceRotationEvent(double degree) {
        for (IDeviceRotationListener listener : mDeviceRotationEventListeners) {
            listener.onDeviceRotated(degree);
        }
    }

    public interface IStepDetectedListener {
        void onStepDetected();
    }

    public void addStepDetectedListener(IStepDetectedListener listener) {
        if (!mStepDetectedEventListeners.contains(listener)) {
            mStepDetectedEventListeners.add(listener);
        }
        if (!isActive()) resume();
    }

    public void removeStepDetectedListener(IStepDetectedListener listener) {
        if (mStepDetectedEventListeners.remove(listener)) {
            if (!subscribersExist()) pause();
        }
    }

    private void emitStepDetectedEvent() {
        for (IStepDetectedListener listener : mStepDetectedEventListeners) {
            listener.onStepDetected();
        }
    }

    public interface IGravityChangedListener {
        void onGravityChanged(float newGravity);
    }

    public void addGravityChangedListener(IGravityChangedListener listener) {
        mGravityChangedEventListeners.add(listener);
    }

    private void emitGravityChangedEvent(float newGravity) {
        for (IGravityChangedListener listener : mGravityChangedEventListeners) {
            listener.onGravityChanged(newGravity);
        }
    }

    private static float[] lowPass( float[] newSensorData, float[] oldSensorData ) {
        if ( oldSensorData == null ) return newSensorData.clone();

        for ( int i=0; i < newSensorData.length; i++ ) {
            oldSensorData[i] = newSensorData[i] + LOW_PASS_ALPHA * (oldSensorData[i] - newSensorData[i]);
        }
        return oldSensorData;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

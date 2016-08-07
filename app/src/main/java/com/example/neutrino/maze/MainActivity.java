package com.example.neutrino.maze;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // One human step
    private static final float STEP_LENGTH = 0.015f;

    // GUI-related fields
    private FloorPlanView uiFloorPlanView;
    private Toolbar uiToolbar;
    private FloatingActionButton uiFab;

    // Map north angle
    private float mapNorth = 0.0f;

    private float currentDegree = 0f;
    private float mOffsetX;
    private float mOffsetY;

    // device sensor manager
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private Sensor mGravity;
    private Sensor mRotation;
    private Sensor mStepDetector;
    private float[] mGravitySensorRawData;
    private float[] mGeomagneticSensorRawData;
    private static final float[] mRotationMatrix = new float[9];
    private static final float[] mInclinationMatrix = new float[9];
    private static final float[] mOrientation = new float[3];
    private boolean mHaveAccelerometer;
    private boolean mHaveMagnetometer;
    private boolean mHaveGravity;
    private boolean mHaveRotation;
    private boolean mHaveStepDetector;


    /*
     * time smoothing constant for low-pass filter
     * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    static final float LOW_PASS_ALPHA = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiFloorPlanView = (FloorPlanView) findViewById(R.id.ui_MapContainer);
        uiToolbar = (Toolbar) findViewById(R.id.toolbar);
        uiFab = (FloatingActionButton) findViewById(R.id.fab);
        setSupportActionBar(uiToolbar);

        uiFab.setOnTouchListener(new View.OnTouchListener() {
                                     @Override
                                     public boolean onTouch(View view, MotionEvent motionEvent) {
                                         uiFloorPlanView.loadEngine();
                                         return true;
                                     }
                                 }
        );

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mHaveRotation = mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_UI);
        if (!mHaveRotation) {
            mHaveGravity = mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_UI);

            // if there is a gravity sensor we do not need the accelerometer
            if (!mHaveGravity) {
                mHaveAccelerometer = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            mHaveMagnetometer = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
        }

        // Step detector
        mHaveStepDetector = mSensorManager.registerListener(this, mStepDetector, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    protected float[] lowPass( float[] newSensorData, float[] oldSensorData ) {
        if ( oldSensorData == null ) return newSensorData;

        for ( int i=0; i < newSensorData.length; i++ ) {
            oldSensorData[i] = newSensorData[i] + LOW_PASS_ALPHA * (oldSensorData[i] - newSensorData[i]);
        }
        return oldSensorData;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean gotRotationMatrix = false;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY: {
                mGravitySensorRawData = lowPass(event.values.clone(), mGravitySensorRawData);
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                mGravitySensorRawData = lowPass(event.values.clone(), mGravitySensorRawData);
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD: {
                mGeomagneticSensorRawData = lowPass(event.values.clone(), mGeomagneticSensorRawData);
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR: {
                // calculate the rotation matrix
                SensorManager.getRotationMatrixFromVector( mRotationMatrix, event.values );
                gotRotationMatrix = true;
                break;
            }
            case Sensor.TYPE_STEP_DETECTOR: {
                mOffsetX += (float) (Math.sin(Math.toRadians(currentDegree)) * STEP_LENGTH);
                mOffsetY += (float) (Math.cos(Math.toRadians(currentDegree)) * STEP_LENGTH);
                uiFloorPlanView.updateOffset(mOffsetX, mOffsetY);
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
            float degree = Math.round(Math.toDegrees(mOrientation[0]) + mapNorth);
            uiFloorPlanView.updateAngle(currentDegree - degree);
            currentDegree = degree;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }
}

package com.example.neutrino.maze;

import android.content.Context;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // One human step
    private static final float STEP_LENGTH = 0.78f; // 78cm
    private static final float WALL_CREATION_DISTANCE = STEP_LENGTH;

    private float mTravelledDistance = 0;

    // GUI-related fields
    private FloorPlanView uiFloorPlanView;
    private Toolbar uiToolbar;
    private ToggleButton uiModeSwitch;
    private FloatingActionButton uiFabDeleteWall;
    private FloatingActionButton uiFabAddWall;
    private FloatingActionButton uiFabSetLocation;
    private FloatingActionButton uiFabAutobuilderMode;
    private FloatingActionButton uiFabAutobuilderLeft;
    private FloatingActionButton uiFabAutobuilderRight;
    private TextView uiWallLengthText;

    // Map north angle
    private float mapNorth = 0.0f;

    private float currentDegree = 0f;
    private float mOffsetX;
    private float mOffsetY;

    private boolean mIsAutobuildModeEnabled = false;
    private boolean mStepDetectorInited;
    private boolean mAutobuilderFabsVisible = true;

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

    private WifiManager mWifiManager;
    private WifiScanner mWifiScanner;

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
        uiFabDeleteWall = (FloatingActionButton) findViewById(R.id.fab_delete_wall);
        uiFabAddWall = (FloatingActionButton) findViewById(R.id.fab_add_wall);
        uiFabSetLocation = (FloatingActionButton) findViewById(R.id.fab_set_location);
        uiFabAutobuilderMode = (FloatingActionButton) findViewById(R.id.fab_map_autobuilder);
        uiFabAutobuilderLeft = (FloatingActionButton) findViewById(R.id.fab_map_autobuilder_left);
        uiFabAutobuilderRight = (FloatingActionButton) findViewById(R.id.fab_map_autobuilder_right);
        uiModeSwitch = (ToggleButton) findViewById(R.id.tb_edit_mode);
        uiWallLengthText = (TextView) findViewById(R.id.tv_wall_length);

        setSupportActionBar(uiToolbar);
        getSupportActionBar().setTitle("");

        AppSettings.init(this); //getApplicationContext()
        setUiListeners();

        // initialize your android device sensor capabilities
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanner = new WifiScanner(mWifiManager);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    private void setUiListeners() {
        uiFloorPlanView.setOnWallLengthChangedListener(new IWallLengthChangedListener() {
            @Override
            public void onWallLengthChanged(float wallLength) {
                uiWallLengthText.setText(String.format(java.util.Locale.US,"Wall length: %.2fm", wallLength));
            }
        });

        ViewTreeObserver vto = uiFloorPlanView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (MazeServer.connectionAvailable(getApplicationContext())) {
                    MazeServer server = new MazeServer(getApplicationContext());
                    server.downloadFloorPlan(new MazeServer.AsyncResponse() {
                        @Override
                        public void processFinish(String jsonString) {
                            uiFloorPlanView.setFloorPlanAsJSon(jsonString);
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "No Internet connection", Toast.LENGTH_SHORT).show();
                }

                uiFloorPlanView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        uiModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                uiFloorPlanView.setMode(isChecked);
                if (isChecked) {
                    uiToolbar.setBackgroundColor(AppSettings.editModeColor);
                } else {
                    uiToolbar.setBackgroundColor(AppSettings.primaryColor);

                    if (MazeServer.connectionAvailable(getApplicationContext())) {
                        MazeServer server = new MazeServer(getApplicationContext());
                        server.uploadFloorPlan(uiFloorPlanView.getFloorPlanAsJSon());
                    } else {
                        Toast.makeText(getApplicationContext(), "No Internet connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        uiFabAutobuilderMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAutobuilderFabsVisible = !mAutobuilderFabsVisible;
                if (mAutobuilderFabsVisible) {
                    TypedValue outValue = new TypedValue();
                    getResources().getValue(R.dimen.alpha_default, outValue, true);
                    final float alpha = outValue.getFloat();

                    uiFabAutobuilderLeft.show(new FloatingActionButton.OnVisibilityChangedListener() {
                        @Override
                        public void onShown(FloatingActionButton fab) {
                            super.onShown(fab);
                            fab.setAlpha(alpha);
                        }
                    });
                    uiFabAutobuilderRight.show(new FloatingActionButton.OnVisibilityChangedListener() {
                        @Override
                        public void onShown(FloatingActionButton fab) {
                            super.onShown(fab);
                            fab.setAlpha(alpha);
                        }
                    });
                }
                else {
                    uiFabAutobuilderLeft.hide();
                    uiFabAutobuilderRight.hide();
                }
            }
        });

        uiFabAutobuilderLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uiFloorPlanView.autobuilderMode ^= uiFloorPlanView.BUILDER_MODE_LEFT;
                updateAutobuilderFabsState();
            }
        });

        uiFabAutobuilderRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uiFloorPlanView.autobuilderMode ^= uiFloorPlanView.BUILDER_MODE_RIGHT;
                updateAutobuilderFabsState();
            }
        });

        uiFabDeleteWall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uiFloorPlanView.operation == FloorPlanView.Operation.REMOVE_WALL) {
                    uiFloorPlanView.operation = FloorPlanView.Operation.NONE;
                }
                else {
                    uiFloorPlanView.operation = FloorPlanView.Operation.REMOVE_WALL;
                }
                updateOperationFabsState();
            }
        });


        uiFabAddWall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uiFloorPlanView.operation == FloorPlanView.Operation.ADD_WALL) {
                    uiFloorPlanView.operation = FloorPlanView.Operation.NONE;
                }
                else {
                    uiFloorPlanView.operation = FloorPlanView.Operation.ADD_WALL;
                }
                updateOperationFabsState();
            }
        });

        uiFabSetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uiFloorPlanView.operation == FloorPlanView.Operation.SET_LOCATION) {
                    uiFloorPlanView.operation = FloorPlanView.Operation.NONE;
                }
                else {
                    uiFloorPlanView.operation = FloorPlanView.Operation.SET_LOCATION;
                }
                updateOperationFabsState();
            }
        });

        uiFabAutobuilderMode.callOnClick();
    }

    private void updateOperationFabsState() {
        switch (uiFloorPlanView.operation) {
            case ADD_WALL:
                uiFabDeleteWall.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                uiFabAddWall.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
                uiFabSetLocation.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                break;
            case REMOVE_WALL:
                uiFabDeleteWall.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
                uiFabAddWall.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                uiFabSetLocation.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                break;
            case SET_LOCATION:
                uiFabDeleteWall.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                uiFabAddWall.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                uiFabSetLocation.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
                break;
            case NONE:
                uiFabDeleteWall.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                uiFabAddWall.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                uiFabSetLocation.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                break;
        }
    }

    private void updateAutobuilderFabsState() {
        if ((uiFloorPlanView.autobuilderMode & uiFloorPlanView.BUILDER_MODE_LEFT) != 0) {
            uiFabAutobuilderLeft.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
        }
        else {
            uiFabAutobuilderLeft.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
        }

        if ((uiFloorPlanView.autobuilderMode & uiFloorPlanView.BUILDER_MODE_RIGHT) != 0) {
            uiFabAutobuilderRight.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
        }
        else {
            uiFabAutobuilderRight.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
        }

        switch (uiFloorPlanView.autobuilderMode) {
            case FloorPlanView.BUILDER_MODE_NONE:
                uiFabAutobuilderMode.setBackgroundTintList(ColorStateList.valueOf(AppSettings.accentColor));
                uiFabAutobuilderMode.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_directions_walk_white_24dp));
                break;
            case FloorPlanView.BUILDER_MODE_LEFT:
                uiFabAutobuilderMode.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
                uiFabAutobuilderMode.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_directions_walk_left_wall_white_24dp));
                break;
            case FloorPlanView.BUILDER_MODE_RIGHT:
                uiFabAutobuilderMode.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
                uiFabAutobuilderMode.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_directions_walk_right_wall_white_24dp));
                break;
            case FloorPlanView.BUILDER_MODE_BOTH:
                uiFabAutobuilderMode.setBackgroundTintList(ColorStateList.valueOf(AppSettings.primaryDarkColor));
                uiFabAutobuilderMode.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_directions_walk_both_walls_white_24dp));
                break;
        }
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

        registerReceiver(mWifiScanner, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

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
        uiFloorPlanView.initWallsAutobuilder();
        mStepDetectorInited = true;

        mWifiScanner.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
        mStepDetectorInited = false;
        unregisterReceiver(mWifiScanner);
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
                uiFloorPlanView.updateOffset(mOffsetX, -mOffsetY); // -y for moving map downwards

                mTravelledDistance += STEP_LENGTH;
                if (mTravelledDistance >= WALL_CREATION_DISTANCE) {
                    uiFloorPlanView.autobuildWalls();
                    mTravelledDistance = 0;
                }
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

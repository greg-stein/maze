package com.example.neutrino.maze;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import java.util.HashMap;
import java.util.List;

public class MapActivity extends AppCompatActivity implements SensorEventListener {

    // current (X, Y)
    private float currentX = 0f;
    private float currentY = 0f;
    // new (X, Y)
    private float stepX = 0f;
    private float stepY = 0f;
    // The point to rotate map around
    private float pivotX = 0.5f;
    private float pivotY = 0.9f;
    // Map north angle
    private float mapNorth = 0.0f;
    // One human step
    private float moveFactor = 0.01f;

    // How much pixels occupy one meter
    private float mapScale = 10.0f;
    // Cell size 5x5 meters. The whole floor plan is divided into cells
    private float cellSize = 5.0f; //meters
    private HashMap<String, Float>[][] signalAt;

    WifiManager mWifiManager;

    // Holds map graphic
    private ImageView iv_FloorPlan;

    // Where is the map's north
    private EditText et_MapNorth;
    // Current position on the map
    private TextView tv_Position;
    // Number of available APs
    private TextView tv_APs;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    // Identifies if access to gui controls is OK
    private boolean mIsGuiInitialized = false;

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context c, Intent intent) {
            if ((mIsGuiInitialized) && (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                try {
                    tv_APs.setText(String.valueOf(mScanResults.size()));
                } catch (Exception e) {
                    Snackbar exceptionSnackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), e.getMessage(), Snackbar.LENGTH_SHORT);
                    exceptionSnackbar.show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        tv_APs = (TextView) findViewById(R.id.tv_APs);
        mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mWifiManager.startScan();

        et_MapNorth = (EditText) findViewById(R.id.map_north);
        et_MapNorth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mapNorth = Float.parseFloat(s.toString());
                } catch (NumberFormatException nfe) {
                    mapNorth = 0.0f;
                }
            }
        });

        tv_Position = (TextView) findViewById(R.id.tv_coord);
        iv_FloorPlan = (ImageView) findViewById(R.id.imageViewCompass);
        iv_FloorPlan.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //TODO: Calculate real (X, Y) after translation and rotation
                    float[] currentPosition = {event.getX(), event.getY()};
                    float[] mappedPosition = new float[2];
                    Matrix matrix = new Matrix();

                    matrix.postRotate(-currentDegree, iv_FloorPlan.getWidth() * pivotX,
                            iv_FloorPlan.getHeight() * pivotY);
                    int toolbarHeight = findViewById(R.id.toolbar).getHeight();
                    matrix.postTranslate(-stepX * iv_FloorPlan.getWidth(),
                            -(stepY * iv_FloorPlan.getHeight() + toolbarHeight));
                    matrix.mapPoints(mappedPosition, currentPosition);

                    tv_Position.setText("[" + String.valueOf((int)mappedPosition[0]) + ":"
                            + String.valueOf((int)mappedPosition[1]) + "]");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                }
                return false; // Allow click
            }
        });
        iv_FloorPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapActivity.this.stepY += (float) (Math.cos(Math.toRadians(currentDegree)) * moveFactor);
                MapActivity.this.stepX += (float) (Math.sin(Math.toRadians(currentDegree)) * moveFactor);
            }
        });

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mIsGuiInitialized = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
        unregisterReceiver(mWifiScanReceiver);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION: {
                // get the angle around the z-axis rotated
                float degree = Math.round(event.values[0] + mapNorth);

                RotateAnimation ra = new RotateAnimation(
                        currentDegree,
                        -degree,
                        Animation.RELATIVE_TO_SELF, pivotX,
                        Animation.RELATIVE_TO_SELF, pivotY
                );

                TranslateAnimation ta = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, currentX, Animation.RELATIVE_TO_SELF, stepX,
                        Animation.RELATIVE_TO_SELF, currentY, Animation.RELATIVE_TO_SELF, stepY
                );

                AnimationSet animationSet = new AnimationSet(true);
                animationSet.addAnimation(ta);
                animationSet.addAnimation(ra);
                animationSet.setFillAfter(true);
                animationSet.setDuration(210);

                iv_FloorPlan.startAnimation(animationSet);

                currentDegree = -degree;
                currentX = stepX;
                currentY = stepY;
                break;
            }
            case Sensor.TYPE_STEP_DETECTOR: {
                stepY += (float) (Math.cos(Math.toRadians(currentDegree)) * moveFactor);
                stepX += (float) (Math.sin(Math.toRadians(currentDegree)) * moveFactor);
                break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }
}

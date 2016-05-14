package com.example.neutrino.maze;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class MapActivity extends AppCompatActivity implements SensorEventListener {
    private final int STEP_LENGTH = 76; // as per google average step length is 30 inches
    private final int MAX_LEVEL = 100;
    private final String SIGNALS_MAP_FILE = "sigmap.wad"; // :)

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
    private float moveFactor = 0.015f;

    // Stores signal levels for all accessible APs at each entry
    private Cell[][] sigMap;
    // Current position in sigMap
    private int currentCellX = 0;
    private int currentCellY = 0;
    // Holds latest scan results to compare
    private Cell lastScan;

    WifiManager mWifiManager;

    // Holds map graphic
    private CellsImageView iv_FloorPlan;

    // Save cell signals to internal DB
    private Button b_SaveCell;
    // Where is the map's north
    private EditText et_MapNorth;
    // Current position on the map
    private TextView tv_Position;
    // Number of available APs
    private TextView tv_APs;
    // For simulating steps
    private FloatingActionButton fab_Mail;
    // Enables pedometer for moving on map
    private ToggleButton tb_EnablePedometer;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    // Identifies if access to gui controls is OK
    private boolean mIsGuiInitialized = false;

    // Counter for scan indices
    private int scanIndex = 0;

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context c, Intent intent) {
            if ((mIsGuiInitialized) && (intent.getAction() == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                try {
                    List<ScanResult> scanResults = mWifiManager.getScanResults();
                    mWifiManager.startScan();
                    scanIndex++;

                    lastScan.clear();
                    for (ScanResult scanResult : scanResults) {
                        int level = WifiManager.calculateSignalLevel(scanResult.level, MAX_LEVEL);
                        lastScan.addSignal(scanResult.BSSID, level);
                    }

                    // #5:[34]
                    tv_APs.setText("#" + scanIndex + ":[" + String.valueOf(scanResults.size()) + "]");
                } catch (Exception e) {
                    ShowError(e);
                }
            }
        }
    };

    private void ShowError(Exception e) {
        Snackbar exceptionSnackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), e.getMessage(), Snackbar.LENGTH_SHORT);
        exceptionSnackbar.show();
    }

    //Size of cellMap
    private int mMapCellsX;
    private int mMapCellsY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fab_Mail = (FloatingActionButton) findViewById(R.id.fab_mail);
        tv_Position = (TextView) findViewById(R.id.tv_coord);
        iv_FloorPlan = (CellsImageView) findViewById(R.id.imageViewCompass);
        tv_APs = (TextView) findViewById(R.id.tv_APs);
        et_MapNorth = (EditText) findViewById(R.id.map_north);
        tb_EnablePedometer = (ToggleButton) findViewById(R.id.tb_enable_pedometer);
        b_SaveCell = (Button) findViewById(R.id.b_save_cell);

        lastScan = new Cell();

        mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mWifiManager.startScan();

        b_SaveCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sigMap[currentCellX][currentCellY] = lastScan;
                lastScan = new Cell();
            }
        });

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

        fab_Mail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepY += (float) (Math.cos(Math.toRadians(currentDegree)) * moveFactor);
                stepX += (float) (Math.sin(Math.toRadians(currentDegree)) * moveFactor);
                updateLocation();
            }
        });

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mIsGuiInitialized = true;

        ViewTreeObserver vto = iv_FloorPlan.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onGlobalLayout() {
                iv_FloorPlan.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mMapCellsX = iv_FloorPlan.getWidthInCells();
                mMapCellsY = iv_FloorPlan.getHeightInCells();

                // Failed to load from saved state? Create new one.
                if (loadSigMap() == false) {
                    sigMap = new Cell[mMapCellsX][mMapCellsY];
                }
                iv_FloorPlan.setCells(sigMap);

                int toolbarHeight = findViewById(R.id.toolbar).getHeight();
                iv_FloorPlan.setCellsOffsetY(toolbarHeight); 
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        saveSigMap();
    }

//    @Override
//    public void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//
//        loadSigMap();
//    }

//    @Override
//    public void onSaveInstanceState(Bundle savedInstanceState) {
//        super.onSaveInstanceState(savedInstanceState);
//
//        saveSigMap();
//    }

    private void saveSigMap() {
        Context context = getApplicationContext();
        FileOutputStream fos = null;

        try {
            File f = context.getFilesDir();
            fos = context.openFileOutput(SIGNALS_MAP_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(sigMap);
            os.close();
            fos.close();
        } catch (IOException e) {
            ShowError(e);
            e.printStackTrace();
        }
    }

    private boolean loadSigMap() {
        Context context = getApplicationContext();
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(SIGNALS_MAP_FILE);
            ObjectInputStream is = new ObjectInputStream(fis);
            sigMap = (Cell[][]) is.readObject();
            is.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            ShowError(e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void updateLocation() {
        //Calculate real (X, Y) after translation and rotation
        float[] currentPosition = {pivotX * iv_FloorPlan.getWidth(), pivotY * iv_FloorPlan.getHeight()};
        float[] mappedPosition = new float[2];
                
        Matrix matrix = new Matrix();

        matrix.postRotate(-currentDegree, iv_FloorPlan.getWidth() * pivotX,
                iv_FloorPlan.getHeight() * pivotY);
        int toolbarHeight = findViewById(R.id.toolbar).getHeight();
        matrix.postTranslate(-stepX * iv_FloorPlan.getWidth(),
                -(stepY * iv_FloorPlan.getHeight() + toolbarHeight));
        matrix.mapPoints(mappedPosition, currentPosition);

        currentCellX = iv_FloorPlan.toCellMetric((int) mappedPosition[0]);
        currentCellY = iv_FloorPlan.toCellMetric((int) mappedPosition[1]);
        tv_Position.setText("pos:[" + String.valueOf(currentCellX) + ":"
                + String.valueOf(currentCellY) + "]");
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
                if (tb_EnablePedometer.isChecked()) {
                    stepY += (float) (Math.cos(Math.toRadians(currentDegree)) * moveFactor);
                    stepX += (float) (Math.sin(Math.toRadians(currentDegree)) * moveFactor);
                    updateLocation();
                }
                break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }
}

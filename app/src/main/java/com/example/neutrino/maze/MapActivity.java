package com.example.neutrino.maze;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;

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

    // Holds map graphic
    private ImageView image;

    // Where is the map's north
    private EditText ui_MapNorth;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ui_MapNorth = (EditText) findViewById(R.id.map_north);
        ui_MapNorth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mapNorth = Float.parseFloat(s.toString());
                } catch (NumberFormatException nfe) {
                    mapNorth = 0.0f;
                }
            }
        });
        image = (ImageView) findViewById(R.id.imageViewCompass);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapActivity.this.stepY += (float) (Math.cos(Math.toRadians(currentDegree)) * moveFactor);
                MapActivity.this.stepX += (float) (Math.sin(Math.toRadians(currentDegree)) * moveFactor);
            }
        });

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
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

                image.startAnimation(animationSet);

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

package com.example.neutrino.maze;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class MapActivity extends AppCompatActivity implements SensorEventListener {

    private float stepX = 0f;
    private float stepY = 0f;
    private float currentX = 0f;
    private float currentY = 0f;
    private float moveFactor = 0.01f;

    // define the display assembly compass picture
    private ImageView image;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    TextView tvHeading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // our compass image
        image = (ImageView) findViewById(R.id.imageViewCompass);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapActivity.this.stepY += 0.005;//(float) (Math.cos(Math.toRadians(-currentDegree)) * moveFactor);
                MapActivity.this.stepX += 0;//(float) (Math.sin(Math.toRadians(-currentDegree)) * moveFactor);
            }
        });

        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) findViewById(R.id.tvHeading);

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);

        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");

        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f + stepX,
                Animation.RELATIVE_TO_SELF, 0.5f + stepY
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }
}

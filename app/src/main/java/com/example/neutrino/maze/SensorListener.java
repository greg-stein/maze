package com.example.neutrino.maze;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by Greg Stein on 4/11/2017.
 */

public class SensorListener implements SensorEventListener {
    private static SensorListener instance = new SensorListener();
    private List<IStepDetectedListener> mStepDetectedEventListeners;

    public static SensorListener getInstance() {return instance;}

    private SensorManager mSensorManager;
    private Sensor mStepDetector;
    private boolean mHaveStepDetector;

    private SensorListener() {
        mSensorManager = (SensorManager) AppSettings.appActivity.getSystemService(SENSOR_SERVICE);
        mStepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    public void onActivityResume() {
        mHaveStepDetector = mSensorManager.registerListener(this, mStepDetector, SensorManager.SENSOR_DELAY_UI);
    }

    public void onActivityPause() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_DETECTOR: {
                emitStepDetectedEvent();
            }
        }
    }

    public interface IStepDetectedListener {
        void onStepDetected();
    }

    public void addStepDetectedListener(IStepDetectedListener listener) {
        mStepDetectedEventListeners.add(listener);
    }

    private void emitStepDetectedEvent() {
        for (IStepDetectedListener listener : mStepDetectedEventListeners) {
            listener.onStepDetected();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

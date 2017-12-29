package com.example.neutrino.maze;

import android.Manifest;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Greg Stein on 3/22/2017.
 */

public class SensorDataGrabber implements SensorEventListener {
    public static final String MAIN_APP_DIR = "maze";   // find a way to use @string/app_name
    public static final String SENSOR_DATA_DIR = "sensor_data";
    private static final String MAGNETOMETER_SENSOR_LOG_FILENAME = "magnetometer_sensor_data.csv";
    private static final String ACCELEROMETER_SENSOR_LOG_FILENAME = "accelerometer_sensor_data.csv";
    private static final String GYROSCOPE_SENSOR_LOG_FILENAME = "gyroscope_sensor_data.csv";
    private static final String ROTATION_FUSED_SENSOR_LOG_FILENAME = "rotation_fused_sensor_data.csv";
    private static final String GRAVITY_SENSOR_LOG_FILENAME = "gravity_sensor_data.csv";
    private static final String GYROSCOPE_UNCALIBRATED_SENSOR_LOG_FILENAME = "gyroscope_uncalibrated_sensor_data.csv";
    private static final String MAGNETOMETER_UNCALIBRATED_SENSOR_LOG_FILENAME = "magnetometer_uncalibrated_sensor_data.csv";

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private Sensor mGravity;
    private Sensor mRotation;
    private Sensor mGyroscope;
    private Sensor mGyroscopeUncalibrated;
    private Sensor mMagnetometerUncalibrated;

    private boolean mHaveAccelerometer;
    private boolean mHaveMagnetometer;
    private boolean mHaveGravity;
    private boolean mHaveRotation;
    private boolean mHaveGyroscope;
    private boolean mHaveGyroscopeUncalibrated;
    private boolean mHaveMagnetometerUncalibrated;

    private CSVWriter mMagnetometerCsvWriter;
    private CSVWriter mAccelerometerCsvWriter;
    private CSVWriter mGyroscopeCsvWriter;
    private CSVWriter mRotationFusedCsvWriter;
    private CSVWriter mGravityCsvWriter;
    private CSVWriter mGyroscopeUncalibratedCsvWriter;
    private CSVWriter mMagnetometerUncalibratedCsvWriter;
    private boolean mIsRecording;
    private File mSensorDataDir;

    public SensorDataGrabber(SensorManager sensorManager) {
        mSensorManager = sensorManager;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGyroscopeUncalibrated = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        mMagnetometerUncalibrated = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);

    }

    private FileWriter createWriter(String filename) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == false) {
            return null;
        }

        File file = new File(mSensorDataDir, filename);
        FileWriter writer = null;
        try {
            file.createNewFile();
            writer = new FileWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return writer;
    }

    private File getDirectoryForSensorData() {
        File mainDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), MAIN_APP_DIR);
        if (mainDir.isDirectory() || mainDir.mkdir()) {
            File sensorDir = new File(mainDir, SENSOR_DATA_DIR);
            if (sensorDir.isDirectory() || sensorDir.mkdir()) {
                for (int i = 1;;i++) {
                    File subDir = new File(sensorDir, String.format("%03d",i));
                    if (!subDir.exists()) {
                        if (subDir.mkdir()) {
                            return subDir;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void startListeningToSensors() {
        mHaveRotation = mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_GAME);
        mHaveMagnetometer = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        mHaveMagnetometerUncalibrated = mSensorManager.registerListener(this, mMagnetometerUncalibrated, SensorManager.SENSOR_DELAY_GAME);
        mHaveAccelerometer = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mHaveGravity = mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_GAME);
        mHaveGyroscope = mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
        mHaveGyroscopeUncalibrated = mSensorManager.registerListener(this, mGyroscopeUncalibrated, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopListeningToSensors() {
        mSensorManager.unregisterListener(this, mMagnetometer);
        mSensorManager.unregisterListener(this, mMagnetometerUncalibrated);
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mGravity);
        mSensorManager.unregisterListener(this, mGyroscope);
        mSensorManager.unregisterListener(this, mGyroscopeUncalibrated);
        mSensorManager.unregisterListener(this, mRotation);
    }

    public void openSensorLogFiles() {
        mSensorDataDir = getDirectoryForSensorData();
        if (mSensorDataDir == null) {
            throw new RuntimeException("Could not get directory for sensor data");
        }
        mMagnetometerCsvWriter = new CSVWriter(createWriter(MAGNETOMETER_SENSOR_LOG_FILENAME), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        mAccelerometerCsvWriter = new CSVWriter(createWriter(ACCELEROMETER_SENSOR_LOG_FILENAME), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        mGyroscopeCsvWriter = new CSVWriter(createWriter(GYROSCOPE_SENSOR_LOG_FILENAME), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        mRotationFusedCsvWriter = new CSVWriter(createWriter(ROTATION_FUSED_SENSOR_LOG_FILENAME), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        mGravityCsvWriter = new CSVWriter(createWriter(GRAVITY_SENSOR_LOG_FILENAME), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        mGyroscopeUncalibratedCsvWriter = new CSVWriter(createWriter(GYROSCOPE_UNCALIBRATED_SENSOR_LOG_FILENAME), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        mMagnetometerUncalibratedCsvWriter = new CSVWriter(createWriter(MAGNETOMETER_UNCALIBRATED_SENSOR_LOG_FILENAME), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
    }

    public void closeSensorLogFiles() {
        try {
            mMagnetometerCsvWriter.flush();
            mAccelerometerCsvWriter.flush();
            mGyroscopeCsvWriter.flush();
            mRotationFusedCsvWriter.flush();
            mGravityCsvWriter.flush();
            mGyroscopeUncalibratedCsvWriter.flush();
            mMagnetometerUncalibratedCsvWriter.flush();

            mMagnetometerCsvWriter.close();
            mAccelerometerCsvWriter.close();
            mGyroscopeCsvWriter.close();
            mRotationFusedCsvWriter.close();
            mGravityCsvWriter.close();
            mGyroscopeUncalibratedCsvWriter.close();
            mMagnetometerUncalibratedCsvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] toStrings(float[] data) {
        String[] strings = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            strings[i] = String.valueOf(data[i]);
        }

        return strings;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED: {
                if (mIsRecording) {
                    String[] row = toStrings(event.values);
                    mGyroscopeUncalibratedCsvWriter.writeNext(row);
                }
            }
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED: {
                if (mIsRecording) {
                    String[] row = toStrings(event.values);
                    mMagnetometerUncalibratedCsvWriter.writeNext(row);
                }
            }
            case Sensor.TYPE_GYROSCOPE: {
                if (mIsRecording) {
                    String[] row = toStrings(event.values);
                    mGyroscopeCsvWriter.writeNext(row);
                }
            }
            case Sensor.TYPE_GRAVITY: {
                if (mIsRecording) {
                    String[] row = toStrings(event.values);
                    mGravityCsvWriter.writeNext(row);
                }
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                if (mIsRecording) {
                    String[] row = toStrings(event.values);
                    mAccelerometerCsvWriter.writeNext(row);
                }
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD: {
                if (mIsRecording) {
                    String[] row = toStrings(event.values);
                    mMagnetometerCsvWriter.writeNext(row);
                }
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR: {
                // calculate the rotation matrix
                if (mIsRecording) {
                    String[] row = toStrings(event.values);
                    mRotationFusedCsvWriter.writeNext(row);
                }
                break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void setIsRecording(boolean mIsRecording) {
        this.mIsRecording = mIsRecording;
    }

    public boolean isRecording() { return mIsRecording; }
}

package com.example.neutrino.maze;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

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

    private class SensorController {
        Sensor mSensor;
        int mType;
        boolean mAvailable;
        String mFilename;
        CSVWriter mCsvWriter;

        public SensorController(int type, String filename) {
            mType = type;
            mFilename = filename;
            mSensor = mSensorManager.getDefaultSensor(mType);
        }

        public void register(SensorDataGrabber sensorDataGrabber) {
            mAvailable = sensorDataGrabber.mSensorManager.registerListener(sensorDataGrabber, mSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        public void unregister(SensorDataGrabber sensorDataGrabber) {
            sensorDataGrabber.mSensorManager.unregisterListener(sensorDataGrabber, mSensor);
        }

        public void createCsvWriter() {
            mCsvWriter = new CSVWriter(createWriter(mFilename), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        }

        public void closeCsvWriter() throws IOException {
            mCsvWriter.flush();
            mCsvWriter.close();
        }

        public int getType() {
            return mType;
        }

        public void writeSensorData(String[] row) {
            mCsvWriter.writeNext(row);
        }
    };

    private SensorManager mSensorManager;

    private boolean mIsRecording;
    private File mSensorDataDir;

    private ArrayList<SensorController> mAllSensorControllers = new ArrayList<>();

    public SensorDataGrabber(SensorManager sensorManager) {
        mSensorManager = sensorManager;

        mAllSensorControllers.add(new SensorController(Sensor.TYPE_ACCELEROMETER, ACCELEROMETER_SENSOR_LOG_FILENAME));
        mAllSensorControllers.add(new SensorController(Sensor.TYPE_MAGNETIC_FIELD, MAGNETOMETER_SENSOR_LOG_FILENAME));
        mAllSensorControllers.add(new SensorController(Sensor.TYPE_GRAVITY, GRAVITY_SENSOR_LOG_FILENAME));
        mAllSensorControllers.add(new SensorController(Sensor.TYPE_ROTATION_VECTOR, ROTATION_FUSED_SENSOR_LOG_FILENAME));
        mAllSensorControllers.add(new SensorController(Sensor.TYPE_GYROSCOPE, GYROSCOPE_SENSOR_LOG_FILENAME));
        mAllSensorControllers.add(new SensorController(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, GYROSCOPE_UNCALIBRATED_SENSOR_LOG_FILENAME));
        mAllSensorControllers.add(new SensorController(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, MAGNETOMETER_UNCALIBRATED_SENSOR_LOG_FILENAME));
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

        for (SensorController controller: mAllSensorControllers) {
            controller.register(this);
        }
    }

    public void stopListeningToSensors() {
        for (SensorController controller: mAllSensorControllers) {
            controller.unregister(this);
        }
    }

    public void openSensorLogFiles() {
        mSensorDataDir = getDirectoryForSensorData();
        if (mSensorDataDir == null) {
            throw new RuntimeException("Could not get directory for sensor data");
        }

        for (SensorController controller: mAllSensorControllers) {
            controller.createCsvWriter();
        }
    }

    public void closeSensorLogFiles() {
        try {
            for (SensorController controller: mAllSensorControllers) {
                controller.closeCsvWriter();
            }
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
        if (mIsRecording) {
            String[] row = toStrings(event.values);
            int type = event.sensor.getType();
            for (SensorController controller : mAllSensorControllers) {
                if (controller.getType() == type) {
                    controller.writeSensorData(row);
                }
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

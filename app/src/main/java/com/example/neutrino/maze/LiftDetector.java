package com.example.neutrino.maze;

import java.util.Arrays;

/**
 * Created by Dima Ruinskiy on 02/09/17.
 */

public class LiftDetector implements SensorListener.IGravityChangedListener {

    private static LiftDetector instance = new LiftDetector();

    public static LiftDetector getInstance() {
        return instance;
    }

    private static final int LOWPASS_LENGTH = 100;
    private static final int MEDFILT_LENGTH = 15;
    private static final int MIN_EVENT_LENGTH = 80;
    private static final int MAX_EVENT_GAP = 100;

    private static final int WINDOW_SIZE = LOWPASS_LENGTH + MEDFILT_LENGTH;
    private static final float alpha = (1.0f / LOWPASS_LENGTH);

    private float[] signalData;
    private float[] signalDataFiltered;
    private float[] signalDataSign;
    private float[] signalDataSignFiltered;

    private float[] medFiltArray;

    private int k;
    private float runningSum;

    private LiftDetector() {
        signalData = new float[WINDOW_SIZE];
        signalDataFiltered = new float[WINDOW_SIZE];
        medFiltArray = new float[MEDFILT_LENGTH];

        k = 0;
        runningSum = 0;
    }

    @Override
    public void onGravityChanged(float newGravity) {
        signalData[k] = newGravity;                                             // Store new sample
        runningSum -= signalData[(k - LOWPASS_LENGTH) % WINDOW_SIZE];           // Remove oldest sample
        signalDataFiltered[k] = alpha * newGravity + (1 - alpha) * runningSum;  // Low-pass filtered sample
        runningSum += newGravity;                                               // Add new sample to sum
        signalDataSign[k] = Math.signum(signalDataFiltered[k]);                 // Sign of gravity
        signalDataSignFiltered[k] = MedFiltSign();                              // Median-filtered sign function
        k = (k + 1) % WINDOW_SIZE;
    }

    private float MedFiltSign() {
        int m = k - (MEDFILT_LENGTH / 2);
        for (int i = 0; i < MEDFILT_LENGTH; i++, m++) {
            medFiltArray[i] = signalDataSign[m % WINDOW_SIZE];
        }
        Arrays.sort(medFiltArray);
        return medFiltArray[MEDFILT_LENGTH / 2];
    }
}
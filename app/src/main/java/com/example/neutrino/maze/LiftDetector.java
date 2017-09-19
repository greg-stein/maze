package com.example.neutrino.maze;

import java.util.Arrays;

/**
 * Created by Dima Ruinskiy on 02/09/17.
 */

public class LiftDetector implements SensorListener.IGravityChangedListener {

    enum LiftState {
        NOT_IN_MOVING_LIFT,
        IN_LIFT_GOING_UP,
        IN_LIFT_GOING_DOWN,
    };

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

    private int thisSign;
    private int currentNegLength, currentPosLength; // Keeps current length of negative/positive gravity streaks
    private int lastNegLength, lastPosLength;       // Keeps last meaningful streak lengths
    private int currentGap;                         // Current gap between meaningful streaks

    private LiftState currentState;                 // Current state (in moving lift or not)
    private boolean stateChanged;                   // Flag to indicate recent state changed

    private int k;              // Sliding window index
    private float runningSum;   // Running sum for lowpass filter

    private LiftDetector() {
        signalData = new float[WINDOW_SIZE];
        signalDataFiltered = new float[WINDOW_SIZE];
        signalDataSign = new float[WINDOW_SIZE];
        signalDataSignFiltered = new float[WINDOW_SIZE];
        medFiltArray = new float[MEDFILT_LENGTH];

        k = 0;
        runningSum = 0;

        currentState = LiftState.NOT_IN_MOVING_LIFT;
        thisSign = 0;
        currentNegLength = currentPosLength = lastNegLength = lastPosLength = 0;
    }

    @Override
    public void onGravityChanged(float newGravity) {
        signalData[k] = newGravity;                                             // Store new sample
        runningSum -= signalData[(k - LOWPASS_LENGTH) % WINDOW_SIZE];           // Remove oldest sample
        signalDataFiltered[k] = alpha * newGravity + (1 - alpha) * runningSum;  // Low-pass filtered sample
        runningSum += newGravity;                                               // Add new sample to sum
        signalDataSign[k] = Math.signum(signalDataFiltered[k]);                 // Sign of gravity
        signalDataSignFiltered[k] = MedFiltSign();                              // Median-filtered sign function

        thisSign = (int)signalDataSignFiltered[k];
        k = (k + 1) % WINDOW_SIZE;

        ++currentGap;
        // Once gap exceeds maximum allowed, last long streaks are considered irrelevant
        if (currentGap > MAX_EVENT_GAP) {
            lastNegLength = lastPosLength = 0;
        }

        if (thisSign < 0) {
            ++currentNegLength;
            // When changing from positive to negative - check current running positive length
            // If it exceeds the minimum meaningful length, store it and reset the event gap counter
            // However, skip this if state was recently changed:
            // The same positive gravity cannot count towards both start and stop of lift descent
            if (currentPosLength > MIN_EVENT_LENGTH) {
                if (!stateChanged) {
                    lastPosLength = currentPosLength;
                    currentGap = 0;
                } else {
                    stateChanged = false;
                }
            }
            currentPosLength = 0;
        } else if (thisSign > 0) {
            ++currentPosLength;
            // When changing from negative to positive - check current running negative length
            // If it exceeds the minimum meaningful length, store it and reset the event gap counter
            // However, skip this if state was recently changed:
            // The same negative gravity cannot count towards both start and stop of lift ascent
            if (currentNegLength > MIN_EVENT_LENGTH) {
                if (!stateChanged) {
                    lastNegLength = currentNegLength;
                    currentGap = 0;
                } else {
                    stateChanged = false;
                }
            }
            currentNegLength = 0;
        }

        // If we have a meaningful positive streak, followed by a meaningful negative streak,
        // this can indicate either the beginning of lift ascent or the end of a lift descent.
        // Change the state here depending on the current state.
        // Note that we do not check the gap here; if gap exceeds maximum allowed,
        // the last meaningful streak will be reset already earlier.
        if (currentNegLength > MIN_EVENT_LENGTH && lastPosLength > MIN_EVENT_LENGTH) {
            if (currentState == LiftState.NOT_IN_MOVING_LIFT) {
                currentState = LiftState.IN_LIFT_GOING_UP;
                stateChanged = true;
            } else if (currentState == LiftState.IN_LIFT_GOING_DOWN) {
                currentState = LiftState.NOT_IN_MOVING_LIFT;
                stateChanged = true;
            }
        }

        // If we have a meaningful negative streak, followed by a meaningful positive streak,
        // this can indicate either the beginning of lift descent or the end of a lift ascent.
        // Change the state here depending on the current state.
        // Note that we do not check the gap here; if gap exceeds maximum allowed,
        // the last meaningful streak will be reset already earlier.
        if (currentPosLength > MIN_EVENT_LENGTH && lastNegLength > MIN_EVENT_LENGTH) {
            if (currentState == LiftState.NOT_IN_MOVING_LIFT) {
                currentState = LiftState.IN_LIFT_GOING_DOWN;
                stateChanged = true;
            } else if (currentState == LiftState.IN_LIFT_GOING_UP) {
                currentState = LiftState.NOT_IN_MOVING_LIFT;
                stateChanged = true;
            }
        }
    }

    /* Return MEDFILT_LENGTH-point median filtering of the signal centered at the current index */
    private float MedFiltSign() {
        int m = k - (MEDFILT_LENGTH / 2);
        for (int i = 0; i < MEDFILT_LENGTH; i++, m++) {
            medFiltArray[i] = signalDataSign[m % WINDOW_SIZE];
        }
        Arrays.sort(medFiltArray);
        return medFiltArray[MEDFILT_LENGTH / 2];
    }
}
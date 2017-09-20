package com.example.neutrino.maze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dima Ruinskiy on 02/09/17.
 */

public class LiftDetector implements SensorListener.IGravityChangedListener {

    enum LiftState {
        NOT_IN_MOVING_LIFT,
        IN_LIFT_GOING_UP,
        IN_LIFT_GOING_DOWN
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
    private int m;              // Median filter center index (lags behind window index)
    private int n;              // Global event counter (for debug purposes)
    private float runningSum;   // Running sum for lowpass filter

    private List<IElevatorListener> mElevatorListeners = new ArrayList<>();

    private boolean verboseSystemOutput;    // Whether to print detailed data to System.out

    private LiftDetector() {
        signalData = new float[WINDOW_SIZE];
        signalDataFiltered = new float[WINDOW_SIZE];
        signalDataSign = new float[WINDOW_SIZE];
        signalDataSignFiltered = new float[WINDOW_SIZE];
        medFiltArray = new float[MEDFILT_LENGTH];

        k = 0;
        m = (WINDOW_SIZE - MEDFILT_LENGTH / 2) % WINDOW_SIZE;   // MEDFILT_LENGTH/2 samples delay
        n = 0;
        runningSum = 0;

        currentState = LiftState.NOT_IN_MOVING_LIFT;
        thisSign = 0;
        currentNegLength = currentPosLength = lastNegLength = lastPosLength = 0;

        verboseSystemOutput = false;
    }

    public void setVerboseSystemOutput(boolean v) {
        verboseSystemOutput = v;
    }

    @Override
    public void onGravityChanged(float newGravity) {
        signalData[k] = newGravity;                                                 // Store new sample
        runningSum -= signalData[(k - LOWPASS_LENGTH + WINDOW_SIZE) % WINDOW_SIZE]; // Remove oldest sample
        runningSum += newGravity;                                                   // Add new sample to sum
        signalDataFiltered[k] = alpha * runningSum;                                 // Low-pass filtered sample
        signalDataSign[k] = Math.signum(signalDataFiltered[k]);                     // Sign of gravity
        signalDataSignFiltered[m] = MedFiltSign();                                  // Median-filtered sign function

        if (verboseSystemOutput) {
            System.out.printf("%4d Z=%+1.4f G=%+1.4f, S=%+d, F=%+d    ",
                    n, signalData[k], signalDataFiltered[k], (int) signalDataSign[k], (int) signalDataSignFiltered[k]);
            if (signalDataSignFiltered[m] != signalDataSignFiltered[(m - 1 + WINDOW_SIZE) % WINDOW_SIZE]) {
                System.out.print(n - MEDFILT_LENGTH / 2);
            }
            System.out.print("\n");
        }

        thisSign = (int) signalDataSignFiltered[k];
        k = (k + 1) % WINDOW_SIZE;
        m = (m + 1) % WINDOW_SIZE;
        ++n;

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
                ChangeStateAndNotify(LiftState.IN_LIFT_GOING_UP);
            } else if (currentState == LiftState.IN_LIFT_GOING_DOWN) {
                ChangeStateAndNotify(LiftState.NOT_IN_MOVING_LIFT);
            }
        }

        // If we have a meaningful negative streak, followed by a meaningful positive streak,
        // this can indicate either the beginning of lift descent or the end of a lift ascent.
        // Change the state here depending on the current state.
        // Note that we do not check the gap here; if gap exceeds maximum allowed,
        // the last meaningful streak will be reset already earlier.
        if (currentPosLength > MIN_EVENT_LENGTH && lastNegLength > MIN_EVENT_LENGTH) {
            if (currentState == LiftState.NOT_IN_MOVING_LIFT) {
                ChangeStateAndNotify(LiftState.IN_LIFT_GOING_DOWN);
            } else if (currentState == LiftState.IN_LIFT_GOING_UP) {
                ChangeStateAndNotify(LiftState.NOT_IN_MOVING_LIFT);
            }
        }
    }

    /* Return L-point median filtering of the signal centered at the current index (where L is the
     * length of the median filter (L must be odd).
     * Note that the median filter needs "future" samples, so the calculation lags the main loop index
     * by (L-1)/2. The sample at index K is the median value of samples [K-(L-1)/2 ... K+(L-1)/2].
     * Therefore after receiving the sample at K, the main loop updates the median filtered value
     * at index M = K-(L-1)/2, since only at that point all the neede "future" samples are available.
     * Note also that since L is assumed odd, we rely on the automatic rounding down of integer
     * division, so we can use L/2 and not (L-1)/2.
     */
    private float MedFiltSign() {
        for (int i = m - (MEDFILT_LENGTH / 2), j = 0; j < MEDFILT_LENGTH; i++, j++) {
            medFiltArray[j] = signalDataSign[(i + WINDOW_SIZE) % WINDOW_SIZE];
        }
        Arrays.sort(medFiltArray);
        return medFiltArray[MEDFILT_LENGTH / 2];
    }

    private void ChangeStateAndNotify(LiftState newState) {
        currentState = newState;
        stateChanged = true;
        emitLiftStateChangedEvent(newState);
    }

    public interface IElevatorListener {
        void onLiftStateChanged(LiftState newState);
    }

    public void addElevatorListener(IElevatorListener listener) {
        mElevatorListeners.add(listener);
    }

    private void emitLiftStateChangedEvent(LiftState newState) {
        for (IElevatorListener listener : mElevatorListeners) {
            listener.onLiftStateChanged(newState);
        }
    }

}
package com.example.neutrino.maze;

/**
 * Created by Dima Ruinskiy on 02/09/17.
 */

public class LiftDetector implements SensorListener.IGravityChangedListener {

    private static LiftDetector instance = new LiftDetector();
    public static LiftDetector getInstance() {return instance;}

    private LiftDetector() {}

    @Override
    public void onGravityChanged(float newGravity) {
    }
}

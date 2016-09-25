package com.example.neutrino.maze;

/**
 * Created by Greg Stein on 9/25/2016.
 *
 * This class is for debugging/development only!!
 */
public class Mark extends Footprint {
    private static final float MARK_RADIUS = 0.05f;

    public Mark(float cx, float cy, int color) {
        super(cx, cy, MARK_RADIUS);
        setColor(color);
    }
}

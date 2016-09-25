package com.example.neutrino.maze;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 9/17/2016.
 */
public class TugOfWar {
    public interface ITugger {
        void getPosition(PointF position);
        float getForce(); // force = 1/error
    }

    private List<ITugger> mTuggers = new ArrayList<>();
    private final PointF mTuggerPos = new PointF();
    private final PointF mCurrentWeightedCenter = new PointF();

    public void registerTugger(ITugger tugger) {
        mTuggers.add(tugger);
    }

    public PointF getCurrentPosition() {
        fusePositions();
        return mCurrentWeightedCenter;
    }

    private void fusePositions() {
        float tuggerForce, totalForce = 0f;
        float centerX = 0f, centerY = 0f;

        for (ITugger tugger : mTuggers) {
            tugger.getPosition(mTuggerPos);
            tuggerForce = tugger.getForce();

            centerX += tuggerForce * mTuggerPos.x;
            centerY += tuggerForce * mTuggerPos.y;
            totalForce += tuggerForce;
        }

        mCurrentWeightedCenter.set(centerX/totalForce, centerY/totalForce);
    }
}

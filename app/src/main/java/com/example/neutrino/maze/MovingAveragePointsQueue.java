package com.example.neutrino.maze;

import android.graphics.PointF;

/**
 * Created by Greg Stein on 4/11/2017.
 */

public class MovingAveragePointsQueue extends MovingAverageQueueBase<PointF> {
    private PointF mPointSum = new PointF();

    public MovingAveragePointsQueue(int windowSize) {
        super(windowSize);
    }

    public PointF getMeanPoint() {
        PointF mean = new PointF();

        final int pointsInQueue = mQueue.size();
        mean.set(mPointSum.x / pointsInQueue, mPointSum.y / pointsInQueue);
        return mean;
    }

    @Override
    protected void addToSum(PointF item) {
        mPointSum.offset(item.x, item.y);
    }

    @Override
    protected void subtractFromSum(PointF item) {
        mPointSum.offset(-item.x, -item.y);
    }
}

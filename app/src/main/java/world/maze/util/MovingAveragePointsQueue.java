package world.maze.util;

import android.graphics.PointF;

/**
 * Created by Greg Stein on 4/11/2017.
 */

public class MovingAveragePointsQueue extends MovingAverageQueueBase<PointF> {
    private float mSumX = 0;
    private float mSumY = 0;

    public MovingAveragePointsQueue(int windowSize) {
        super(windowSize);
    }

    public PointF meanPoint() {
        PointF mean = new PointF();

        final int pointsInQueue = mQueue.size();
        mean.set(mSumX / pointsInQueue, mSumY / pointsInQueue);
        return mean;
    }

    public double variance() {
        PointF mean = meanPoint();
        double variance = 0;

        for (PointF point : mQueue) {
            final float diffX = point.x - mean.x;
            final float diffY = point.y - mean.y;

            variance += diffX * diffX + diffY * diffY;
        }

        return variance/mQueue.size();
    }

    @Override
    protected void addToSum(PointF item) {
        mSumX += item.x;
        mSumY += item.y;
    }

    @Override
    protected void subtractFromSum(PointF item) {
        mSumX -= item.x;
        mSumY -= item.y;
    }
}

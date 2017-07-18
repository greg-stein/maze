package com.example.neutrino.maze;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by Greg Stein on 3/6/2017.
 */

public abstract class MovingAverageQueueBase<T> {
    public static final int MAX_SCANS_TO_AVERAGE = 3;
    protected int mWindowSize = MAX_SCANS_TO_AVERAGE;
    protected ArrayDeque<T> mQueue = new ArrayDeque<>(mWindowSize);

    public MovingAverageQueueBase(int windowSize) {
        setWindowSize(windowSize);
    }

    public void add(T item) {
        if (mQueue.size() == mWindowSize) {
            subtractFromSum(mQueue.remove());
        }

        if (mQueue.add(item)) {
            addToSum(item);
        }
        else {
            throw new RuntimeException("Unable to add new item to queue.");
        }
    }


    protected abstract void addToSum(T item);

    protected abstract void subtractFromSum(T item);

    public int getItemsNum() {
        return mQueue.size();
    }

    public T getLastItem() {return mQueue.peekLast();}

    public int getWindowSize() {return mWindowSize;}

    public void setWindowSize(int windowSize) {
        mWindowSize = windowSize;
    }
}

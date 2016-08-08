package com.example.neutrino.maze;

import android.graphics.PointF;

/**
 * Created by Greg Stein on 8/7/2016.
 */
public class VectorHelper {

    /**
     * Well, this is tough thing to describe what this func does. It gets a line
     * defined by two points and constructs two parallel lines on both sides of
     * the given line. To illustrate this here is a sketch:
     *
     *          * A               * A'
     *         /                /    * A
     *       /        =>      /    /    * A''
     *     /                /    /    /
     *   * B              * B' /    /
     *                       * B  /
     *                          * B''
     * The distance between each produced line and the original one is given in
     * parameter dist
     *
     * @param a          First point of a given line
     * @param b          Second point of given line
     * @param dist       Desired distance between produced lines and given line
     * @param splitLines [OUT] float array which will hold the resulted lines
     *                   The format of output is as follows:
     *                       splitLines[0] = A'.x, splitLines[1] = A'.y
     *                       splitLines[3] = A''.x, splitLines[4] = A''.y
     *                       splitLines[6] = B'.x, splitLines[7] = B'.y
     *                       splitLines[9] = B''.x, splitLines[10] = B''.y
     *                       splitLines[2,5,8,11] are reserved for Z (not provided
     *                       by this implementation)
     */
    public static void splitLine(PointF a, PointF b, float dist, float[] splitLines) {
        float[] vector = {a.x - b.x, a.y - b.y};
        float magnitude = (float) Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1]);
        float[] identityVector = {vector[0]/magnitude, vector[1]/magnitude};
        float[] orthogonalIdentityVector = {identityVector[1], -identityVector[0]};

        splitLines[0] = a.x + dist * orthogonalIdentityVector[0];
        splitLines[1] = a.y + dist * orthogonalIdentityVector[1];

        splitLines[3] = a.x - dist * orthogonalIdentityVector[0];
        splitLines[4] = a.y - dist * orthogonalIdentityVector[1];

        splitLines[6] = b.x + dist * orthogonalIdentityVector[0];
        splitLines[7] = b.y + dist * orthogonalIdentityVector[1];

        splitLines[9] = b.x - dist * orthogonalIdentityVector[0];
        splitLines[10] = b.y - dist * orthogonalIdentityVector[1];
    }
}

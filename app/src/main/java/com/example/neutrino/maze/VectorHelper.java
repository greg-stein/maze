package com.example.neutrino.maze;

import android.graphics.Color;
import android.graphics.Matrix;
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

    /**
     * Aligns one vector to another so that they are parallel or orthogonal, depending on
     * what is closer.
     * @param u1 First point of reference vector
     * @param u2 Second point of reference vector
     * @param v1 First point of alignee vector
     * @param v2 Second point of alignee vector
     * @param t Threshold indicates when alignment should be performed.
     */
    public static void alignVector(PointF u1, PointF u2, PointF v1, PointF v2, float t) {
        float[] u = {u2.x - u1.x, u2.y - u1.y};
        float[] v = {v2.x - v1.x, v2.y - v1.y};
        float uMag = (float) Math.sqrt(u[0]*u[0] + u[1]*u[1]);
        float vMag = (float) Math.sqrt(v[0]*v[0] + v[1]*v[1]);
        float[] ui = {u[0]/uMag, u[1]/uMag};
        float[] vi = {v[0]/vMag, v[1]/vMag};

        float dotProduct = ui[0]*vi[0] + ui[1]*vi[1];
        float cos_t = (float) Math.sqrt(1 - t*t);

        if (dotProduct > cos_t) {
            // v1 + vMag * ui
            v2.set(v1);
            v2.offset(vMag * ui[0], vMag * ui[1]);
        }
        else if (dotProduct < -cos_t) {
            // v1 - vMag * ui
            v2.set(v1);
            v2.offset(-vMag * ui[0], -vMag * ui[1]);
        }
        else if (dotProduct > -t && dotProduct < t) {
            float[] orthoUi = {-ui[1], ui[0]};
            float dotPOrthoUiVi = orthoUi[0] * vi[0] + orthoUi[1] * vi[1];

            if (dotPOrthoUiVi > 0) {
                // v1 + vMag * orthoUi
                v2.set(v1);
                v2.offset(vMag * orthoUi[0], vMag * orthoUi[1]);
            }
            else {
                // v1 - vMag * orthoUi
                v2.set(v1);
                v2.offset(-vMag * orthoUi[0], -vMag * orthoUi[1]);
            }
        }
    }

    public static void colorTo3F(int color, float[] fColor) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int alpha = Color.alpha(color);

        fColor[0] = red/255f;
        fColor[1] = green/255f;
        fColor[2] = blue/255f;
        fColor[3] = alpha/255f;
    }

    public static void approximateCircle(PointF center, float radius, int segments, Matrix rotationMatrix, float[] vertices, int offset, int stride) {
        PointF startPos = new PointF(radius, 0);
        vertices[offset] = startPos.x;
        vertices[offset + 1] = startPos.y;
        int nextOffset;

        for (int segment = 0; segment < segments; segment++) {
            nextOffset = offset + stride;
            rotationMatrix.mapPoints(vertices, nextOffset, vertices, offset, 1);
            vertices[offset] += center.x;
            vertices[offset + 1] += center.y;
            offset = nextOffset;
        }
        vertices[offset] += center.x;
        vertices[offset + 1] += center.y;
    }

    // vertices should contain at least segments * GlEngine.COORDS_PER_VERTEX * 2
    public static void buildRing(PointF center, float inner_radius, float outer_radius, int segments, float[] vertices) {
        final int STRIDE = GlEngine.COORDS_PER_VERTEX * 2;
        float theta = (float) (2 * Math.PI / segments);
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(theta);
//        rotationMatrix.postTranslate(center.x, center.y);

        approximateCircle(center, outer_radius, segments, rotationMatrix, vertices, 0, STRIDE);
        approximateCircle(center, inner_radius, segments, rotationMatrix, vertices, GlEngine.COORDS_PER_VERTEX, STRIDE);
    }

    // vertices should contain at least segments * GlEngine.COORDS_PER_VERTEX
    public static void buildCircle(PointF center, float radius, int segments, float[] vertices) {
        final int STRIDE = GlEngine.COORDS_PER_VERTEX;
        float theta = 360f / segments;
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(theta);
//        rotationMatrix.preTranslate(center.x, center.y);

        vertices[0] = center.x;
        vertices[1] = center.y;
        approximateCircle(center, radius, segments, rotationMatrix, vertices, GlEngine.COORDS_PER_VERTEX, STRIDE);
    }
}

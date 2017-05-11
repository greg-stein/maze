package com.example.neutrino.maze.rendering;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

import org.apache.commons.math3.geometry.euclidean.twod.SubLine;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

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

    // vertices should contain at least segments * GlRenderBuffer.COORDS_PER_VERTEX * 2
    public static void buildRing(PointF center, float inner_radius, float outer_radius, int segments, float[] vertices) {
        final int STRIDE = GlRenderBuffer.COORDS_PER_VERTEX * 2;
        float theta = 360f / segments;
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(theta);

        approximateCircle(center, outer_radius, segments, rotationMatrix, vertices, 0, STRIDE);
        approximateCircle(center, inner_radius, segments, rotationMatrix, vertices, GlRenderBuffer.COORDS_PER_VERTEX, STRIDE);
    }

    // vertices should contain at least segments * GlRenderBuffer.COORDS_PER_VERTEX
    public static void buildCircle(PointF center, float radius, int segments, float[] vertices) {
        final int STRIDE = GlRenderBuffer.COORDS_PER_VERTEX;
        float theta = 360f / segments;
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(theta);

        vertices[0] = center.x;
        vertices[1] = center.y;
        approximateCircle(center, radius, segments, rotationMatrix, vertices, GlRenderBuffer.COORDS_PER_VERTEX, STRIDE);
    }

    public static boolean linesIntersect(PointF A, PointF B, PointF O, PointF M) {
        float v1[] = {A.x - O.x, A.y - O.y}; // OA
        float v2[] = {B.x - O.x, B.y - O.y}; // OB
        float v3[] = {M.x - O.x, M.y - O.y}; // OM
        float v4[] = {M.x - A.x, M.y - A.y}; // AM
        float v5[] = {B.x - A.x, B.y - A.y}; // AB

        // Calculate point M' = M mirrored by AB
        float abLengthSq = v5[0] * v5[0] + v5[1] * v5[1]; // AB • AB
        float abDam = v5[0] * v4[0] + v5[1] * v4[1]; // AB • AM
        v5[0] *= 2 * (1 / abLengthSq) * abDam;
        v5[1] *= 2 * (1 / abLengthSq) * abDam;
        v5[0] -= v4[0];
        v5[1] -= v4[1];
        v5[0] += v1[0]; // v5 now holds vector OM'
        v5[1] += v1[1];

        float omLengthSq = v3[0] * v3[0] + v3[1] * v3[1]; // OM • OM
        float omMirroredLengthSq = v5[0] * v5[0] + v5[1] * v5[1]; // OM' • OM'
        if (omMirroredLengthSq > omLengthSq) return false; // mirrored M is farther (on opposite side of/above AB) than M

        if (omMirroredLengthSq == omLengthSq) { // M == M'? => M on line that goes through AB
            if (v1[0]/v3[0] == v1[1]/v3[1]) {// OA || OM ? => All four points lay on same line
                // Yeah, this code is not as efficient. But it got called once per eon. It just covers
                // the case when all four points are on same line.
                RectF rectAB = new RectF(A.x, A.y, B.x, B.y); rectAB.sort();
                RectF rectOM = new RectF(O.x, O.y, M.x, M.y); rectOM.sort();
                return rectAB.intersect(rectOM);
            }
        }

        // Otherwise point M is "above" line A-B, we need to test if its obscured
        // Test: sign(v1 x v3) == sign(v3 x v2)
        float crossV1V3 = v1[0]*v3[1] - v1[1]*v3[0];
        float crossV3V2 = v3[0]*v2[1] - v3[1]*v2[0];
        return (crossV1V3 < 0) == (crossV3V2 < 0);
    }

    public static float squareDistance(PointF p1, PointF p2) {
        final float diffX = p1.x - p2.x;
        final float diffY = p1.y - p2.y;
        return diffX * diffX + diffY * diffY;
    }

    public static PointF projection(PointF a1, PointF a2, PointF b1, PointF b2) {
        float a[] = {a2.x - a1.x, a2.y - a1.y};
        float b[] = {b2.x - b1.x, b2.y - b1.y};
        float bMagnitude = (float) Math.hypot(b[0], b[1]);
        float bUnit[] = {b[0] / bMagnitude, b[1] / bMagnitude};

        float aScalar = a[0] * bUnit[0] + a[1] * bUnit[1];
        return new PointF(aScalar * bUnit[0], aScalar * bUnit[1]);
    }

    // Test if line intersects rectangle
    public static boolean lineIntersect(PointF lineA, PointF lineB, RectF rect) {
        // If one of line ends within rect => intersects
        // Rect.contains method tests if left <= x < right and top <= y < bottom
        // Hence points that lay on left/top edges are included => check if we need to exclude them
        if (rect.contains(lineA.x, lineA.y) && (lineA.x != rect.left) && (lineA.y != rect.top)) {
            return true;
        }
        if (rect.contains(lineB.x, lineB.y) && (lineB.x != rect.left) && (lineB.y != rect.top)) {
            return true;
        }

        // Test if given line intersects one of rects diagonals
        if (linesIntersect(lineA, lineB, new PointF(rect.left, rect.top), new PointF(rect.right, rect.bottom)))
            return true;
        if (linesIntersect(lineA, lineB, new PointF(rect.left, rect.bottom), new PointF(rect.right, rect.top)))
            return true;

        return false;
    }
}

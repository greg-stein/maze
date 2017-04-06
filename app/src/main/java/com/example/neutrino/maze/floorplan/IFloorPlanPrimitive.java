package com.example.neutrino.maze.floorplan;

import com.example.neutrino.maze.rendering.GlRenderBuffer;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Greg Stein on 9/21/2016.
 */
public interface IFloorPlanPrimitive {
    int getVerticesDataSize();

    int getIndicesDataSize();

    void updateVertices();

    // This method puts vertex data into given buffer
    // Buffer.position() is saved internally for further updates
    // This method call should be followed immediately by putIndices() method call
    void putVertices(FloatBuffer verticesBuffer);

    void putIndices(ShortBuffer indexBuffer);

    void updateBuffer(FloatBuffer verticesBuffer);

    int getColor();

    void setColor(int color);

    int getVertexBufferPosition();

    int getIndexBufferPosition();

    void setRemoved(boolean removed);

    boolean isRemoved();

    void cloak();

    void scaleVertices(float scaleFactor);

    GlRenderBuffer getContainingBuffer();

    void setContainingBuffer(GlRenderBuffer mGlBuffer);

    void rewriteToBuffer();
}

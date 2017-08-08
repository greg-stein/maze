package com.example.neutrino.maze.floorplan.transitions;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;

import com.example.neutrino.maze.WiFiLocator;
import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.IFloorPlanPrimitive;
import com.example.neutrino.maze.floorplan.Tag;
import com.example.neutrino.maze.rendering.GlRenderBuffer;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Greg Stein on 7/20/2017.
 */

public class Teleport extends Tag implements ITeleport, IFloorPlanPrimitive {
    private static final float ELEVATOR_MARK_RADIUS = 1.5f;
    private Footprint mElevatorMark;

    public enum Type {
        ELEVATOR, ESCALATOR, STAIRS, RAMP
    }

    private Type mType;

    public Teleport() {
        super();
    }

    public Teleport(PointF location, String id) {
        this(location, id, Type.ELEVATOR);
    }

    public Teleport(PointF location, String id, Type type) {
        super(location, id);
        mElevatorMark = new Footprint(location.x, location.y, ELEVATOR_MARK_RADIUS);
        mElevatorMark.setColor(Color.DKGRAY);
        mType = type;
    }

    public Type getType() {
        return mType;
    }

    public void setType(Type type) {
        this.mType = type;
    }

    @Override
    public String getId() {
        return super.getLabel();
    }

    @Override
    public void setId(String id) {
        super.setLabel(id);
    }

    @Override
    public PointF getLocation() {
        return super.getLocation();
    }

    @Override
    public void setLocation(PointF location) {
        super.setLocation(location);
    }

    @Override
    public WiFiLocator.WiFiFingerprint getFingerprint() {
        return null;
    }

    @Override
    public void setFingerprint(WiFiLocator.WiFiFingerprint fingerprint) {

    }

    @Override
    public String getFloor() {
        return null;
    }

    @Override
    public void setFloor(String floorId) {

    }

    ///////////////////////////////////////////////////////////////////////////////////
    /// Adapter Section. Wrapping mElevatorMark                                     ///
    ///////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getVerticesDataSize() {
        return mElevatorMark.getVerticesDataSize();
    }

    @Override
    public int getIndicesDataSize() {
        return mElevatorMark.getIndicesDataSize();
    }

    @Override
    public void updateVertices() {
        mElevatorMark.updateVertices();
    }

    @Override
    public void putVertices(FloatBuffer verticesBuffer) {
        mElevatorMark.putVertices(verticesBuffer);
    }

    @Override
    public void putIndices(ShortBuffer indexBuffer) {
        mElevatorMark.putIndices(indexBuffer);
    }

    @Override
    public void updateBuffer(FloatBuffer verticesBuffer) {
        mElevatorMark.updateBuffer(verticesBuffer);
    }

    @Override
    public int getColor() {
        return mElevatorMark.getColor();
    }

    @Override
    public void setColor(int color) {
        mElevatorMark.setColor(color);
    }

    @Override
    public int getVertexBufferPosition() {
        return mElevatorMark.getVertexBufferPosition();
    }

    @Override
    public int getIndexBufferPosition() {
        return mElevatorMark.getIndexBufferPosition();
    }

    @Override
    public void setRemoved(boolean removed) {
        mElevatorMark.setRemoved(removed);
    }

    @Override
    public boolean isRemoved() {
        return mElevatorMark.isRemoved();
    }

    @Override
    public void cloak() {
        mElevatorMark.cloak();
    }

    @Override
    public void scaleVertices(float scaleFactor) {
        mElevatorMark.scaleVertices(scaleFactor);
    }

    @Override
    public GlRenderBuffer getContainingBuffer() {
        return mElevatorMark.getContainingBuffer();
    }

    @Override
    public void setContainingBuffer(GlRenderBuffer mGlBuffer) {
        mElevatorMark.setContainingBuffer(mGlBuffer);
    }

    @Override
    public void rewriteToBuffer() {
        mElevatorMark.rewriteToBuffer();
    }

    @Override
    public RectF getBoundingBox() {
        return mElevatorMark.getBoundingBox();
    }
}

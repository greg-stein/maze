package world.maze.floorplan.transitions;

import android.graphics.PointF;
import android.graphics.RectF;

import world.maze.AppSettings;
import world.maze.core.WiFiLocator;
import world.maze.floorplan.Floor;
import world.maze.floorplan.IFloorPlanPrimitive;
import world.maze.floorplan.Ring;
import world.maze.floorplan.Tag;
import world.maze.rendering.GlRenderBuffer;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Greg Stein on 7/20/2017.
 */

public class Teleport extends Tag implements ITeleport, IFloorPlanPrimitive {
    public static final float ELEVATOR_MARK_OUTER_RADIUS = 2.00f;
    public static final float ELEVATOR_MARK_INNER_RADIUS = 1.75f;
    public static final int ELEVATOR_MARK_SEGMENTS_NUM = 32;
    private transient Ring mElevatorMark;

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
        mElevatorMark = new Ring(location.x, location.y, ELEVATOR_MARK_INNER_RADIUS, ELEVATOR_MARK_OUTER_RADIUS, ELEVATOR_MARK_SEGMENTS_NUM);
        mElevatorMark.setColor(AppSettings.teleportColor);
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
    public Floor getFloor() {
        return null;
    }

    @Override
    public void setFloor(Floor floor) {

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

    @Override
    public void handleMove(float x, float y) {
        super.handleMove(x, y);
        mElevatorMark.handleMove(x, y);
    }

    @Override
    public void setTapLocation(float x, float y) {
        super.setTapLocation(x, y);
        mElevatorMark.setTapLocation(x, y);
    }

    @Override
    public void handleMoveStart() {
        super.handleMoveStart();
        mElevatorMark.handleMoveStart();
    }

    @Override
    public void handleMoveEnd() {
        super.handleMoveEnd();
        mElevatorMark.handleMoveEnd();
    }

    @Override
    public boolean hasPoint(float x, float y) {
        return mElevatorMark.hasPoint(x, y);
    }

}

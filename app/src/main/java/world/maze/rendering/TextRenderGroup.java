package world.maze.rendering;

import android.graphics.PointF;

import world.maze.floorplan.IMoveable;
import world.maze.floorplan.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 12/29/2017.
 */

public class TextRenderGroup extends RenderGroupBase {
    private boolean mReadyForRender;

    private List<Tag> mRenderedTags = new ArrayList<>();
    private List<Tag> mTagsNotRenderedYet = new ArrayList<>();
    private GLText mGlText;

    public TextRenderGroup(List<? extends Tag> tags, GLText glText) {
        if (tags != null) {
            mTagsNotRenderedYet.addAll(tags);
        }
        mGlText = glText;
        mReadyForRender = false;
    }

    @Override
    public void prepareForRender() {
        for (Tag tag : mTagsNotRenderedYet) {
            tag.setRenderedTextWidth(mGlText.getLength(tag.getLabel()));
            tag.setRenderedTextHeight(mGlText.getCharHeight());
            tag.updateBoundariesRect();
        }
        mRenderedTags.addAll(mTagsNotRenderedYet);
        mTagsNotRenderedYet.clear();
        mReadyForRender = true;
    }

    public boolean isReadyForRender() {
        return mReadyForRender;
    }

    @Override
    public void render(float[] scratch, float deviceAngle) {
        if (!isVisible()) return;

        mGlText.begin(0.0f, 0.0f, 1.0f, 1.0f, scratch); // Begin Text Rendering (Set Color BLUE)

        for (Tag tag : mRenderedTags) {
            PointF tagLocation = tag.getLocation();
            float[] boundaries = tag.getBoundaryCorners();
            float[] boundariesTransformed = tag.getBoundaryCornersTransformed();

            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setRotate(-deviceAngle, tagLocation.x, tagLocation.y);
            m.mapPoints(boundariesTransformed, boundaries);

            mGlText.draw(tag.getLabel(), boundariesTransformed[6], boundariesTransformed[7], 0, 0, 0, -deviceAngle);  // Draw Text Centered

        }

        mGlText.end();                                   // End Text Rendering
    }

    @Override
    public void glDeallocate() {

    }

    @Override
    public IMoveable findElementHavingPoint(PointF p) {
        for (Tag tag : mRenderedTags) {
            if (tag.hasPoint(p.x, p.y)) {
                return tag;
            }
        }

        return null;
    }

    @Override
    public boolean isEmpty() {
        return mRenderedTags.isEmpty();
    }

    public void addItem(Tag tag) {
        mTagsNotRenderedYet.add(tag);
        mReadyForRender = false;
        setChanged(true);
        emitElementAddedEvent(tag);
    }

    @Override
    public void removeElement(IMoveable tag) {
        mRenderedTags.remove(tag);
        setChanged(true);
        emitElementRemovedEvent(tag);
    }

    @Override
    public void clear() {
        mRenderedTags.clear();
    }

    public void setReadyForRender(boolean readyForRender) {
        mReadyForRender = readyForRender;
    }
}
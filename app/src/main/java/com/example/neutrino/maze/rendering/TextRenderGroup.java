package com.example.neutrino.maze.rendering;

import android.graphics.PointF;
import android.opengl.GLES20;

import com.example.neutrino.maze.AppSettings;
import com.example.neutrino.maze.floorplan.IMoveable;
import com.example.neutrino.maze.floorplan.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 12/29/2017.
 */

public class TextRenderGroup implements IRenderGroup {
    private boolean mReadyForRender;

    private List<Tag> mRenderedTags = new ArrayList<>();
    private List<Tag> mTagsNotRenderedYet = new ArrayList<>();
    private GLText mGlText;
    private boolean mIsVisible = false;
    private boolean mChanged;

    public TextRenderGroup(List<Tag> tags, GLText glText) {
        mTagsNotRenderedYet.addAll(tags);
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

    @Override
    public boolean isVisible() {
        return mIsVisible;
    }

    @Override
    public void setVisible(boolean visible) {
        mIsVisible = visible;
    }

    public boolean isReadyForRender() {
        return mReadyForRender;
    }

    @Override
    public void render(float[] scratch, float deviceAngle) {
        if (!mIsVisible) return;

        // TODO: This is not good. What if we have several TextRenderGroups? Each will do glUseProgram?
        // TODO: this should go away with the refactoring of text render code. Currently only one
        // TODO: TextRenderGroup is supported and it should be after all other groups.
        GLES20.glUseProgram(AppSettings.oglTextRenderProgram);
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
        mChanged = true;
    }

    @Override
    public void removeElement(IMoveable tag) {
        mRenderedTags.remove(tag);
        mChanged = true;
    }

    @Override
    public void clear() {
        mRenderedTags.clear();
    }

    @Override
    public boolean isChanged() {
        return mChanged;
    }

    @Override
    public void setChanged(boolean changed) {
        mChanged = changed;
    }

    public void setReadyForRender(boolean readyForRender) {
        mReadyForRender = readyForRender;
    }
}
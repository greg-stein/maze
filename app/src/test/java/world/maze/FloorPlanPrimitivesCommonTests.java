package com.example.neutrino.maze;

import android.graphics.RectF;

import com.example.neutrino.maze.floorplan.Footprint;
import com.example.neutrino.maze.floorplan.LocationMark;
import com.example.neutrino.maze.floorplan.Wall;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 5/9/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class FloorPlanPrimitivesCommonTests {
    @Test
    public void wallBoundingBoxTest() {
        Wall wall = new Wall(1, 2, 3, 4);
        RectF boundingBox = wall.getBoundingBox();
        assertThat(boundingBox.left, is(equalTo(1f)));
        assertThat(boundingBox.top, is(equalTo(2f)));
        assertThat(boundingBox.right, is(equalTo(3f)));
        assertThat(boundingBox.bottom, is(equalTo(4f)));
    }

    @Test
    public void wallBoundingBoxUnsortedTest() {
        Wall wall = new Wall(1, 4, 3, 2);
        RectF boundingBox = wall.getBoundingBox();
        assertThat(boundingBox.left, is(equalTo(1f)));
        assertThat(boundingBox.top, is(equalTo(2f)));
        assertThat(boundingBox.right, is(equalTo(3f)));
        assertThat(boundingBox.bottom, is(equalTo(4f)));
    }

    @Test
    public void footprintBoundingBoxTest() {
        Footprint footprint = new Footprint(1, 4);
        RectF boundingBox = footprint.getBoundingBox();
        assertThat(boundingBox.left, is(equalTo(-1.5f)));
        assertThat(boundingBox.top, is(equalTo(1.5f)));
        assertThat(boundingBox.right, is(equalTo(3.5f)));
        assertThat(boundingBox.bottom, is(equalTo(6.5f)));
    }

    @Test
    public void locationMarkBoundingBoxTest() {
        LocationMark locationMark = new LocationMark(1, 4, 3, 5);
        RectF boundingBox = locationMark.getBoundingBox();
        assertThat(boundingBox.left, is(equalTo(-4f)));
        assertThat(boundingBox.top, is(equalTo(-1f)));
        assertThat(boundingBox.right, is(equalTo(6f)));
        assertThat(boundingBox.bottom, is(equalTo(9f)));
    }
}

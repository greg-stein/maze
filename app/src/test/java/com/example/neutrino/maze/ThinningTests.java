package com.example.neutrino.maze;

import android.graphics.Color;
import android.graphics.Point;

import com.example.neutrino.maze.vectorization.ImageArray;
import com.example.neutrino.maze.vectorization.Thinning;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Greg Stein on 11/23/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = 23)
public class ThinningTests {

    @Rule
    public ErrorCollector collector= new ErrorCollector();

    @Test
    public void getNeighboursAndTransitionsTest() {
        int[] data = new int[] {
                Color.WHITE, Color.BLACK, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.WHITE,
                Color.WHITE, Color.WHITE, Color.WHITE
        };
        ImageArray imageArray = new ImageArray(data, 3, 3);

        Thinning.getNeighboursAndTransitions(imageArray, new Point(1, 1), true);

        assertThat(Thinning.transitions, is(equalTo(1)));
        assertThat(Thinning.neighbours, is(equalTo(2)));
        assertFalse(Thinning.hasThreeConsequentEvenBlackNeighbours);
    }

    @Test
    public void getNeighboursAndTransitionsTest2() {
        int[] data = new int[] {
                Color.WHITE, Color.BLACK, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.BLACK
        };
        ImageArray imageArray = new ImageArray(data, 3, 3);

        Thinning.getNeighboursAndTransitions(imageArray, new Point(1, 1), true);

        assertThat(Thinning.transitions, is(equalTo(1)));
        assertThat(Thinning.neighbours, is(equalTo(5)));
        assertTrue(Thinning.hasThreeConsequentEvenBlackNeighbours);
    }

    @Test
    public void getNeighboursAndTransitionsTest3() {
        int[] data = new int[] {
                Color.WHITE, Color.BLACK, Color.BLACK,
                Color.BLACK, Color.BLACK, Color.BLACK,
                Color.BLACK, Color.BLACK, Color.BLACK
        };
        ImageArray imageArray = new ImageArray(data, 3, 3);

        Thinning.getNeighboursAndTransitions(imageArray, new Point(1, 1), true);

        assertThat(Thinning.transitions, is(equalTo(1)));
        assertThat(Thinning.neighbours, is(equalTo(7)));
        assertTrue(Thinning.hasThreeConsequentEvenBlackNeighbours);
    }

    @Test
    public void getNeighboursAndTransitionsTest4() {
        int[] data = new int[] {
                Color.WHITE, Color.BLACK, Color.BLACK,
                Color.BLACK, Color.BLACK, Color.WHITE,
                Color.WHITE, Color.BLACK, Color.BLACK
        };
        ImageArray imageArray = new ImageArray(data, 3, 3);

        Thinning.getNeighboursAndTransitions(imageArray, new Point(1, 1), true);

        assertThat(Thinning.transitions, is(equalTo(3)));
        assertThat(Thinning.neighbours, is(equalTo(5)));
        assertFalse(Thinning.hasThreeConsequentEvenBlackNeighbours);
    }

    @Test
    public void getNeighboursAndTransitionsTest5() {
        int[] data = new int[] {
                Color.WHITE, Color.BLACK, Color.BLACK,
                Color.BLACK, Color.BLACK, Color.BLACK,
                Color.WHITE, Color.BLACK, Color.BLACK
        };
        ImageArray imageArray = new ImageArray(data, 3, 3);

        Thinning.getNeighboursAndTransitions(imageArray, new Point(1, 1), false);

        assertThat(Thinning.transitions, is(equalTo(2)));
        assertThat(Thinning.neighbours, is(equalTo(6)));
        assertTrue(Thinning.hasThreeConsequentEvenBlackNeighbours);
    }
}

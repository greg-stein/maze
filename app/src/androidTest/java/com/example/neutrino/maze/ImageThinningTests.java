package com.example.neutrino.maze;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.example.neutrino.maze.floorplan.vectorization.ImageArray;
import com.example.neutrino.maze.floorplan.vectorization.Thinning;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
//import static org.hamcrest.Matchers.hasItemInArray;
//import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
//import static org.hamcrest.Matchers.closeTo;
//import static org.hamcrest.Matchers.contains;
//import static org.hamcrest.Matchers.empty;
//import static org.hamcrest.Matchers.hasItem;
//import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 1/5/2017.
 */
@RunWith(AndroidJUnit4.class)
public class ImageThinningTests {
    private static final String RESOURCE_TEST_DIR = "/thinningTest";
    private static ImageThinningTests instance;

    public ImageThinningTests() {
        instance = this;
    }

    private static Bitmap readBitmapFromResources(String bitmapRes) {
        Bitmap bitmapFromRes = null;

        try {
            Class<? extends ImageThinningTests> aClass = instance.getClass();
            InputStream in_s = aClass.getResourceAsStream(RESOURCE_TEST_DIR + "/" + bitmapRes);

            bitmapFromRes = BitmapFactory.decodeStream(in_s);

        } catch (Exception e) {
            e.printStackTrace(); // АААА! Жопа!! жопА!!
        }

        return bitmapFromRes;
    }

    private void assertBitmapsEqual(Bitmap expected, Bitmap actual) {
        assertThat(expected.getWidth(), is(equalTo(actual.getWidth())));
        assertThat(expected.getHeight(), is(equalTo(actual.getHeight())));

        int width = expected.getWidth();
        int height = expected.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                assertThat(expected.getPixel(x, y), is(equalTo(actual.getPixel(x, y))));
            }
        }
    }

    @Test
    public void bitmapToImageArrayTest() {
        Bitmap original = readBitmapFromResources("test1original.png");
        assertNotNull(original);
        ImageArray imageArray = new ImageArray(original);
        Bitmap actual = imageArray.toBitmap();

        assertNotNull(actual);
        assertBitmapsEqual(original, actual);
    }
}



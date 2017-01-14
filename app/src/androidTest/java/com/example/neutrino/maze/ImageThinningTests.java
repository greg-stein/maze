package com.example.neutrino.maze;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.filters.MediumTest;

import com.example.neutrino.maze.floorplan.vectorization.ImageArray;
import com.example.neutrino.maze.floorplan.vectorization.Thinning;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameters;

/**
 * Created by Greg Stein on 1/5/2017.
 */
@RunWith(Parameterized.class)
@MediumTest
public class ImageThinningTests {
    private static final String RESOURCE_TEST_DIR = "/thinningTest";
    private final Bitmap mOriginal;
    private final Bitmap mExpected;
    private final int mTestCaseIndex;

    public ImageThinningTests(Bitmap original, Bitmap expected, int testCaseIndex) {
        mOriginal = original;
        mExpected = expected;
        mTestCaseIndex = testCaseIndex;
    }

    @Parameters
    public static Iterable<Object[]> data() {
        List<Object[]> parameters = new ArrayList<>();
        int index = 1;
        Bitmap original;
        Bitmap expected;

        boolean resourcesExist;
        do {
            original = TestHelper.readBitmapFromResources(RESOURCE_TEST_DIR + "/test" + index + "original.png");
            expected = TestHelper.readBitmapFromResources(RESOURCE_TEST_DIR + "/test" + index + "expected.png");

            resourcesExist = original != null && expected != null;

            if (resourcesExist) {
                parameters.add(new Object[]{original, expected, Integer.valueOf(index)});
            }

            index++;
        } while (resourcesExist);

        return parameters;
    }

    private void assertBitmapsEqual(Bitmap expected, Bitmap actual) {
        assertThat(expected.getWidth(), is(equalTo(actual.getWidth())));
        assertThat(expected.getHeight(), is(equalTo(actual.getHeight())));

        int width = expected.getWidth();
        int height = expected.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x == 0 && y == 0) continue; // skip first pixel, it marks end of pixel data
                assertThat(String.format("(x, y)=(%d, %d)", x, y), expected.getPixel(x, y), is(equalTo(actual.getPixel(x, y))));
            }
        }
    }

    @Test
    public void bitmapToImageArrayTest() {
        assertNotNull(mOriginal);
        ImageArray imageArray = new ImageArray(mOriginal);
        Bitmap actual = imageArray.toBitmap();

        assertNotNull(actual);
        assertBitmapsEqual(mOriginal, actual);
    }

    @Test
    public void thinningTest() {
        assertNotNull(mOriginal);
        assertNotNull(mExpected);

        ImageArray imageArray = new ImageArray(mOriginal);
        imageArray.findBlackPixels();
        Thinning.doZhangSuenThinning(imageArray);
        Bitmap actual = imageArray.toBitmap();
        assertBitmapsEqual(mExpected, actual);
    }
}



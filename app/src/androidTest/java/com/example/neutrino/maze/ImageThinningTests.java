package com.example.neutrino.maze;

import android.graphics.Bitmap;
import android.support.test.filters.MediumTest;

import com.example.neutrino.maze.vectorization.FloorplanVectorizer;
import com.example.neutrino.maze.vectorization.ImageArray;
import com.example.neutrino.maze.vectorization.Thinning;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

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
                original = FloorplanVectorizer.toGrayscale(original, 1);
                parameters.add(new Object[]{original, expected, Integer.valueOf(index)});
            }

            index++;
        } while (resourcesExist);

        return parameters;
    }

    @Test
    public void bitmapToImageArrayTest() {
        assertNotNull(mOriginal);
        ImageArray imageArray = new ImageArray(mOriginal);
        Bitmap actual = imageArray.toBitmap();

        assertNotNull(actual);
        TestHelper.assertBitmapsEqual(mOriginal, actual);
    }

    @Test
    public void thinningTest() {
        assertNotNull(mOriginal);
        assertNotNull(mExpected);

        ImageArray imageArray = new ImageArray(mOriginal);
        imageArray.findBlackPixels();
        Thinning.doZhangSuenThinning(imageArray);
        Bitmap actual = imageArray.toBitmap();
        TestHelper.assertBitmapsEqual(mExpected, actual);
    }
}



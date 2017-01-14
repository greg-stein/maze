package com.example.neutrino.maze;

import android.graphics.Bitmap;
import android.support.test.filters.MediumTest;

import com.example.neutrino.maze.floorplan.vectorization.HoughTransform;
import com.example.neutrino.maze.floorplan.vectorization.ImageArray;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greg Stein on 1/11/2017.
 */
@RunWith(Parameterized.class)
@MediumTest
public class ImageHoughTests {
    private static final String TEST_DIRECTORY = "/houghTest";
    private static final String JSON_DATA_SOURCE = TEST_DIRECTORY + "/data.json";

    private final Bitmap image;
    private final HoughTransform.LineSegment[] expectedSegments;

    public ImageHoughTests(Bitmap image, HoughTransform.LineSegment[] expectedSegments) {
        this.image = image;
        this.expectedSegments = expectedSegments;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> readTestCases() {
        List<Object[]> parameters = new ArrayList<>();

        InputStream stream = TestHelper.openFileFromResources(JSON_DATA_SOURCE);
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(stream);
        HoughTestCase[] testCases = gson.fromJson(reader, HoughTestCase[].class);

        for (HoughTestCase testCase : testCases) {
            final Bitmap image = TestHelper.readBitmapFromResources(TEST_DIRECTORY + "/" + testCase.imageFile);
            parameters.add(new Object[] {image, testCase.expectedLineSegments});
        }
        return parameters;
    }

    @Test
    public void houghTestCaseCheck() {
        ImageArray imageArray = new ImageArray(image);
        imageArray.findBlackPixels();
        HoughTransform houghTransform = new HoughTransform(imageArray);
        houghTransform.buildHoughSpace();
        List<HoughTransform.LineSegment> actualSegments = houghTransform.getLineSegments(50);

        assertNotNull(actualSegments);
        assertThat(actualSegments.size(), is(equalTo(expectedSegments.length)));
        for (HoughTransform.LineSegment segment : expectedSegments) {
            assertThat(actualSegments, hasItem(segment));
        }
    }

    /**
     * Created by Greg Stein on 1/12/2017.
     */
    public static class HoughTestCase {
        public String imageFile;
        public HoughTransform.LineSegment[] expectedLineSegments;
    }
}

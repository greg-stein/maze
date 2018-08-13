package world.maze;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.test.filters.MediumTest;

import world.maze.vectorization.HoughTransform;
import world.maze.vectorization.HoughTransform.LineSegment;
import world.maze.vectorization.ImageArray;
import world.maze.vectorization.LineSegmentsRecognizer;
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
    private final LineSegment[] expectedSegments;

    public ImageHoughTests(Bitmap image, LineSegment[] expectedSegments) {
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

        final LineSegment segment = new LineSegment(new Point(10, 10), new Point(80, 80));
        Bitmap image = renderSegments(100, 100, segment);
        parameters.add(new Object[] {image, new LineSegment[] {segment}});

        final LineSegment[] segments2 = new LineSegment[] {
                new LineSegment(new Point(10, 10), new Point(80, 80)),
                new LineSegment(new Point(90, 10), new Point(10, 90))
        };

        Bitmap image2 = renderSegments(100, 100, segments2);
        parameters.add(new Object[] {image2, segments2});

        return parameters;
    }

    private static Bitmap renderSegments(int width, int height, LineSegment... segments) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Paint p = new Paint();
        p.setColor(Color.BLACK);

        for (LineSegment segment : segments) {
            // Android BUG in documentation: excluding last pixel?
            c.drawLine(segment.start.x, segment.start.y, segment.end.x, segment.end.y, p);
            c.drawPoint(segment.end.x, segment.end.y, p);
        }

        return bmp;
    }

    @Test
    public void houghTestCaseCheck() {
        ImageArray imageArray = new ImageArray(image);
        imageArray.findBlackPixels();
        HoughTransform houghTransform = new HoughTransform(imageArray);
        houghTransform.buildHoughSpace();
        List<LineSegment> actualSegments = houghTransform.getLineSegments(50);

        System.out.println(actualSegments);
        assertNotNull(actualSegments);
        assertThat(actualSegments.size(), is(equalTo(expectedSegments.length)));
        for (HoughTransform.LineSegment segment : expectedSegments) {
            assertThat(actualSegments, hasItem(segment));
        }
    }

    @Test
    public void segmentsRecognizerTestCaseCheck() {
        ImageArray imageArray = new ImageArray(image);
        imageArray.findBlackPixels();
        LineSegmentsRecognizer houghTransform = new LineSegmentsRecognizer(imageArray);
        List<LineSegment> actualSegments = houghTransform.findStraightSegments();
        List<LineSegment> mergedSegments = HoughTransform.mergeSegments(actualSegments);

        System.out.println(actualSegments);
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

package world.maze;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import world.maze.vectorization.FloorplanVectorizer;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Greg Stein on 1/24/2017.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CommonStuffTests {

    @Test
    public void imagePaddingTest() {
        int[] pixels = new int[] {Color.BLACK};
        Bitmap bmp = Bitmap.createBitmap(pixels, 1, 1, Bitmap.Config.ARGB_8888);

        int[] expectedPixels = new int[] {
                Color.WHITE, Color.WHITE, Color.WHITE,
                Color.WHITE, Color.BLACK, Color.WHITE,
                Color.WHITE, Color.WHITE, Color.WHITE
        };
        Bitmap expected = Bitmap.createBitmap(expectedPixels, 3, 3, Bitmap.Config.ARGB_8888);

        Bitmap actual = FloorplanVectorizer.toGrayscale(bmp, 1);

        TestHelper.assertBitmapsEqual(expected, actual);
    }
}

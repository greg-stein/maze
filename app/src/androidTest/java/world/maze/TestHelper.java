package world.maze;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Greg Stein on 1/12/2017.
 */
public class TestHelper {
    public static InputStream openFileFromResources(String path) {
        Class<? extends TestHelper> aClass = TestHelper.class;
        InputStream in_s = aClass.getResourceAsStream(path);
        return in_s;
    }

    public static Bitmap readBitmapFromResources(String bitmapRes) {
        Bitmap bitmapFromRes = null;

        try {
            InputStream in_s = openFileFromResources(bitmapRes);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Rect erect = new Rect();
            bitmapFromRes = BitmapFactory.decodeStream(in_s, erect, options);
        } catch (Exception e) {
            e.printStackTrace(); // АААА! Жопа!! жопА!!
        }

        return bitmapFromRes;
    }

    static void assertBitmapsEqual(Bitmap expected, Bitmap actual) {
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
}
